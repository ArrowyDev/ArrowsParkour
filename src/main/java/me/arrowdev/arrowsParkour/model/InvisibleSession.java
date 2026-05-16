package me.arrowdev.arrowsParkour.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class InvisibleSession {

    private final Map<Location, Material> originalBlocks = new HashMap<>();
    private BukkitTask countdownTask;
    private BukkitTask actionBarTask;
    private int remainingSeconds;

    public InvisibleSession(int seconds) {
        this.remainingSeconds = seconds;
    }

    public Map<Location, Material> getOriginalBlocks() { return originalBlocks; }

    public void addOriginalBlock(Location loc, Material material) {
        originalBlocks.put(loc, material);
    }

    public int getRemainingSeconds() { return remainingSeconds; }
    public void setRemainingSeconds(int seconds) { this.remainingSeconds = seconds; }

    public BukkitTask getCountdownTask() { return countdownTask; }
    public void setCountdownTask(BukkitTask task) { this.countdownTask = task; }

    public BukkitTask getActionBarTask() { return actionBarTask; }
    public void setActionBarTask(BukkitTask task) { this.actionBarTask = task; }

    public void cancelTasks() {
        if (countdownTask != null) try { countdownTask.cancel(); } catch (Exception ignored) {}
        if (actionBarTask != null) try { actionBarTask.cancel(); } catch (Exception ignored) {}
    }
}