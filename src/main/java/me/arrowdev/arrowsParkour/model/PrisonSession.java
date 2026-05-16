package me.arrowdev.arrowsParkour.model;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class PrisonSession {

    private final Location savedLocation;

    private final List<Location> prisonBlocks = new ArrayList<>();

    private BukkitTask countdownTask;
    private BukkitTask actionBarTask;

    private int remainingSeconds;

    private final int totalSeconds;

    private final String subtitleText;

    public PrisonSession(Location savedLocation, int seconds, String subtitleText) {
        this.savedLocation = savedLocation;
        this.remainingSeconds = seconds;
        this.totalSeconds = seconds;
        this.subtitleText = subtitleText;
    }

    public Location getSavedLocation() { return savedLocation; }

    public List<Location> getPrisonBlocks() { return prisonBlocks; }
    public void addPrisonBlock(Location loc) { prisonBlocks.add(loc); }

    public int getRemainingSeconds() { return remainingSeconds; }
    public void setRemainingSeconds(int seconds) { this.remainingSeconds = seconds; }
    public int getTotalSeconds() { return totalSeconds; }

    public String getSubtitleText() { return subtitleText; }

    public BukkitTask getCountdownTask() { return countdownTask; }
    public void setCountdownTask(BukkitTask task) { this.countdownTask = task; }

    public BukkitTask getActionBarTask() { return actionBarTask; }
    public void setActionBarTask(BukkitTask task) { this.actionBarTask = task; }

    public void cancelTasks() {
        if (countdownTask != null) {
            try { countdownTask.cancel(); } catch (Exception ignored) {}
        }
        if (actionBarTask != null) {
            try { actionBarTask.cancel(); } catch (Exception ignored) {}
        }
    }
}