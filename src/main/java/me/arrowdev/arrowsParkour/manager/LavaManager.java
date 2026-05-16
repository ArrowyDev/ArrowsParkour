package me.arrowdev.arrowsParkour.manager;

import me.arrowdev.arrowsParkour.ArrowsParkour;
import me.arrowdev.arrowsParkour.model.LavaSession;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LavaManager {

    private final ArrowsParkour plugin;
    private final Map<UUID, LavaSession> sessions = new HashMap<>();

    public LavaManager(ArrowsParkour plugin) {
        this.plugin = plugin;
    }

    public void startLava(Player player, int intervalTicks) {
        UUID uuid = player.getUniqueId();

        // Zaten aktifse önce durdur
        if (sessions.containsKey(uuid)) {
            stopLava(player);
        }

        FileConfiguration cfg = plugin.getConfig();
        String path = "parkours." + uuid;

        if (!cfg.contains(path + ".baseX")) {
            player.sendMessage("§cParkurun yok!");
            return;
        }

        int baseX = cfg.getInt(path + ".baseX");
        int baseZ = cfg.getInt(path + ".baseZ");
        int baseY = cfg.getInt(path + ".baseY");
        String worldName = cfg.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            player.sendMessage("§cDünya bulunamadı!");
            return;
        }

        int startY = baseY + 1;
        int maxY = baseY + 99;

        LavaSession session = new LavaSession(startY, maxY, baseX, baseZ);
        sessions.put(uuid, session);

        // Lav yükselme task'ı
        BukkitTask risingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopLava(player);
                return;
            }

            LavaSession s = sessions.get(uuid);
            if (s == null) return;

            int y = s.getCurrentY();
            if (y > s.getMaxY()) {
                player.sendMessage("§c🌋 Lav zirveye ulaştı!");
                stopLava(player);
                return;
            }

            // Bu Y seviyesini lavla doldur (sadece AIR blokları)
            int filled = 0;
            for (int x = baseX; x < baseX + 17; x++) {
                for (int z = baseZ; z < baseZ + 17; z++) {
                    Location loc = new Location(world, x, y, z);
                    Material blockType = loc.getBlock().getType();
                    if (blockType == Material.AIR || blockType == Material.CAVE_AIR) {
                        loc.getBlock().setType(Material.LAVA);
                        s.addLavaBlock(loc);
                        filled++;
                    }
                }
            }

            s.setCurrentY(y + 1);

            int level = y - baseY;

        }, 0L, (long) intervalTicks);

        session.setRisingTask(risingTask);

        // Action bar task
        BukkitTask actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            LavaSession s = sessions.get(uuid);
            if (s == null) return;

            int level = s.getCurrentY() - baseY - 1;
        }, 0L, 5L);

        session.setActionBarTask(actionBarTask);

        player.sendMessage("§c🌋 Lav yükselmeye başladı!");
        player.sendTitle("§c🌋 LAV YÜKSELİYOR!", "§7Parkuru tamamlamaya çalış!", 10, 40, 20);

        plugin.getLogger().info("🌋 Lav başladı: " + player.getName() + " | Hız: " + intervalTicks + " tick");
    }

    public void stopLava(Player player) {
        UUID uuid = player.getUniqueId();
        LavaSession session = sessions.get(uuid);
        if (session == null) return;

        session.cancelTasks();
        removeLavaBlocks(session);
        sessions.remove(uuid);

        if (player.isOnline()) {
            player.sendMessage("§a🌋 Lav durduruldu ve temizlendi!");
            player.sendActionBar("§aLav temizlendi!");
        }

        plugin.getLogger().info("🌋 Lav durduruldu: " + player.getName());
    }

    private void removeLavaBlocks(LavaSession session) {
        for (Location loc : session.getLavaBlocks()) {
            try {
                if (loc.getBlock().getType() == Material.LAVA) {
                    loc.getBlock().setType(Material.AIR);
                }
            } catch (Exception ignored) {}
        }
    }

    public void onPlayerQuit(Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            stopLava(player);
        }
    }

    public void onPlayerDeath(Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            stopLava(player);
        }
    }

    public boolean hasLava(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public LavaSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void clearAll() {
        for (LavaSession session : sessions.values()) {
            session.cancelTasks();
            removeLavaBlocks(session);
        }
        sessions.clear();
    }
}