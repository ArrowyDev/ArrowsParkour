package me.arrowdev.arrowsParkour.manager;

import me.arrowdev.arrowsParkour.ArrowsParkour;
import me.arrowdev.arrowsParkour.model.PrisonSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PrisonManager {

    private final ArrowsParkour plugin;
    private final Map<UUID, PrisonSession> sessions = new HashMap<>();

    // ★ YENİ: Kazma verilen oyuncuları takip et
    private final Set<UUID> prisonPickaxePlayers = new HashSet<>();

    public PrisonManager(ArrowsParkour plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    // HAPİSHANEYİ BAŞLAT VEYA SÜREYİ GÜNCELLE
    // =====================================================================

    public void startPrison(Player target, int seconds, String username) {
        UUID uuid = target.getUniqueId();

        if (isInPrison(target)) {
            updatePrisonTime(target, seconds, username);
            return;
        }

        if (plugin.getParkourManager() != null) {
            plugin.getParkourManager().cancelCountdown(target);
        }

        Location saved = target.getLocation().clone();
        String subtitleText = "§c" + username + " §fseni hapise attı!";

        PrisonSession session = new PrisonSession(saved, seconds, subtitleText);
        sessions.put(uuid, session);

        Location prisonCenter = buildPrison(target, session);
        if (prisonCenter == null) {
            target.sendMessage("§cHapishane inşa edilemedi! Parkur verisi yok.");
            sessions.remove(uuid);
            return;
        }

        Location teleportLoc = new Location(
                prisonCenter.getWorld(),
                prisonCenter.getX(),
                prisonCenter.getY(),
                prisonCenter.getZ(),
                saved.getYaw(),
                saved.getPitch()
        );
        target.teleport(teleportLoc);

        target.sendTitle("§4⛓ HAPİSHANE ⛓", subtitleText, 10, 60, 20);

        BukkitTask actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!target.isOnline()) return;
            PrisonSession s = sessions.get(uuid);
            if (s == null) return;
            String bar = "§c⛓ Hapishane: §e" + s.getRemainingSeconds() + " §csaniye kaldı";
            target.sendActionBar(bar);
        }, 0L, 1L);

        session.setActionBarTask(actionBarTask);

        BukkitTask countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!target.isOnline()) return;

            PrisonSession s = sessions.get(uuid);
            if (s == null) return;

            int remaining = s.getRemainingSeconds();

            if (remaining <= 0) {
                freePrisoner(target);
                return;
            }

            s.setRemainingSeconds(remaining - 1);
        }, 20L, 20L);

        session.setCountdownTask(countdownTask);

        plugin.getLogger().info("⛓ Hapishane başladı: " + target.getName() + " | " + seconds + " saniye");
    }

    // =====================================================================
    // SÜRE GÜNCELLE
    // =====================================================================

    private void updatePrisonTime(Player target, int seconds, String username) {
        UUID uuid = target.getUniqueId();
        PrisonSession session = sessions.get(uuid);
        if (session == null) return;

        int oldRemaining = session.getRemainingSeconds();
        int newRemaining = oldRemaining + seconds;

        if (newRemaining <= 0) {
            target.sendTitle("§a✓ Serbest!", "§7Süre sıfırlandı, eski konumuna döndün.", 10, 40, 20);
            freePrisoner(target);
            return;
        }

        session.setRemainingSeconds(newRemaining);

        if (seconds > 0) {
            target.sendTitle(
                    "§c⛓ Süre Uzatıldı!",
                    "§c" + username + " §fsüreye §e+" + seconds + " §fsaniye ekledi! §7(Toplam: §e" + newRemaining + "§7s)",
                    10, 50, 10
            );
        } else {
            target.sendTitle(
                    "§e⛓ Süre Kısaltıldı!",
                    "§c" + username + " §fsüreden §e" + Math.abs(seconds) + " §fsaniye eksiltti! §7(Kalan: §e" + newRemaining + "§7s)",
                    10, 50, 10
            );
        }

        plugin.getLogger().info("⛓ Süre güncellendi: " + target.getName() +
                " | " + oldRemaining + "s → " + newRemaining + "s (" +
                (seconds > 0 ? "+" : "") + seconds + ")");
    }

    // =====================================================================
    // HAPİSHANEYİ İNŞA ET — 5x5 dış, 3x3 iç alan
    // =====================================================================

    private Location buildPrison(Player target, PrisonSession session) {
        FileConfiguration cfg = plugin.getConfig();
        String path = "parkours." + target.getUniqueId();

        if (!cfg.contains(path + ".baseX")) return null;

        int baseX = cfg.getInt(path + ".baseX");
        int baseZ = cfg.getInt(path + ".baseZ");
        int baseY = cfg.getInt(path + ".baseY");

        int centerX = baseX + 8;
        int centerZ = baseZ + 8;
        int prisonY = baseY + 5;

        World world = target.getWorld();

        // Taban (5x5)
        for (int x = centerX - 2; x <= centerX + 2; x++) {
            for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                Location loc = new Location(world, x, prisonY, z);
                session.addPrisonBlock(loc);
                loc.getBlock().setType(Material.STONE_BRICKS);
            }
        }

        // Duvarlar — dış çevre IRON_BARS, iç 3x3 boş
        for (int y = prisonY + 1; y <= prisonY + 3; y++) {
            for (int x = centerX - 2; x <= centerX + 2; x++) {
                for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                    if (x >= centerX - 1 && x <= centerX + 1
                            && z >= centerZ - 1 && z <= centerZ + 1) {
                        continue;
                    }
                    Location loc = new Location(world, x, y, z);
                    session.addPrisonBlock(loc);
                    loc.getBlock().setType(Material.IRON_BARS);
                }
            }
        }

        // Tavan (5x5)
        for (int x = centerX - 2; x <= centerX + 2; x++) {
            for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                Location loc = new Location(world, x, prisonY + 4, z);
                session.addPrisonBlock(loc);
                loc.getBlock().setType(Material.STONE_BRICKS);
            }
        }

        return new Location(world, centerX + 0.5, prisonY + 1, centerZ + 0.5);
    }

    // =====================================================================
    // ★ YENİ: KAZMA VER
    // =====================================================================

    public void givePrisonPickaxe(Player player) {
        UUID uuid = player.getUniqueId();

        prisonPickaxePlayers.add(uuid);

        // Netherite Pickaxe oluştur
        org.bukkit.inventory.ItemStack pickaxe = new org.bukkit.inventory.ItemStack(Material.NETHERITE_PICKAXE);
        org.bukkit.inventory.meta.ItemMeta meta = pickaxe.getItemMeta();
        meta.setDisplayName("§c⛓ Hapishane Kazması");
        meta.setLore(java.util.Arrays.asList(
                "§7Bu kazma ile sadece demir parmaklıkları kırabilirsin.",
                "§7Kır ve kaç!"
        ));

        // ★ Kırılmaz yap (infinite durability)
        meta.setUnbreakable(true);

        pickaxe.setItemMeta(meta);

        // Envanterinin ilk boş slotuna ver, doluysa elindekini değiştir
        player.getInventory().addItem(pickaxe);

        player.sendMessage("§a⛓ Hapishane kazması aldın! Demir parmaklıkları kır ve kaç!");
        player.sendTitle("§c⛓ KAÇIŞ!", "§7Demir parmaklıkları kır ve kaç!", 10, 40, 10);

        plugin.getLogger().info("⛓ Kazma verildi: " + player.getName());
    }

    // =====================================================================
    // ★ YENİ: OYUNCU HAPİSHANEDEN KAÇTI MI?
    // Demir parmaklık kırıldıktan sonra oyuncu hapishane dışına çıkınca çağrılır
    // =====================================================================

    public void onPlayerEscaped(Player player) {
        UUID uuid = player.getUniqueId();

        // Envanterde hâlâ kazma var mı diye kontrol et — bulk silme YOK
        if (!hasAnyPrisonPickaxeInInventory(player)) {
            prisonPickaxePlayers.remove(uuid);
        }

        freePrisoner(player);

        plugin.getLogger().info("⛓ Oyuncu kaçtı: " + player.getName());
    }

    private boolean hasAnyPrisonPickaxeInInventory(Player player) {
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.NETHERITE_PICKAXE) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName()
                        && meta.getDisplayName().equals("§c⛓ Hapishane Kazması")) {
                    return true;
                }
            }
        }
        return false;
    }

    public void freePrisoner(Player player) {
        UUID uuid = player.getUniqueId();
        PrisonSession session = sessions.get(uuid);
        if (session == null) return;

        session.cancelTasks();
        removePrisonStructure(session);

        if (player.isOnline()) {
            player.teleport(session.getSavedLocation());

            // ★ Ses artık ışınlandıktan SONRA, oyuncunun yeni (eski) konumunda çalıyor
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1.2f);

            player.sendTitle("§a✓ Serbest!", "§7Eski konumuna döndün.", 10, 40, 20);
            player.sendActionBar("§aSerbest kaldın!");
        }

        sessions.remove(uuid);
        plugin.getLogger().info("✓ Serbest bırakıldı: " + player.getName());
    }

    public void freePrisonerSilently(Player player) {
        UUID uuid = player.getUniqueId();
        PrisonSession session = sessions.get(uuid);
        if (session == null) return;

        session.cancelTasks();
        removePrisonStructure(session);

        // ★ Kazma silme YOK

        sessions.remove(uuid);
    }

    public void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        PrisonSession session = sessions.get(uuid);
        if (session == null) return;

        session.cancelTasks();
        removePrisonStructure(session);
        // ★ prisonPickaxePlayers.remove(uuid) YOK — kazmalar kalıcı
        sessions.remove(uuid);

        plugin.getLogger().info("⛓ Oyuncu çıktı, hapishane silindi: " + player.getName());
    }

    // =====================================================================
    // YAPIYI TEMİZLE
    // =====================================================================

    private void removePrisonStructure(PrisonSession session) {
        for (Location loc : session.getPrisonBlocks()) {
            try {
                loc.getBlock().setType(Material.AIR);
            } catch (Exception ignored) {}
        }
    }

    // =====================================================================
    // YARDIMCI
    // =====================================================================

    public boolean isInPrison(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public PrisonSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void clearAll() {
        for (PrisonSession session : sessions.values()) {
            session.cancelTasks();
            removePrisonStructure(session);
        }
        sessions.clear();
        prisonPickaxePlayers.clear();
    }
}