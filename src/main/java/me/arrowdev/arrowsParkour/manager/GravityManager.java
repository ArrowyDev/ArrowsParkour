package me.arrowdev.arrowsParkour.manager;

import me.arrowdev.arrowsParkour.ArrowsParkour;
import me.arrowdev.arrowsParkour.model.GravitySession;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GravityManager {

    private static final double LOW_GRAVITY = 0.015;
    private static final double HIGH_GRAVITY = 0.30;

    private final ArrowsParkour plugin;
    private final Map<UUID, GravitySession> sessions = new HashMap<>();
    private final Attribute gravityAttribute;

    public GravityManager(ArrowsParkour plugin) {
        this.plugin = plugin;
        this.gravityAttribute = resolveGravityAttribute();

        if (gravityAttribute != null) {
            plugin.getLogger().info("Gravity attribute bulundu: " + gravityAttribute);
        } else {
            plugin.getLogger().warning("Gravity attribute bulunamadı! Bu sürüm desteklenmiyor olabilir.");
        }
    }

    /**
     * Reflection ile sürümden bağımsız gravity attribute bulma.
     * 1.21.2+  -> Attribute.GRAVITY
     * 1.20.5 - 1.21.1 -> Attribute.GENERIC_GRAVITY
     */
    private Attribute resolveGravityAttribute() {
        // Önce yeni ismi dene (1.21.2+)
        try {
            return (Attribute) Attribute.class.getField("GRAVITY").get(null);
        } catch (Exception ignored) {}

        // Sonra eski ismi dene (1.20.5 - 1.21.1)
        try {
            return (Attribute) Attribute.class.getField("GENERIC_GRAVITY").get(null);
        } catch (Exception ignored) {}

        // Hiçbiri yoksa Registry dene
        try {
            org.bukkit.Registry<Attribute> registry = org.bukkit.Registry.ATTRIBUTE;
            Attribute attr = registry.get(org.bukkit.NamespacedKey.minecraft("generic.gravity"));
            if (attr != null) return attr;
            attr = registry.get(org.bukkit.NamespacedKey.minecraft("gravity"));
            if (attr != null) return attr;
        } catch (Exception ignored) {}

        return null;
    }

    public void startGravity(Player player, String mode, int seconds) {
        UUID uuid = player.getUniqueId();

        if (gravityAttribute == null) {
            player.sendMessage("§cBu sunucu sürümünde gravity attribute desteklenmiyor!");
            return;
        }

        if (sessions.containsKey(uuid)) {
            stopGravity(player, false);
        }

        AttributeInstance attr = player.getAttribute(gravityAttribute);
        if (attr == null) {
            player.sendMessage("§cBu sunucuda gravity attribute'u desteklenmiyor!");
            return;
        }

        double originalValue = attr.getBaseValue();
        double targetValue = mode.equalsIgnoreCase("low") ? LOW_GRAVITY : HIGH_GRAVITY;
        attr.setBaseValue(targetValue);

        GravitySession session = new GravitySession(seconds, originalValue, mode);
        sessions.put(uuid, session);

        BukkitTask actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            GravitySession s = sessions.get(uuid);
            if (s == null) return;
            String label = s.getMode().equalsIgnoreCase("low") ? "§b🪶 Düşük Yerçekimi" : "§4⬇ Yüksek Yerçekimi";
            player.sendActionBar(label + ": §e" + s.getRemainingSeconds() + " §fsaniye kaldı");
        }, 0L, 1L);
        session.setActionBarTask(actionBarTask);

        BukkitTask countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopGravity(player, false);
                return;
            }
            GravitySession s = sessions.get(uuid);
            if (s == null) return;

            if (s.getRemainingSeconds() <= 0) {
                stopGravity(player, true);
                return;
            }
            s.setRemainingSeconds(s.getRemainingSeconds() - 1);
        }, 20L, 20L);
        session.setCountdownTask(countdownTask);

        String modeLabel = mode.equalsIgnoreCase("low") ? "§b🪶 DÜŞÜK YERÇEKİMİ" : "§4⬇ YÜKSEK YERÇEKİMİ";
        player.sendTitle(modeLabel, "§7" + seconds + " saniye boyunca aktif!", 10, 50, 10);
        player.sendMessage(modeLabel + " §fetkinleştirildi! §7(" + seconds + "s)");

        plugin.getLogger().info("🪶 Gravity başladı: " + player.getName() + " | " + mode + " | " + seconds + "s");
    }

    public void stopGravity(Player player, boolean sendEndMessage) {
        UUID uuid = player.getUniqueId();
        GravitySession session = sessions.get(uuid);
        if (session == null) return;

        session.cancelTasks();

        if (gravityAttribute != null) {
            AttributeInstance attr = player.getAttribute(gravityAttribute);
            if (attr != null) {
                attr.setBaseValue(session.getOriginalValue());
            }
        }

        sessions.remove(uuid);

        if (sendEndMessage && player.isOnline()) {
            player.sendTitle("§a✓ NORMAL", "§7Yerçekimi eski haline döndü.", 10, 40, 10);
            player.sendMessage("§a🪶 Yerçekimi normale döndü!");
        }

        plugin.getLogger().info("🪶 Gravity bitti: " + player.getName());
    }

    public void onPlayerQuit(Player player) {
        if (sessions.containsKey(player.getUniqueId())) stopGravity(player, false);
    }

    public void onPlayerDeath(Player player) {
        if (sessions.containsKey(player.getUniqueId())) stopGravity(player, false);
    }

    public boolean hasGravity(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void clearAll() {
        for (Map.Entry<UUID, GravitySession> entry : sessions.entrySet()) {
            entry.getValue().cancelTasks();
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && gravityAttribute != null) {
                AttributeInstance attr = p.getAttribute(gravityAttribute);
                if (attr != null) attr.setBaseValue(entry.getValue().getOriginalValue());
            }
        }
        sessions.clear();
    }
}