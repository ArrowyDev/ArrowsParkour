package me.arrowdev.arrowsParkour.model;

import org.bukkit.entity.Arrow;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class ArrowRainSession {

    private final List<Arrow> arrows = new ArrayList<>();
    private BukkitTask spawnTask;
    private BukkitTask countdownTask;
    private BukkitTask cleanupTask;
    private int remainingSeconds;

    public ArrowRainSession(int seconds) {
        this.remainingSeconds = seconds;
    }

    public List<Arrow> getArrows() { return arrows; }
    public void addArrow(Arrow arrow) { arrows.add(arrow); }

    public int getRemainingSeconds() { return remainingSeconds; }
    public void setRemainingSeconds(int s) { this.remainingSeconds = s; }

    public BukkitTask getSpawnTask() { return spawnTask; }
    public void setSpawnTask(BukkitTask t) { this.spawnTask = t; }

    public BukkitTask getCountdownTask() { return countdownTask; }
    public void setCountdownTask(BukkitTask t) { this.countdownTask = t; }

    public BukkitTask getCleanupTask() { return cleanupTask; }
    public void setCleanupTask(BukkitTask t) { this.cleanupTask = t; }

    public void cancelTasks() {
        if (spawnTask != null) try { spawnTask.cancel(); } catch (Exception ignored) {}
        if (countdownTask != null) try { countdownTask.cancel(); } catch (Exception ignored) {}
        if (cleanupTask != null) try { cleanupTask.cancel(); } catch (Exception ignored) {}
    }

    public void removeAllArrows() {
        for (Arrow arrow : arrows) {
            try {
                if (arrow != null && arrow.isValid()) arrow.remove();
            } catch (Exception ignored) {}
        }
        arrows.clear();
    }
}