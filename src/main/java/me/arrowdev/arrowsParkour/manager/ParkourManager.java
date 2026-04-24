package me.arrowdev.arrowsParkour.manager;

import me.arrowdev.arrowsParkour.ArrowsParkour;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.*;

public class ParkourManager {
    private final ArrowsParkour plugin;
    private final Map<UUID, ParkourSession> sessions;
    private final Map<UUID, BukkitTask> countdownTasks;
    private final Map<UUID, Integer> countdownSeconds;
    private final Set<UUID> loadedPlayers;
    private final Map<UUID, Long> frozenPlayers = new HashMap<>();

    private static final int WIDTH = 20, LENGTH = 20, MAX_STEPS = 100;
    private static final int[] PATTERN = {8, 8, 8, 8};
    private static final int[][] DIRS = {{1,0}, {0,1}, {-1,0}, {0,-1}};

    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public void createOrUpdateBossBar(Player player) {
        UUID uuid = player.getUniqueId();
        FileConfiguration cfg = plugin.getConfig();

        int wins = cfg.getInt("parkours." + uuid + ".wins", 0);

        BossBar bar = bossBars.get(uuid);

        if (bar == null) {
            bar = Bukkit.createBossBar(
                    "§6Win: §a" + wins,
                    BarColor.GREEN,
                    BarStyle.SOLID
            );
            bar.addPlayer(player);
            bar.setVisible(true);
            bossBars.put(uuid, bar);
        } else {
            bar.setTitle("§6Win: §a" + wins);
        }
    }

    public void removeBossBar(Player player) {
        UUID uuid = player.getUniqueId();

        BossBar bar = bossBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
    }

    public void freezePlayer(Player player, int seconds) {
        UUID uuid = player.getUniqueId();

        long endTime = System.currentTimeMillis() + (seconds * 1000L);
        frozenPlayers.put(uuid, endTime);

        player.sendMessage("§c" + seconds + " saniye boyunca hareket edemezsin!");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (frozenPlayers.containsKey(uuid)) {
                frozenPlayers.remove(uuid);
                player.sendMessage("§aArtık hareket edebilirsin!");
            }
        }, seconds * 20L);
    }

    public boolean isFrozen(Player player) {
        UUID uuid = player.getUniqueId();

        if (!frozenPlayers.containsKey(uuid)) return false;

        long end = frozenPlayers.get(uuid);

        if (System.currentTimeMillis() >= end) {
            frozenPlayers.remove(uuid);
            return false;
        }

        return true;
    }

    public ParkourManager(ArrowsParkour plugin) {
        this.plugin = plugin;
        this.sessions = new HashMap<>();
        this.countdownTasks = new HashMap<>();
        this.countdownSeconds = new HashMap<>();
        this.loadedPlayers = new HashSet<>();
    }

    public void initialize() {
        Bukkit.getScheduler().runTaskLater(plugin, this::loadAll, 1L);
    }

    public void onPlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();

        if (!loadedPlayers.contains(uuid)) {
            loadPlayerParkour(uuid, player);
            loadedPlayers.add(uuid);
        }
    }

    private void loadPlayerParkour(UUID uuid, Player player) {
        FileConfiguration cfg = plugin.getConfig();
        String path = "parkours." + uuid;

        if (!cfg.contains(path)) {
            plugin.getLogger().info("Oyuncunun parkoru yok: " + player.getName());
            return;
        }

        try {
            String worldName = cfg.getString(path + ".world");
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                plugin.getLogger().warning("❌ Dünya bulunamadı: " + worldName);
                return;
            }

            ParkourSession session = new ParkourSession(player);
            session.setCompleted(cfg.getBoolean(path + ".completed", false));
            session.setStartY(cfg.getInt(path + ".startY", 64));

            List<String> blockLocations = cfg.getStringList(path + ".blocks");

            if (blockLocations.isEmpty()) {
                plugin.getLogger().warning("❌ Blok listesi boş: " + uuid);
                return;
            }

            List<String> jumpList = cfg.getStringList(path + ".jumpBlocks");

            for (String s : jumpList) {
                String[] parts = s.split(",");

                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);

                Location loc = new Location(world, x, y, z);
                session.addJumpBlock(loc);
            }

            int blocksLoaded = 0;
            for (String blockStr : blockLocations) {
                try {
                    String[] parts = blockStr.split(":");
                    String[] coords = parts[0].split(",");

                    if (coords.length != 3) continue;

                    int x = Integer.parseInt(coords[0]);
                    int y = Integer.parseInt(coords[1]);
                    int z = Integer.parseInt(coords[2]);

                    Location blockLoc = new Location(world, x, y, z);

                    Material material = Material.STONE;
                    if (parts.length > 1) {
                        try {
                            material = Material.valueOf(parts[1]);
                        } catch (IllegalArgumentException ignored) {
                            plugin.getLogger().warning("⚠ Bilinmeyen material: " + parts[1]);
                            material = Material.STONE;
                        }
                    }

                    session.addBlock(blockLoc, material);
                    blockLoc.getBlock().setType(material);

                    plugin.getLogger().info("✓ Blok yüklendi: " + x + "," + y + "," + z + " -> " + material.name());
                    blocksLoaded++;
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("⚠ Blok parse hatası: " + blockStr);
                }
            }

            sessions.put(uuid, session);
            plugin.getLogger().info("✓ Parkour yüklendi: " + player.getName() + " (" + blocksLoaded + " blok)");

        } catch (Exception e) {
            plugin.getLogger().warning("❌ Parkour yükleme hatası: " + uuid);
            e.printStackTrace();
        }
    }

    public void startActionBarTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                ParkourSession session = getSession(p);
                if (session == null) continue;

                int currentY = p.getLocation().getBlockY();
                int startY = session.getStartY();
                int height = currentY - startY;

                int forward = session.getForwardProtection();
                int backward = session.getBackwardProtection();
                int net = forward - backward;

                String netDisplay;
                if (net > 0) {
                    netDisplay = "§aNet: +" + net;
                } else if (net < 0) {
                    netDisplay = "§cNet: " + net;
                } else {
                    netDisplay = "§7Net: 0";
                }

                String actionBar =
                        "§eYükseklik: §a" + height +
                                " §7| §aİleri: " + forward +
                                " §7| §cGeri: " + backward +
                                " §7| " + netDisplay;

                p.sendActionBar(actionBar);
            }
        }, 0L, 2L); // 2L yaparak daha az sık update et
    }

    public void createFullParkour(Player player) {
        UUID uuid = player.getUniqueId();

        clearParkourCompletely(player);

        ParkourSession session = new ParkourSession(player);
        session.setStartY(player.getLocation().getBlockY());
        sessions.put(uuid, session);

        World world = player.getWorld();
        Location loc = player.getLocation();

        Vector dir = loc.getDirection().normalize();
        Vector left = new Vector(-dir.getZ(), 0, dir.getX()).normalize();

        Location start = loc.clone().add(left.multiply(2));

        int baseX = start.getBlockX();
        int baseZ = start.getBlockZ();
        int baseY = loc.getBlockY() - 1;

        int baseWidth = 17;
        int baseLength = 17;

        plugin.getLogger().info("📍 Base oluşturuluyor: baseX=" + baseX + ", baseZ=" + baseZ + ", baseY=" + baseY);

        for (int x = 0; x < baseWidth; x++) {
            for (int z = 0; z < baseLength; z++) {
                Location b = new Location(world, baseX + x, baseY, baseZ + z);
                b.getBlock().setType(Material.STONE);
                session.addBlock(b, Material.STONE);
            }
        }

        plugin.getLogger().info("✓ Base blokları eklendi: " + (baseWidth * baseLength) + " blok");

        int minX = -1;
        int maxX = baseWidth;
        int minZ = -1;
        int maxZ = baseLength;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (x == minX || x == maxX || z == minZ || z == maxZ) {
                    for (int y = baseY; y <= baseY + MAX_STEPS + 10; y++) {
                        Location b = new Location(world, baseX + x, y, baseZ + z);
                        b.getBlock().setType(Material.BARRIER);
                        session.addBlock(b, Material.BARRIER);
                    }
                }
            }
        }

        plugin.getLogger().info("✓ Barrier duvarları eklendi");

        int currentX = 0;
        int currentZ = 0;
        int platformStep = 0;
        int patternIdx = 0;
        int remaining = PATTERN[0];

        Location lastBlock = null;

        while (platformStep < MAX_STEPS) {
            int dirIdx = patternIdx % 4;
            int[] dirArr = DIRS[dirIdx];
            int move = Math.min(remaining, MAX_STEPS - platformStep);

            for (int i = 0; i < move; i++) {
                int y = baseY + 1 + platformStep;

                Location block = new Location(world, baseX + currentX, y, baseZ + currentZ);
                block.getBlock().setType(Material.STONE);
                session.addBlock(block, Material.STONE);
                session.addJumpBlock(block);
                lastBlock = block;

                currentX += dirArr[0];
                currentZ += dirArr[1];
                platformStep++;

                currentX += dirArr[0];
                currentZ += dirArr[1];

                if (platformStep >= MAX_STEPS) break;
            }

            remaining -= move;
            if (remaining <= 0) {
                patternIdx++;
                remaining = PATTERN[patternIdx % PATTERN.length];
            }
        }

        plugin.getLogger().info("✓ Spiral platformları eklendi: " + MAX_STEPS + " step");

        FileConfiguration cfg = plugin.getConfig();
        cfg.set("parkours." + uuid + ".winX", lastBlock.getBlockX());
        cfg.set("parkours." + uuid + ".winY", lastBlock.getBlockY());
        cfg.set("parkours." + uuid + ".winZ", lastBlock.getBlockZ());

        cfg.set("parkours." + uuid + ".baseX", baseX);
        cfg.set("parkours." + uuid + ".baseZ", baseZ);
        cfg.set("parkours." + uuid + ".baseY", baseY);

        plugin.saveConfig();

        saveParkourSession(uuid, session, baseX, baseZ, baseY);

        player.sendMessage("§aParkur oluşturuldu! (" + session.getAllBlocks().size() + " blok)");
        player.sendMessage("§7+100 blok çık → her 100 blokta kazanırsın!");
        plugin.getLogger().info("✅ Parkur oluşturuldu: " + player.getName() + " - Toplam Blok: " + session.getAllBlocks().size());
    }

    public void saveParkourSession(UUID uuid, ParkourSession session, int baseX, int baseZ, int baseY) {
        FileConfiguration cfg = plugin.getConfig();

        cfg.set("parkours." + uuid + ".completed", session.isCompleted());
        cfg.set("parkours." + uuid + ".startY", session.getStartY());
        cfg.set("parkours." + uuid + ".baseX", baseX);
        cfg.set("parkours." + uuid + ".baseZ", baseZ);
        cfg.set("parkours." + uuid + ".baseY", baseY);
        cfg.set("parkours." + uuid + ".world", session.getPlayer().getWorld().getName());

        List<String> blockLocations = new ArrayList<>();
        int stoneCount = 0, barrierCount = 0, otherCount = 0;

        for (Location loc : session.getAllBlocks()) {
            String key = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            Material material = session.getBlockMaterials().getOrDefault(key, Material.STONE);
            blockLocations.add(key + ":" + material.name());

            if (material == Material.STONE) stoneCount++;
            else if (material == Material.BARRIER) barrierCount++;
            else otherCount++;

            plugin.getLogger().info("💾 Kaydediliyor: " + key + " -> " + material.name());
        }

        List<String> jumpList = new ArrayList<>();

        for (Location loc : session.getJumpBlocks()) {
            jumpList.add(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        }

        cfg.set("parkours." + uuid + ".jumpBlocks", jumpList);

        cfg.set("parkours." + uuid + ".blocks", blockLocations);
        plugin.saveConfig();

        plugin.getLogger().info("✅ Parkour config'e kaydedildi!");
        plugin.getLogger().info("  📊 Stone: " + stoneCount + ", Barrier: " + barrierCount + ", Diğer: " + otherCount);
        plugin.getLogger().info("  📝 Toplam blok: " + blockLocations.size());
    }

    public ParkourSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void startCountdownIfNeeded(Player player, int heightDifference) {
        UUID uuid = player.getUniqueId();
        int level = heightDifference / 100;

        if (countdownTasks.containsKey(uuid)) {
            BukkitTask oldTask = countdownTasks.remove(uuid);
            if (oldTask != null) oldTask.cancel();
            countdownSeconds.remove(uuid);
        }

        countdownSeconds.put(uuid, 10);
        ParkourSession session = getSession(player);
        if (session == null) return;

        FileConfiguration cfg = plugin.getConfig();
        String path = "parkours." + uuid + ".wins";

        int baseX = cfg.getInt("parkours." + uuid + ".baseX");
        int baseZ = cfg.getInt("parkours." + uuid + ".baseZ");

        int baseWidth = 17;
        int baseLength = 17;

        Location startLoc = new Location(
                player.getWorld(),
                baseX + (baseWidth / 2.0),
                session.getStartY() + 1,
                baseZ + (baseLength / 2.0)
        );

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            Integer remaining = countdownSeconds.get(uuid);
            if (remaining == null) return;

            if (remaining > 0) {
                player.sendTitle("§6" + remaining, "§7Başlangıca ışınlanıyorsun...", 0, 20, 0);
                countdownSeconds.put(uuid, remaining - 1);
            } else {
                player.teleport(startLoc);
                player.sendMessage("§eBaşlangıca ışınlandın!");

                // Korumaları sıfırla
                session.setForwardProtection(0);
                session.setBackwardProtection(0);

                // Win sayısını arttır
                int currentWins = cfg.getInt(path, 0);
                cfg.set(path, currentWins + 1);
                plugin.saveConfig();
                createOrUpdateBossBar(player);

                // Clean old countdown
                BukkitTask t = countdownTasks.remove(uuid);
                if (t != null) t.cancel();
                countdownSeconds.remove(uuid);
            }

        }, 0L, 20L);

        countdownTasks.put(uuid, task);
    }

    public void cancelCountdown(Player player) {
        UUID uuid = player.getUniqueId();

        if (countdownTasks.containsKey(uuid)) {
            BukkitTask task = countdownTasks.remove(uuid);
            task.cancel();
            countdownSeconds.remove(uuid);
        }
    }

    public void clearParkourCompletely(Player player) {
        UUID uuid = player.getUniqueId();

        if (countdownTasks.containsKey(uuid)) {
            countdownTasks.get(uuid).cancel();
            countdownTasks.remove(uuid);
        }

        countdownSeconds.remove(uuid);

        ParkourSession session = sessions.remove(uuid);

        if (session != null) {
            plugin.getLogger().info("Parkour siliniyor: " + player.getName() + " (" + session.getAllBlocks().size() + " blok)");
            for (Location loc : session.getAllBlocks()) {
                loc.getBlock().setType(Material.AIR);
            }
        }

        plugin.getConfig().set("parkours." + uuid, null);
        plugin.saveConfig();
    }

    public void clearParkour(Player player) {
        clearParkourCompletely(player);
        player.sendMessage("§eParkur temizlendi!");
    }

    public void saveAll() {
        FileConfiguration cfg = plugin.getConfig();

        for (Map.Entry<UUID, ParkourSession> e : sessions.entrySet()) {
            cfg.set("parkours." + e.getKey() + ".completed", e.getValue().isCompleted());
            cfg.set("parkours." + e.getKey() + ".startY", e.getValue().getStartY());
        }

        plugin.saveConfig();
    }

    private void loadAll() {
        plugin.reloadConfig();
        plugin.getLogger().info("Config yeniden yüklendi.");
    }

    public ArrowsParkour getPlugin() {
        return plugin;
    }

    public void clearAllSessions() {
        plugin.getLogger().info("clearAllSessions çağrıldı!");

        for (BukkitTask task : countdownTasks.values()) {
            try {
                task.cancel();
            } catch (Exception ignored) {}
        }
        countdownTasks.clear();
        countdownSeconds.clear();

        for (ParkourSession session : sessions.values()) {
            for (Location loc : session.getAllBlocks()) {
                try {
                    loc.getBlock().setType(Material.AIR);
                } catch (Exception ignored) {}
            }
        }

        sessions.clear();
        loadedPlayers.clear();
    }
}