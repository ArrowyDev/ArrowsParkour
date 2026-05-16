package me.arrowdev.arrowsParkour.manager;

import me.arrowdev.arrowsParkour.ArrowsParkour;
import me.arrowdev.arrowsParkour.model.PrisonSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PrisonManager {

    private final ArrowsParkour plugin;
    private final Map<UUID, PrisonSession> sessions = new HashMap<>();

    public PrisonManager(ArrowsParkour plugin) {
        this.plugin = plugin;
    }

    public void startPrison(Player target, int seconds, String username) {
        UUID uuid = target.getUniqueId();

        if (isInPrison(target)) {
            updatePrisonTime(target, seconds, username);
            return;
        }

        ParkourManager parkourManager = plugin.getParkourManager();
        if (parkourManager != null) {
            parkourManager.cancelCountdown(target);
        }

        // If palyer join the prison for the first time: save last position and build the prison
        Location saved = target.getLocation().clone();
        String subtitleText = "§c" + username + " §fseni hapise attı!";

        PrisonSession session = new PrisonSession(saved, seconds, subtitleText);
        sessions.put(uuid, session);

        // Build prison
        Location prisonCenter = buildPrison(target, session);
        if (prisonCenter == null) {
            target.sendMessage("§cHapishane inşa edilemedi! Parkur verisi yok.");
            sessions.remove(uuid);
            return;
        }

        // Teleport the player in prison
        Location teleportLoc = new Location(
                prisonCenter.getWorld(),
                prisonCenter.getX(),
                prisonCenter.getY(),
                prisonCenter.getZ(),
                saved.getYaw(),
                saved.getPitch()
        );
        target.teleport(teleportLoc);

        // Title + Subtitle
        target.sendTitle(
                "§4⛓ HAPİSHANE ⛓",
                subtitleText,
                10, 60, 20
        );

        // Action bar task
        BukkitTask actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!target.isOnline()) return;
            PrisonSession s = sessions.get(uuid);
            if (s == null) return;

            int remaining = s.getRemainingSeconds();
            String bar = "§c⛓ Hapishane: §e" + remaining + " §csaniye kaldı";
            target.sendActionBar(bar);
        }, 0L, 1L);

        session.setActionBarTask(actionBarTask);

        // Countdown task
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

    private void updatePrisonTime(Player target, int seconds, String username) {
        UUID uuid = target.getUniqueId();
        PrisonSession session = sessions.get(uuid);
        if (session == null) return;

        int oldRemaining = session.getRemainingSeconds();
        int newRemaining = oldRemaining + seconds;

        if (newRemaining <= 0) {
            target.sendTitle(
                    "§a✓ Serbest!",
                    "§7Süre sıfırlandı, eski konumuna döndün.",
                    10, 40, 20
            );
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

    private Location buildPrison(Player target, PrisonSession session) {
        FileConfiguration cfg = plugin.getConfig();
        String path = "parkours." + target.getUniqueId();

        if (!cfg.contains(path + ".baseX")) {
            return null;
        }

        int baseX = cfg.getInt(path + ".baseX");
        int baseZ = cfg.getInt(path + ".baseZ");
        int baseY = cfg.getInt(path + ".baseY");

        int centerX = baseX + 8;
        int centerZ = baseZ + 8;
        int prisonY = baseY + 5;

        org.bukkit.World world = target.getWorld();

        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                Location loc = new Location(world, x, prisonY, z);
                session.addPrisonBlock(loc);
                loc.getBlock().setType(Material.STONE_BRICKS);
            }
        }

        for (int y = prisonY + 1; y <= prisonY + 2; y++) {
            for (int x = centerX - 1; x <= centerX + 1; x++) {
                for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                    if (x == centerX && z == centerZ) continue;
                    Location loc = new Location(world, x, y, z);
                    session.addPrisonBlock(loc);
                    loc.getBlock().setType(Material.IRON_BARS);
                }
            }
        }

        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                Location loc = new Location(world, x, prisonY + 3, z);
                session.addPrisonBlock(loc);
                loc.getBlock().setType(Material.STONE_BRICKS);
            }
        }

        return new Location(world, centerX + 0.5, prisonY + 1, centerZ + 0.5);
    }

    public void freePrisoner(Player player) {
        UUID uuid = player.getUniqueId();
        PrisonSession session = sessions.get(uuid);
        if (session == null) return;

        session.cancelTasks();
        removePrisonStructure(session);

        if (player.isOnline()) {
            player.teleport(session.getSavedLocation());
            player.sendTitle("§a✓ Serbest!", "§7Eski konumuna döndün.", 10, 40, 20);
            player.sendActionBar("§aSerbest kaldın!");

            // ★ WIN NOKTASINA DÖNDÜYSE SAYACI BAŞLAT
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                ParkourManager parkourManager = plugin.getParkourManager();
                if (parkourManager == null) return;

                me.arrowdev.arrowsParkour.model.ParkourSession parkourSession =
                        parkourManager.getSession(player);
                if (parkourSession == null) return;

                if (!parkourManager.isInParkourWorld(player)) return;

                int currentY = player.getLocation().getBlockY();
                int startY = parkourSession.getStartY();
                int heightDiff = currentY - startY;

                if (heightDiff >= 100) {
                    parkourManager.startCountdownIfNeeded(player, heightDiff);
                    plugin.getLogger().info("✓ Hapis sonrası win noktası tespit edildi, sayaç başlatıldı: "
                            + player.getName() + " | yükseklik farkı: " + heightDiff);
                }
            }, 5L); // 5 tick bekle — teleport yerleşsin
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
        sessions.remove(uuid);
    }

    public void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        PrisonSession session = sessions.get(uuid);
        if (session == null) return;

        session.cancelTasks();
        removePrisonStructure(session);
        sessions.remove(uuid);

        plugin.getLogger().info("⛓ Oyuncu çıktı, hapishane silindi: " + player.getName());
    }

    private void removePrisonStructure(PrisonSession session) {
        for (Location loc : session.getPrisonBlocks()) {
            try {
                loc.getBlock().setType(Material.AIR);
            } catch (Exception ignored) {}
        }
    }

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
    }
}