package me.arrowdev.arrowsParkour.model;

import org.bukkit.scheduler.BukkitTask;

public class GravitySession {

    private int remainingSeconds;
    private final double originalValue;
    private final String mode;
    private BukkitTask actionBarTask;
    private BukkitTask countdownTask;

    public GravitySession(int remainingSeconds, double originalValue, String mode) {
        this.remainingSeconds = remainingSeconds;
        this.originalValue = originalValue;
        this.mode = mode;
    }

    public int getRemainingSeconds() { return remainingSeconds; }
    public void setRemainingSeconds(int remainingSeconds) { this.remainingSeconds = remainingSeconds; }

    public double getOriginalValue() { return originalValue; }
    public String getMode() { return mode; }

    public void setActionBarTask(BukkitTask task) { this.actionBarTask = task; }
    public void setCountdownTask(BukkitTask task) { this.countdownTask = task; }

    public void cancelTasks() {
        if (actionBarTask != null) { try { actionBarTask.cancel(); } catch (Exception ignored) {} }
        if (countdownTask != null) { try { countdownTask.cancel(); } catch (Exception ignored) {} }
    }
}