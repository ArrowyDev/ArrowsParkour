package me.arrowdev.arrowsParkour.manager;

import me.arrowdev.arrowsParkour.ArrowsParkour;
import me.arrowdev.arrowsParkour.model.ArrowRainSession;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class ArrowRainManager {

    private final ArrowsParkour plugin;
    private final Map<UUID, ArrowRainSession> sessions = new HashMap<>();
    private final Random random = new Random();

    public ArrowRainManager(ArrowsParkour plugin) {
        this.plugin = plugin;
    }

    public void startArrowRain(Player player, int seconds) {
        UUID uuid = player.getUniqueId();

        if (sessions.containsKey(uuid)) {
            stopArrowRain(player);
        }

        FileConfiguration cfg = plugin.getConfig();
        String path = "parkours." + uuid;

        if (!cfg.contains(path + ".baseX")) {
            player.sendMessage("§cParkurun yok!");
            return;
        }

        int baseX = cfg.getInt(path + ".baseX");
        int baseZ = cfg.getInt(path + ".baseZ");
        String worldName = cfg.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            player.sendMessage("§cDünya bulunamadı!");
            return;
        }

        ArrowRainSession session = new ArrowRainSession(seconds);
        sessions.put(uuid, session);

        // ★ Ok spawn task — her 4 tickte 3-5 ok yağdır
        BukkitTask spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopArrowRain(player);
                return;
            }

            ArrowRainSession s = sessions.get(uuid);
            if (s == null) return;

            int arrowCount = 3 + random.nextInt(3); // 3-5 ok

            for (int i = 0; i < arrowCount; i++) {
                int x = baseX + random.nextInt(17);
                int z = baseZ + random.nextInt(17);
                // Oyuncunun 15-25 blok yukarısından yağsın
                int y = player.getLocation().getBlockY() + 15 + random.nextInt(11);

                Location spawnLoc = new Location(world, x + 0.5, y, z + 0.5);

                // Hafif rastgele yön sapması
                double offsetX = (random.nextDouble() - 0.5) * 0.3;
                double offsetZ = (random.nextDouble() - 0.5) * 0.3;
                Vector direction = new Vector(offsetX, -1.0, offsetZ).normalize();

                Arrow arrow = world.spawnArrow(spawnLoc, direction, 0.8f, 2.0f);
                arrow.setDamage(2.0);       // Hafif hasar
                arrow.setKnockbackStrength(1);
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                arrow.setFireTicks(0);

                s.addArrow(arrow);
            }
        }, 0L, 4L);

        session.setSpawnTask(spawnTask);

        // ★ Yerde kalan okları temizle (her 2 saniyede)
        BukkitTask cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ArrowRainSession s = sessions.get(uuid);
            if (s == null) return;

            s.getArrows().removeIf(arrow -> {
                if (arrow == null || !arrow.isValid()) return true;
                if (arrow.isInBlock() || arrow.isOnGround()) {
                    arrow.remove();
                    return true;
                }
                // 5 saniyeden uzun yaşayan okları sil
                if (arrow.getTicksLived() > 100) {
                    arrow.remove();
                    return true;
                }
                return false;
            });
        }, 40L, 40L);

        session.setCleanupTask(cleanupTask);

        // ★ Geri sayım
        BukkitTask countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopArrowRain(player);
                return;
            }

            ArrowRainSession s = sessions.get(uuid);
            if (s == null) return;

            if (s.getRemainingSeconds() <= 0) {
                stopArrowRain(player);
                return;
            }

            s.setRemainingSeconds(s.getRemainingSeconds() - 1);
        }, 20L, 20L);

        session.setCountdownTask(countdownTask);

        plugin.getLogger().info("🏹 ArrowRain başladı: " + player.getName() + " | " + seconds + "s");
    }

    public void stopArrowRain(Player player) {
        UUID uuid = player.getUniqueId();
        ArrowRainSession session = sessions.get(uuid);
        if (session == null) return;

        session.cancelTasks();
        session.removeAllArrows();
        sessions.remove(uuid);


        plugin.getLogger().info("🏹 ArrowRain bitti: " + player.getName());
    }

    public void onPlayerQuit(Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            stopArrowRain(player);
        }
    }

    public void onPlayerDeath(Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            stopArrowRain(player);
        }
    }

    public boolean hasArrowRain(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void clearAll() {
        for (ArrowRainSession session : sessions.values()) {
            session.cancelTasks();
            session.removeAllArrows();
        }
        sessions.clear();
    }
}