package me.arrowdev.arrowsParkour.task;

import me.arrowdev.arrowsParkour.ArrowsParkour;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import org.bukkit.Bukkit;
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
    private static final int MAX_TICKS = 1200;
    private static final double ARRIVAL_DISTANCE = 0.3;

    // ★ Yön: true = ileri (up), false = geri (down)
    private final boolean goingForward;

    public WolfMovementTask(ArrowsParkour plugin, Player player, Wolf wolf,
                            ParkourSession session, Location targetLoc, int targetBlockIndex) {
        this.plugin = plugin;
        this.player = player;
        this.wolf = wolf;
        this.session = session;
        this.jumpBlocks = session.getJumpBlocks();
        this.targetIndex = targetBlockIndex;
        this.currentIndex = findNearestIndex(wolf.getLocation());
        this.goingForward = (targetIndex > currentIndex);
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

        // ★ Yöne göre bitiş koşulu
        boolean finished = goingForward
                ? (currentIndex >= targetIndex)
                : (currentIndex <= targetIndex);

        if (finished) {
            finish();
            cancel();
            return;
        }

        Location currentTarget = jumpBlocks.get(currentIndex).clone().add(0.5, 1.2, 0.5);
        double distance = wolf.getLocation().distance(currentTarget);

        if (distance < ARRIVAL_DISTANCE) {
            // ★ Yöne göre index güncelle
            if (goingForward) {
                currentIndex++;
            } else {
                currentIndex--;
            }
            return;
        }

        moveToward(currentTarget);

        if (!wolf.getPassengers().contains(player)) {
            wolf.addPassenger(player);
        }

        if (ticks % 20 == 0) {
            plugin.getLogger().info("🐺 Wolf | Index: " + currentIndex + "/" + targetIndex +
                    " | Yön: " + (goingForward ? "İLERİ" : "GERİ") +
                    " | Mesafe: " + String.format("%.2f", distance));
        }
    }

    private void moveToward(Location target) {
        Location wolfLoc = wolf.getLocation();

        double dx = target.getX() - wolfLoc.getX();
        double dy = target.getY() - wolfLoc.getY();
        double dz = target.getZ() - wolfLoc.getZ();

        double hDist = Math.sqrt(dx * dx + dz * dz);

        // ★ Hızı istediğin gibi buradan ayarlayabilirsin
        // 0.35 → 0.6 yatay hız
        // 0.5  → 0.8 hızlanma faktörü
        double speed = Math.min(0.6, hDist * 0.8);

        double vx = 0, vz = 0;
        if (hDist > 0.01) {
            vx = (dx / hDist) * speed;
            vz = (dz / hDist) * speed;
        }

        // -0.2 → -0.4 aşağı hız
        //  0.3 →  0.5 yukarı hız
        double vy = Math.max(-0.4, Math.min(0.5, dy * 0.8));

        wolf.setVelocity(new Vector(vx, vy, vz));

        Location face = wolfLoc.clone();
        face.setDirection(target.toVector().subtract(wolfLoc.toVector()));
        wolf.teleport(face);
    }

    private void finish() {
        // ★ 1. Flag → onDismount müdahale etmesin
        session.setWolfFinishing(true);

        // ★ 2. Oyuncuyu wolf'tan indir
        wolf.eject();

        // ★ 3. Landing bloğunu hesapla
        // İleri: APCommand'da +1 eklendi, burada -1 ile dengele
        // Geri:  APCommand'da +1/-1 YOK, burada da düzeltme YOK
        int landingIndex;
        if (goingForward) {
            landingIndex = Math.max(0, targetIndex - 1);
        } else {
            landingIndex = targetIndex; // ★ DÜZELTİLDİ: +1 kaldırıldı
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

        // ★ DÜZELTİLDİ: eject + teleport aynı tick'te çalışıyordu
        // wolf.eject() tamamlanmadan teleport olduğu için oyuncu kenarına düşüyordu
        // 1 tick bekleyerek eject'in tamamlanmasını garantiliyoruz
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.teleport(playerTarget);
            player.sendMessage("§a✓ Hedefe başarıyla ulaştınız!");
            session.removeWolf();
            session.setWolfFinishing(false);
        }, 1L);
    }

    private void cleanup() {
        session.setWolfFinishing(false);
        if (session.hasWolf()) {
            session.dismountWolf(player);
            session.removeWolf();
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