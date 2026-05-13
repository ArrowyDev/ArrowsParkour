package me.arrowdev.arrowsParkour.task;

import me.arrowdev.arrowsParkour.ArrowsParkour;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class AnimalMovementTask extends BukkitRunnable {

    private final ArrowsParkour plugin;
    private final Player player;
    private final LivingEntity mount;
    private final ParkourSession session;
    private final List<Location> jumpBlocks;
    private final int targetIndex;

    private int currentIndex;
    private int ticks = 0;
    private static final int MAX_TICKS = 1200;
    private static final double ARRIVAL_DISTANCE = 0.3;

    private final boolean goingForward;

    public AnimalMovementTask(ArrowsParkour plugin, Player player, LivingEntity mount,
                              ParkourSession session, Location targetLoc, int targetBlockIndex) {
        this.plugin = plugin;
        this.player = player;
        this.mount = mount;
        this.session = session;
        this.jumpBlocks = session.getJumpBlocks();
        this.targetIndex = targetBlockIndex;
        this.currentIndex = findNearestIndex(mount.getLocation());
        this.goingForward = (targetIndex > currentIndex);
    }

    @Override
    public void run() {
        ticks++;

        if (!player.isOnline() || mount == null || !mount.isValid()) {
            cleanup();
            cancel();
            return;
        }

        if (ticks > MAX_TICKS) {
            player.sendMessage("§cHayvan hareketi zaman aşımına uğradı!");
            cleanup();
            cancel();
            return;
        }

        boolean finished = goingForward
                ? (currentIndex >= targetIndex)
                : (currentIndex <= targetIndex);

        if (finished) {
            finish();
            cancel();
            return;
        }

        Location currentTarget = jumpBlocks.get(currentIndex).clone().add(0.5, 1.2, 0.5);
        double distance = mount.getLocation().distance(currentTarget);

        if (distance < ARRIVAL_DISTANCE) {
            if (goingForward) {
                currentIndex++;
            } else {
                currentIndex--;
            }
            return;
        }

        moveToward(currentTarget);

        if (!mount.getPassengers().contains(player)) {
            mount.addPassenger(player);
        }

        if (ticks % 20 == 0) {
            plugin.getLogger().info("🐾 Mount | Index: " + currentIndex + "/" + targetIndex +
                    " | Yön: " + (goingForward ? "İLERİ" : "GERİ") +
                    " | Mesafe: " + String.format("%.2f", distance));
        }
    }

    private void moveToward(Location target) {
        Location mountLoc = mount.getLocation();

        double dx = target.getX() - mountLoc.getX();
        double dy = target.getY() - mountLoc.getY();
        double dz = target.getZ() - mountLoc.getZ();

        double hDist = Math.sqrt(dx * dx + dz * dz);

        double speed = Math.min(0.35, hDist * 0.5);

        double vx = 0, vz = 0;
        if (hDist > 0.01) {
            vx = (dx / hDist) * speed;
            vz = (dz / hDist) * speed;
        }

        double vy = Math.max(-0.2, Math.min(0.3, dy * 0.5));

        mount.setVelocity(new Vector(vx, vy, vz));

        Location face = mountLoc.clone();
        face.setDirection(target.toVector().subtract(mountLoc.toVector()));
        mount.teleport(face);
    }

    private void finish() {
        session.setMountFinishing(true);

        mount.eject();

        int landingIndex;
        if (goingForward) {
            landingIndex = Math.max(0, targetIndex - 1);
        } else {
            landingIndex = targetIndex;
        }

        Location jumpBlock = jumpBlocks.get(landingIndex);

        Location playerTarget = new Location(
                player.getWorld(),
                jumpBlock.getBlockX() + 0.5,
                jumpBlock.getBlockY() + 1.0,
                jumpBlock.getBlockZ() + 0.5,
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
        );

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.teleport(playerTarget);
            player.sendMessage("§a✓ Hedefe başarıyla ulaştınız!");
            session.removeMount();
            session.setMountFinishing(false);
        }, 1L);
    }

    private void cleanup() {
        session.setMountFinishing(false);
        if (session.hasMount()) {
            session.dismountAnimal(player);
            session.removeMount();
        }
    }

    private int findNearestIndex(Location loc) {
        int bx = loc.getBlockX();
        int by = loc.getBlockY() - 1;
        int bz = loc.getBlockZ();

        for (int i = 0; i < jumpBlocks.size(); i++) {
            Location jb = jumpBlocks.get(i);
            if (jb.getBlockX() == bx && jb.getBlockY() == by && jb.getBlockZ() == bz) {
                return i;
            }
        }

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