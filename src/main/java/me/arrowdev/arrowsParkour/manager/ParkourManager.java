package me.arrowdev.arrowsParkour.manager;

import me.arrowdev.arrowsParkour.ArrowsParkour;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class ParkourManager {
    private final ArrowsParkour plugin;
    private final Map<UUID, ParkourSession> sessions;

    private static final int WIDTH = 15, LENGTH = 17, MAX_STEPS = 100;
    private static final int[] PATTERN = {8, 6, 8, 6};
    private static final int[][] DIRS = {{1,0}, {0,1}, {-1,0}, {0,-1}};

    public ParkourManager(ArrowsParkour plugin) {
        this.plugin = plugin;
        this.sessions = new HashMap<>();
        loadAll();
    }

    public void createFullParkour(Player player) {
        UUID uuid = player.getUniqueId();

        clearParkourCompletely(player);

        ParkourSession session = new ParkourSession(player);
        session.setStartY(player.getLocation().getBlockY());
        sessions.put(uuid, session);

        World world = player.getWorld();
        Location loc = player.getLocation();

        // Spiral'in dışarı taşmaması için daha geniş alan
        int baseX = loc.getBlockX() - WIDTH / 2;
        int baseZ = loc.getBlockZ() - LENGTH / 2;
        int baseY = loc.getBlockY() - 1;

        // Spiral'in ulaşabileceği max X/Z ofsetini hesapla
        // PATTERN toplamı = 8+6+8+6 = 28, iki tur = ~56 adım, her adım 2 blok ilerliyor
        // Güvenli margin için barrier'ı spiral alanına göre dinamik yapalım
        int spiralMaxOffset = 30; // spiral'in gidebileceği max mesafe
        int barrierMinX = -spiralMaxOffset - 2;
        int barrierMaxX = spiralMaxOffset + 2;
        int barrierMinZ = -spiralMaxOffset - 2;
        int barrierMaxZ = spiralMaxOffset + 2;

        // TABAN - barrier alanı kadar geniş
        for (int x = barrierMinX; x <= barrierMaxX; x++) {
            for (int z = barrierMinZ; z <= barrierMaxZ; z++) {
                Location b = new Location(world, baseX + WIDTH/2 + x, baseY, baseZ + LENGTH/2 + z);
                b.getBlock().setType(Material.STONE);
                session.addBlock(b);
            }
        }

        // BARRIER - spiral alanını tamamen çevreleyen duvarlar, +120 yükseklik
        int bX = baseX + WIDTH/2;
        int bZ = baseZ + LENGTH/2;
        for (int x = barrierMinX; x <= barrierMaxX; x++) {
            for (int z = barrierMinZ; z <= barrierMaxZ; z++) {
                if (x == barrierMinX || x == barrierMaxX || z == barrierMinZ || z == barrierMaxZ) {
                    for (int y = baseY; y <= baseY + MAX_STEPS + 22; y++) {
                        Location b = new Location(world, bX + x, y, bZ + z);
                        b.getBlock().setType(Material.BARRIER);
                        session.addBlock(b);
                    }
                }
            }
        }

        // SPIRAL - tek blok, 1 boşluk, Y sadece platform adımında artar
        int currentX = WIDTH / 2; // merkeze başla
        int currentZ = LENGTH / 2;
        int platformStep = 0;
        int patternIdx = 0;
        int remaining = PATTERN[0];

        while (platformStep < MAX_STEPS) {
            int dirIdx = patternIdx % 4;
            int[] dir = DIRS[dirIdx];
            int move = Math.min(remaining, MAX_STEPS - platformStep);

            for (int i = 0; i < move; i++) {
                int y = baseY + 1 + platformStep;

                // Platform koy
                Location block = new Location(world, baseX + currentX, y, baseZ + currentZ);
                block.getBlock().setType(Material.STONE);
                session.addBlock(block);

                // Platform adımı ilerle
                currentX += dir[0];
                currentZ += dir[1];
                platformStep++;

                // Boşluk - sadece X/Z ilerle, blok koyma, Y artma
                currentX += dir[0];
                currentZ += dir[1];

                if (platformStep >= MAX_STEPS) break;
            }

            remaining -= move;
            if (remaining <= 0) {
                patternIdx++;
                remaining = PATTERN[patternIdx % PATTERN.length];
            }
        }

        saveAll();

        player.sendMessage("§aParkur oluşturuldu!");
        player.sendMessage("§7+100 blok çık → kazanırsın.");
    }

    public ParkourSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void winParkour(Player player) {
        ParkourSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isCompleted()) return;

        session.setCompleted(true);

        player.sendMessage("§6🎉 TEBRİKLER! Kazandın! 🎉");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        player.getWorld().strikeLightningEffect(player.getLocation());
    }

    public void clearParkourCompletely(Player player) {
        UUID uuid = player.getUniqueId();
        ParkourSession session = sessions.remove(uuid);

        if (session != null) {
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
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.contains("parkours")) return;

        for (String uuidStr : cfg.getConfigurationSection("parkours").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) continue;

                ParkourSession session = new ParkourSession(p);
                session.setCompleted(cfg.getBoolean("parkours." + uuidStr + ".completed"));
                session.setStartY(cfg.getInt("parkours." + uuidStr + ".startY"));

                sessions.put(uuid, session);
            } catch (Exception ignored) {}
        }
    }

    public void clearAllSessions() {
        for (ParkourSession session : sessions.values()) {
            for (Location loc : session.getAllBlocks()) {
                loc.getBlock().setType(Material.AIR);
            }
        }
        sessions.clear();
    }
}