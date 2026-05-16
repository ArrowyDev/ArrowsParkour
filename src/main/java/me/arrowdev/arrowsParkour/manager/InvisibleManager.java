package me.arrowdev.arrowsParkour.manager;

import me.arrowdev.arrowsParkour.ArrowsParkour;
import me.arrowdev.arrowsParkour.model.InvisibleSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InvisibleManager {

    private final ArrowsParkour plugin;
    private final Map<UUID, InvisibleSession> sessions = new HashMap<>();

    public InvisibleManager(ArrowsParkour plugin) {
        this.plugin = plugin;
    }

    public void startInvisible(Player player, int seconds) {
        UUID uuid = player.getUniqueId();

        if (sessions.containsKey(uuid)) {
            stopInvisible(player);
        }

        FileConfiguration cfg = plugin.getConfig();
        String path = "parkours." + uuid;

        if (!cfg.contains(path + ".blocks")) {
            player.sendMessage("§cParkurun yok!");
            return;
        }

        String worldName = cfg.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage("§cDünya bulunamadı!");
            return;
        }

        InvisibleSession session = new InvisibleSession(seconds);
        sessions.put(uuid, session);

        java.util.List<String> blockList = cfg.getStringList(path + ".blocks");
        int converted = 0;

        for (String blockStr : blockList) {
            try {
                String[] parts = blockStr.split(":");
                String[] coords = parts[0].split(",");

                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                int z = Integer.parseInt(coords[2]);

                Material original = parts.length > 1 ? Material.valueOf(parts[1]) : Material.STONE;

                if (original == Material.BARRIER || original == Material.AIR) continue;

                Location loc = new Location(world, x, y, z);
                session.addOriginalBlock(loc, original);
                loc.getBlock().setType(Material.BARRIER);
                converted++;
            } catch (Exception ignored) {}
        }

        // Action Bar
        BukkitTask actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            InvisibleSession s = sessions.get(uuid);
            if (s == null) return;
            player.sendActionBar("§7👻 Görünmez: §e" + s.getRemainingSeconds() + " §7saniye");
        }, 0L, 1L);
        session.setActionBarTask(actionBarTask);

        // Countdown
        BukkitTask countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            InvisibleSession s = sessions.get(uuid);
            if (s == null) return;

            if (s.getRemainingSeconds() <= 0) {
                stopInvisible(player);
                return;
            }
            s.setRemainingSeconds(s.getRemainingSeconds() - 1);
        }, 20L, 20L);
        session.setCountdownTask(countdownTask);

        player.sendTitle("§7👻 PARKUR GÖRÜNMEZ", "§7" + seconds + " saniye boyunca bloklar görünmez!", 10, 50, 10);
        player.sendMessage("§7👻 Parkur blokları §e" + seconds + " §7saniye boyunca görünmez yapıldı!");
    }

    public void stopInvisible(Player player) {
        UUID uuid = player.getUniqueId();
        InvisibleSession session = sessions.get(uuid);
        if (session == null) return;

        session.cancelTasks();
        revertBlocks(session);
        sessions.remove(uuid);

        if (player.isOnline()) {
            player.sendMessage("§a👻 Bloklar eski haline döndü!");
        }
    }

    public void stopInvisibleSilently(Player player) {
        UUID uuid = player.getUniqueId();
        InvisibleSession session = sessions.get(uuid);
        if (session == null) return;

        session.cancelTasks();
        revertBlocks(session);
        sessions.remove(uuid);
    }

    private void revertBlocks(InvisibleSession session) {
        for (Map.Entry<Location, Material> entry : session.getOriginalBlocks().entrySet()) {
            try {
                if (entry.getKey().getBlock().getType() == Material.BARRIER) {
                    entry.getKey().getBlock().setType(entry.getValue());
                }
            } catch (Exception ignored) {}
        }
    }

    public void onPlayerQuit(Player player) {
        if (sessions.containsKey(player.getUniqueId())) stopInvisibleSilently(player);
    }

    public void onPlayerDeath(Player player) {
        if (sessions.containsKey(player.getUniqueId())) stopInvisibleSilently(player);
    }

    public boolean hasInvisible(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void clearAll() {
        for (InvisibleSession session : sessions.values()) {
            session.cancelTasks();
            revertBlocks(session);
        }
        sessions.clear();
    }
}