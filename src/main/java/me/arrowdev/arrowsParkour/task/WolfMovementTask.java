package me.arrowdev.arrowsParkour.task;

import me.arrowdev.arrowsParkour.ArrowsParkour;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class WolfMovementTask extends BukkitRunnable {

    private final ArrowsParkour plugin;
    private final Player player;
    private final Wolf wolf;
    private final ParkourSession session;
    private final List<Location> jumpBlocks;
    private final int targetIndex;

    private int currentIndex;
    private int ticks = 0;
    private static final int MAX_TICKS = 1200; // 60 saniye timeout

    // 🟢 DÜZELTİLDİ: Varış mesafesi 0.45'ten 0.3'e düşürüldü.
    // Artık kurt hedefe çok daha yaklaşmadan "vardım" demeyecek.
    private static final double ARRIVAL_DISTANCE = 0.3;

    public WolfMovementTask(ArrowsParkour plugin, Player player, Wolf wolf,
                            ParkourSession session, Location targetLoc, int targetBlockIndex) {
        this.plugin = plugin;
        this.player = player;
        this.wolf = wolf;
        this.session = session;
        this.jumpBlocks = session.getJumpBlocks();
        this.targetIndex = targetBlockIndex;

        this.currentIndex = findNearestIndex(wolf.getLocation());
    }

    @Override
    public void run() {
        ticks++;

        if (!player.isOnline() || wolf == null || !wolf.isValid()) {
            cleanup();
            cancel();
            return;
        }

        if (ticks > MAX_TICKS) {
            player.sendMessage("§cKurt hareketi zaman aşımına uğradı!");
            cleanup();
            cancel();
            return;
        }

        // Hedefe ulaştık mı?
        if (currentIndex >= targetIndex) {
            finish();
            cancel();
            return;
        }

        // Mevcut takip edilen hedef blok
        Location currentTarget = jumpBlocks.get(currentIndex).clone().add(0.5, 1.2, 0.5);
        double distance = wolf.getLocation().distance(currentTarget);

        // Bloğa yeterince yaklaştıysa sıradakine geç
        if (distance < ARRIVAL_DISTANCE) {
            currentIndex++;
            return;
        }

        // Kurdu yumuşakça hareket ettir
        moveToward(currentTarget);

        // Herhangi bir sebeple oyuncu binekten indiyse geri bindir
        if (!wolf.getPassengers().contains(player)) {
            wolf.addPassenger(player);
        }

        if (ticks % 20 == 0) {
            plugin.getLogger().info("🐺 Wolf | Index: " + currentIndex + "/" + targetIndex +
                    " | Mesafe: " + String.format("%.2f", distance));
        }
    }

    private void moveToward(Location target) {
        Location wolfLoc = wolf.getLocation();

        double dx = target.getX() - wolfLoc.getX();
        double dy = target.getY() - wolfLoc.getY();
        double dz = target.getZ() - wolfLoc.getZ();

        double hDist = Math.sqrt(dx * dx + dz * dz);

        // 🟢 DÜZELTİLDİ: Hedefe yaklaşınca daha fazla yavaşla (0.35 -> 0.25)
        // Bu sayede hedefin önünde savrulmayı önler.
        double speed = Math.min(0.25, hDist * 0.4);

        double vx = 0, vz = 0;
        if (hDist > 0.01) {
            vx = (dx / hDist) * speed;
            vz = (dz / hDist) * speed;
        }

        // Dikey hareketi de biraz daha hassas yaptık
        double vy = Math.max(-0.15, Math.min(0.2, dy * 0.4));

        wolf.setVelocity(new Vector(vx, vy, vz));

        // Kurdu hedefe doğru baktır
        Location face = wolfLoc.clone();
        face.setDirection(target.toVector().subtract(wolfLoc.toVector()));
        wolf.teleport(face);
    }

    private void finish() {
        // 🟢 DÜZELTİLDİ: Son duruş noktası optimize edildi.
        // Y ekseni 1.1'den 0.8'e çekildi, böylece bloğun tam üzerine oturur.
        Location finalTarget = jumpBlocks.get(targetIndex).clone().add(0.5, 0.8, 0.5);

        // Son konuma direkt ışınla (kaymayı önlemek için)
        wolf.teleport(finalTarget);

        player.sendMessage("§a✓ Hedefe başarıyla ulaştınız!");

        // Oyuncuyu indirip kurdu kaldır
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            session.dismountWolf(player);
            plugin.getServer().getScheduler().runTaskLater(plugin, session::removeWolf, 8L);
        }, 5L);
    }

    private void cleanup() {
        if (session.hasWolf()) {
            session.dismountWolf(player);
            session.removeWolf();
        }
    }

    private int findNearestIndex(Location loc) {
        // Ayağın altındaki bloğu kontrol et
        int bx = loc.getBlockX();
        int by = loc.getBlockY() - 1;
        int bz = loc.getBlockZ();

        for (int i = 0; i < jumpBlocks.size(); i++) {
            Location jb = jumpBlocks.get(i);
            if (jb.getBlockX() == bx && jb.getBlockY() == by && jb.getBlockZ() == bz) {
                return i;
            }
        }

        // Bulamazsa Y öncelikli en yakın
        int nearest = 0;
        double minDist = Double.MAX_VALUE;

        for (int i = 0; i < jumpBlocks.size(); i++) {
            Location jb = jumpBlocks.get(i);
            double dy = Math.abs(loc.getY() - (jb.getY() + 1));
            double dx = loc.getX() - (jb.getX() + 0.5);
            double dz = loc.getZ() - (jb.getZ() + 0.5);
            double d = Math.sqrt(dx * dx + dz * dz) + (dy * 10);

            if (d < minDist) {
                minDist = d;
                nearest = i;
            }
        }
        return nearest;
    }
}