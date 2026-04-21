package me.arrowdev.arrowsParkour.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParkourSession {
    private final Player player;
    private final List<Location> allBlocks;
    private final Map<String, Material> blockMaterials;
    private boolean completed;
    private int startY;
    private boolean areaEditEnabled;

    public ParkourSession(Player player) {
        this.player = player;
        this.allBlocks = new ArrayList<>();
        this.blockMaterials = new HashMap<>();
        this.completed = false;
        this.areaEditEnabled = false;
    }

    public Player getPlayer() { return player; }
    public List<Location> getAllBlocks() { return allBlocks; }
    public Map<String, Material> getBlockMaterials() { return blockMaterials; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public void addBlock(Location loc) { allBlocks.add(loc); }
    public void addBlock(Location loc, Material material) {
        allBlocks.add(loc);
        String key = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        blockMaterials.put(key, material);
    }

    public int getStartY() { return startY; }
    public void setStartY(int startY) { this.startY = startY; }

    public boolean isAreaEditEnabled() { return areaEditEnabled; }
    public void setAreaEditEnabled(boolean enabled) { this.areaEditEnabled = enabled; }
}