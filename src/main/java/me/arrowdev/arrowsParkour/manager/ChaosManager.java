package me.arrowdev.arrowsParkour.manager;

import me.arrowdev.arrowsParkour.ArrowsParkour;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ChaosManager {

    private final ArrowsParkour plugin;
    private final LavaManager lavaManager;
    private final IceManager iceManager;
    private final InvisibleManager invisibleManager;
    private final ArrowRainManager arrowRainManager;
    private final ParkourManager parkourManager;

    // UUID → [actionBarTask, countdownTask]
    private final Map<UUID, BukkitTask> actionBarTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> countdownTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> jumpTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> freezeTasks = new HashMap<>();
    // Kalan süreyi dışarıdan erişilebilir tutuyoruz
    private final Map<UUID, int[]> remainingSeconds = new HashMap<>();

    public ChaosManager(ArrowsParkour plugin,
                        LavaManager lavaManager,
                        IceManager iceManager,
                        InvisibleManager invisibleManager,
                        ArrowRainManager arrowRainManager,
                        ParkourManager parkourManager) {
        this.plugin = plugin;
        this.lavaManager = lavaManager;
        this.iceManager = iceManager;
        this.invisibleManager = invisibleManager;
        this.arrowRainManager = arrowRainManager;
        this.parkourManager = parkourManager;
    }

    // ================================================================
    // KAOS BAŞLAT
    // ================================================================
    public void startChaos(Player target, int seconds, String username) {
        UUID uuid = target.getUniqueId();

        // Zaten aktif kaos varsa önce temizle
        if (hasChaos(target)) {
            stopChaos(target, false);
        }

        ParkourSession session = parkourManager.getSession(target);
        if (session == null) return;

        int durationTicks = seconds * 20;

        // ── 1) ENDER DRAGON SESİ ──────────────────────────────────
        target.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
        target.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.7f);

        // ── 4) KÖRLÜK ────────────────────────────────────────────
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.DARKNESS,
                durationTicks,
                1,      // seviye 1 → daha yoğun karanlık
                false,
                false,
                false
        ));

        // ── 5) SARHOŞLUK ─────────────────────────────────────────
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.NAUSEA,
                durationTicks, 0, false, false, false
        ));

        iceManager.startIce(target, seconds);

        // ── 7) LAV — her 1 saniyede 1 seviye ────────────────────
        lavaManager.startLava(target, 20);

        // ── 8) OK YAĞMURU ─────────────────────────────────────────
        arrowRainManager.startArrowRain(target, seconds);

        // ── 9) ACTION BAR TASK ────────────────────────────────────
        int[] remaining = {seconds};
        remainingSeconds.put(uuid, remaining);

        BukkitTask actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // ★ DÜZELTME: isOnline() artık yetmiyor — hasChaos kontrolü yeterli
            // task zaten cancellanmışsa çalışmaz, ama ekstra güvence:
            if (!remainingSeconds.containsKey(uuid)) return;

            target.sendActionBar("§4☠ KAOS: §e" + remaining[0]
                    + " §4saniye | §c" + username + " §4tarafından");
        }, 0L, 1L);

        actionBarTasks.put(uuid, actionBarTask);

        // ── 10) COUNTDOWN TASK ────────────────────────────────────
        BukkitTask countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // ★ DÜZELTME: remaining map'te yoksa (stopChaos çağrılmışsa) dur
            if (!remainingSeconds.containsKey(uuid)) return;

            if (remaining[0] <= 0) {
                // Normal süre dolumu — mesajlı temizlik
                stopChaos(target, true);
                return;
            }

            remaining[0]--;
        }, 20L, 20L);

        // ── 3) TITLE ─────────────────────────────────────────────
        target.sendTitle(
                "§4☠ KAOS MODU ☠",
                "§c" + username + " §fkaosu başlattı! §7(" + seconds + "s)",
                10, 70, 20
        );

        countdownTasks.put(uuid, countdownTask);
        // ── 11) HER 10 SANİYEDE BİR ZIPLAT ──────────────────────
        BukkitTask jumpTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!remainingSeconds.containsKey(uuid)) return;
            if (!target.isOnline()) return;

            // Havadaysa zıplatma
            if (!target.isOnGround()) return;

            org.bukkit.util.Vector velocity = target.getVelocity();
            velocity.setY(0.92);
            target.setVelocity(velocity);
        }, 200L, 200L); // 200 tick = 10 saniye

        jumpTasks.put(uuid, jumpTask);

// ── 12) HER 20 SANİYEDE BİR HAREKETSİZ BIRAK ────────────
        BukkitTask freezeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!remainingSeconds.containsKey(uuid)) return;
            if (!target.isOnline()) return;

            parkourManager.freezePlayer(target, 3); // 3 saniye dondur
        }, 400L, 400L); // 400 tick = 20 saniye

        freezeTasks.put(uuid, freezeTask);

    }

    // ================================================================
    // KAOS DURDUR
    // sendEndMessage = true → "Kaos bitti" mesajı göster
    //                = false → sessiz temizlik (ölüm/çıkış için)
    // ================================================================
    public void stopChaos(Player target, boolean sendEndMessage) {
        UUID uuid = target.getUniqueId();

        // Remaining map'ten sil — bu sayede hem action bar hem countdown
        // task'larının içindeki kontrol false döner, artık çalışmazlar
        remainingSeconds.remove(uuid);

        // Action bar task iptal
        BukkitTask abt = actionBarTasks.remove(uuid);
        if (abt != null) try { abt.cancel(); } catch (Exception ignored) {}

        // Countdown task iptal
        BukkitTask cdt = countdownTasks.remove(uuid);
        if (cdt != null) try { cdt.cancel(); } catch (Exception ignored) {}

        // Lav durdur
        if (lavaManager.hasLava(target)) lavaManager.stopLava(target);

        // Ok yağmurunu durdur
        if (arrowRainManager.hasArrowRain(target)) arrowRainManager.stopArrowRain(target);

        BukkitTask jt = jumpTasks.remove(uuid);
        if (jt != null) try { jt.cancel(); } catch (Exception ignored) {}

        BukkitTask ft = freezeTasks.remove(uuid);
        if (ft != null) try { ft.cancel(); } catch (Exception ignored) {}

        // Kırmızı ekranı kaldır
        try { target.setWorldBorder(null); } catch (Exception ignored) {}

        // İksir efektlerini kaldır
        target.removePotionEffect(PotionEffectType.DARKNESS);
        target.removePotionEffect(PotionEffectType.NAUSEA);

        // Ice/Invisible kendi timer'larıyla da durur ama garantiye alalım
        if (iceManager.hasIce(target)) iceManager.stopIceSilently(target);
        if (invisibleManager.hasInvisible(target)) invisibleManager.stopInvisibleSilently(target);

        // Mesaj — sadece süre dolarsa göster, ölüm/çıkışta gösterme
        if (sendEndMessage && target.isOnline()) {
            target.sendTitle("§a✓ KAOS BİTTİ", "§7Her şey normale döndü.", 10, 50, 20);
            target.sendMessage("§a☠ Kaos modu sona erdi!");
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        }

        plugin.getLogger().info("☠ Kaos temizlendi ("
                + (sendEndMessage ? "süre doldu" : "ölüm/çıkış") + "): " + target.getName());
    }

    // ================================================================
    // EVENT HOOK'LARI — Listener'dan çağrılır
    // ================================================================

    /** Oyuncu öldüğünde çağrılır */
    public void onPlayerDeath(Player player) {
        if (hasChaos(player)) {
            stopChaos(player, false); // ★ sessiz temizle, ölüm ekranını bozmayalım
            plugin.getLogger().info("☠ Kaos oyuncu ölümüyle iptal edildi: " + player.getName());
        }
    }

    /** Oyuncu sunucudan/dünyadan çıktığında çağrılır */
    public void onPlayerQuit(Player player) {
        if (hasChaos(player)) {
            // Çıkışta setWorldBorder çalışmayabilir — try-catch zaten var
            stopChaos(player, false);
            plugin.getLogger().info("☠ Kaos oyuncu çıkışıyla iptal edildi: " + player.getName());
        }
    }

    // ================================================================
    // YARDIMCI
    // ================================================================
    public boolean hasChaos(Player player) {
        return remainingSeconds.containsKey(player.getUniqueId());
    }

    public void clearAll() {
        // Plugin kapanırken tüm kaosları sessizce temizle
        for (BukkitTask t : actionBarTasks.values()) {
            try { t.cancel(); } catch (Exception ignored) {}
        }
        for (BukkitTask t : countdownTasks.values()) {
            try { t.cancel(); } catch (Exception ignored) {}
        }
        for (BukkitTask t : jumpTasks.values()) {
            try { t.cancel(); } catch (Exception ignored) {}
        }
        for (BukkitTask t : freezeTasks.values()) {
            try { t.cancel(); } catch (Exception ignored) {}
        }
        jumpTasks.clear();
        freezeTasks.clear();
        actionBarTasks.clear();
        countdownTasks.clear();
        remainingSeconds.clear();
    }
}