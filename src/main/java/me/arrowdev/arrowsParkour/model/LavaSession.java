package me.arrowdev.arrowsParkour.model;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class LavaSession {

    private final List<Location> lavaBlocks = new ArrayList<>();
    private BukkitTask risingTask;
    private BukkitTask actionBarTask;
    private int currentY;
    private final int maxY;
    private final int baseX;
    private final int baseZ;

    public LavaSession(int startY, int maxY, int baseX, int baseZ) {
        this.currentY = startY;
        this.maxY = maxY;
        this.baseX = baseX;
        this.baseZ = baseZ;
    }

    public List<Location> getLavaBlocks() { return lavaBlocks; }
    public void addLavaBlock(Location loc) { lavaBlocks.add(loc); }

    public BukkitTask getRisingTask() { return risingTask; }
    public void setRisingTask(BukkitTask task) { this.risingTask = task; }

    public BukkitTask getActionBarTask() { return actionBarTask; }
    public void setActionBarTask(BukkitTask task) { this.actionBarTask = task; }

    public int getCurrentY() { return currentY; }
    public void setCurrentY(int y) { this.currentY = y; }

    public int getMaxY() { return maxY; }
    public int getBaseX() { return baseX; }
    public int getBaseZ() { return baseZ; }

    public void cancelTasks() {
        if (risingTask != null) {
            try { risingTask.cancel(); } catch (Exception ignored) {}
        }
        if (actionBarTask != null) {
            try { actionBarTask.cancel(); } catch (Exception ignored) {}
        }
    }
}