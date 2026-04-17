package me.arrowdev.arrowsParkour.model;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ParkourSession {
    private final Player player;
    private final List<Location> allBlocks;
    private boolean completed;
    private int startY;

    public ParkourSession(Player player) {
        this.player = player;
        this.allBlocks = new ArrayList<>();
        this.completed = false;
    }

    public Player getPlayer() { return player; }
    public List<Location> getAllBlocks() { return allBlocks; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public void addBlock(Location loc) { allBlocks.add(loc); }

    public int getStartY() { return startY; }
    public void setStartY(int startY) { this.startY = startY; }
}