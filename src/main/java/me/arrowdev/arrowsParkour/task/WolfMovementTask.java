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

    public WolfMovementTask(ArrowsParkour plugin, Player player, Wolf wolf,
                            ParkourSession session, Location targetLoc, int targetBlockIndex) {
        this.plugin = plugin;
        this.player = player;
        this.wolf = wolf;
        this.session = session;
        this.jumpBlocks = session.getJumpBlocks();
        this.targetIndex = targetBlockIndex;

        // Başlangıç: oyuncuya en yakın jumpBlock index'i
        this.currentIndex = findNearestIndex(player.getLocation());
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
        if (currentIndex == targetIndex) {
            finish();
            cancel();
            return;
        }

        // Mevcut hedef blok
        Location currentTarget = jumpBlocks.get(currentIndex).clone().add(0.5, 1.2, 0.5);
        double distance = wolf.getLocation().distance(currentTarget);

        // Bu bloğa ulaştıysak sıradakine geç — eşiği küçült
        if (distance < 0.4) {
            if (targetIndex > currentIndex) currentIndex++;
            else currentIndex--;
            return;
        }

        // Kurdu hareket ettir
        moveToward(currentTarget);

        // Oyuncu kurtta değilse bindirmeyi tekrar dene
        if (!wolf.getPassengers().contains(player)) {
            wolf.addPassenger(player);
        }

        if (ticks % 20 == 0) {
            plugin.getLogger().info("🐺 index=" + currentIndex + "/" + targetIndex +
                    " mesafe=" + String.format("%.2f", distance));
        }
    }

    private void moveToward(Location target) {
        Location wolfLoc = wolf.getLocation();

        double dx = target.getX() - wolfLoc.getX();
        double dy = target.getY() - wolfLoc.getY();
        double dz = target.getZ() - wolfLoc.getZ();

        double hDist = Math.sqrt(dx * dx + dz * dz);

        // Hedefe ne kadar yakınsa o kadar yavaşla
        double speed = Math.min(0.3, hDist * 0.4);

        double vx = 0, vz = 0;
        if (hDist > 0.01) {
            vx = (dx / hDist) * speed;
            vz = (dz / hDist) * speed;
        }

        // gravity(false) olduğu için sadece dy'ye göre git
        double vy = Math.max(-0.15, Math.min(0.15, dy * 0.4));

        wolf.setVelocity(new Vector(vx, vy, vz));

        // Kurdu hedefe baktır
        Location face = wolfLoc.clone();
        face.setDirection(target.toVector().subtract(wolfLoc.toVector()));
        wolf.teleport(face);
    }

    private void finish() {
        Location finalTarget = jumpBlocks.get(targetIndex).clone().add(0.5, 1.2, 0.5);
        player.sendMessage("§a✓ Hedefe ulaştın! (index: " + targetIndex + ")");
        plugin.getLogger().info("✅ WolfMovement tamamlandı → index " + targetIndex);

        // Oyuncuyu indirip kurdu kaldır
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            session.dismountWolf(player);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                session.removeWolf();
            }, 10L);
        }, 3L);
    }

    private void cleanup() {
        if (session.hasWolf()) {
            session.dismountWolf(player);
            session.removeWolf();
        }
    }

    private int findNearestIndex(Location loc) {
        int nearest = 0;
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < jumpBlocks.size(); i++) {
            double d = loc.distance(jumpBlocks.get(i));
            if (d < minDist) {
                minDist = d;
                nearest = i;
            }
        }
        return nearest;
    }
}