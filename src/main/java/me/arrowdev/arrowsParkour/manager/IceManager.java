package me.arrowdev.arrowsParkour.manager;

import me.arrowdev.arrowsParkour.ArrowsParkour;
import me.arrowdev.arrowsParkour.model.IceSession;
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

public class IceManager {

    private final ArrowsParkour plugin;
    private final Map<UUID, IceSession> sessions = new HashMap<>();

    public IceManager(ArrowsParkour plugin) {
        this.plugin = plugin;
    }

    public void startIce(Player player, int seconds) {
        UUID uuid = player.getUniqueId();

        // Zaten aktifse önce durdur (revert et)
        if (sessions.containsKey(uuid)) {
            stopIce(player);
        }

        FileConfiguration cfg = plugin.getConfig();
        String path = "parkours." + uuid;

        // Config'de parkur var mı kontrol et
        if (!cfg.contains(path + ".blocks")) {
            player.sendMessage("§cParkurun yok, buz uygulanamaz!");
            return;
        }

        String worldName = cfg.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            player.sendMessage("§cDünya bulunamadı!");
            return;
        }

        IceSession session = new IceSession(seconds);
        sessions.put(uuid, session);

        // Config'deki tüm blokları oku → buz yap → orijinali sakla
        java.util.List<String> blockList = cfg.getStringList(path + ".blocks");
        int converted = 0;

        for (String blockStr : blockList) {
            try {
                String[] parts = blockStr.split(":");
                String[] coords = parts[0].split(",");

                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                int z = Integer.parseInt(coords[2]);

                Material originalMat = Material.STONE;
                if (parts.length > 1) {
                    try {
                        originalMat = Material.valueOf(parts[1]);
                    } catch (IllegalArgumentException ignored) {}
                }

                // Barrier ve AIR bloklarını değiştirme
                if (originalMat == Material.BARRIER || originalMat == Material.AIR) continue;

                Location loc = new Location(world, x, y, z);

                // Orijinal materyali sakla
                session.addOriginalBlock(loc, originalMat);

                // Buza çevir
                loc.getBlock().setType(Material.ICE);
                converted++;

            } catch (Exception ignored) {}
        }

        plugin.getLogger().info("🧊 Buz başladı: " + player.getName()
                + " | " + converted + " blok | " + seconds + "s");

        // Action bar task
        BukkitTask actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            IceSession s = sessions.get(uuid);
            if (s == null) return;

            player.sendActionBar("§b🧊 Buz: §e" + s.getRemainingSeconds() + " §bsaniye kaldı");
        }, 0L, 1L);

        session.setActionBarTask(actionBarTask);

        // Countdown task
        BukkitTask countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;

            IceSession s = sessions.get(uuid);
            if (s == null) return;

            int remaining = s.getRemainingSeconds();

            if (remaining <= 0) {
                stopIce(player);
                return;
            }

            s.setRemainingSeconds(remaining - 1);
        }, 20L, 20L);

        session.setCountdownTask(countdownTask);

        player.sendTitle(
                "§b🧊 PARKUR DONDU",
                "§7" + seconds + " saniye boyunca buzda koşuyorsun!",
                10, 50, 10
        );
        player.sendMessage("§b🧊 Parkur blokları §e" + seconds + " §bsaniye boyunca buza döndü!");
    }

    public void stopIce(Player player) {
        UUID uuid = player.getUniqueId();
        IceSession session = sessions.get(uuid);
        if (session == null) return;

        session.cancelTasks();
        revertBlocks(session);
        sessions.remove(uuid);

        if (player.isOnline()) {
            player.sendMessage("§a🧊 Parkur blokları eski haline döndü!");
            player.sendActionBar("§aBuz eridi!");
        }

        plugin.getLogger().info("🧊 Buz bitti: " + player.getName());
    }

    public void stopIceSilently(Player player) {
        UUID uuid = player.getUniqueId();
        IceSession session = sessions.get(uuid);
        if (session == null) return;

        session.cancelTasks();
        revertBlocks(session);
        sessions.remove(uuid);
    }

    private void revertBlocks(IceSession session) {
        for (Map.Entry<Location, Material> entry : session.getOriginalBlocks().entrySet()) {
            try {
                Location loc = entry.getKey();
                Material original = entry.getValue();

                // Sadece hala buz olan blokları geri döndür
                // (Başka bir şey tarafından değiştirilmiş olabilir)
                if (loc.getBlock().getType() == Material.ICE) {
                    loc.getBlock().setType(original);
                }
            } catch (Exception ignored) {}
        }
    }

    public void onPlayerQuit(Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            stopIceSilently(player);
            plugin.getLogger().info("🧊 Oyuncu çıktı, buz silindi: " + player.getName());
        }
    }

    public void onPlayerDeath(Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            stopIceSilently(player);
            plugin.getLogger().info("🧊 Oyuncu öldü, buz silindi: " + player.getName());
        }
    }

    public boolean hasIce(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void clearAll() {
        for (Map.Entry<UUID, IceSession> entry : sessions.entrySet()) {
            entry.getValue().cancelTasks();
            revertBlocks(entry.getValue());
        }
        sessions.clear();
    }
}