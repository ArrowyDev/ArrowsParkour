package me.arrowdev.arrowsParkour.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

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
    private int forwardProtection;
    private int backwardProtection;
    private Wolf wolf;
    private int currentBlockIndex;

    public ParkourSession(Player player) {
        this.player = player;
        this.allBlocks = new ArrayList<>();
        this.blockMaterials = new HashMap<>();
        this.completed = false;
        this.areaEditEnabled = false;
        this.forwardProtection = 0;
        this.backwardProtection = 0;
        this.wolf = null;
        this.currentBlockIndex = 0;
    }

    public Player getPlayer() { return player; }
    public List<Location> getAllBlocks() { return allBlocks; }
    public Map<String, Material> getBlockMaterials() { return blockMaterials; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public void addBlock(Location loc) {
        allBlocks.add(loc);
    }

    public void addBlock(Location loc, Material material) {
        allBlocks.add(loc);
        String key = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        blockMaterials.put(key, material);
    }

    public int getStartY() { return startY; }
    public void setStartY(int startY) { this.startY = startY; }

    public boolean isAreaEditEnabled() { return areaEditEnabled; }
    public void setAreaEditEnabled(boolean enabled) { this.areaEditEnabled = enabled; }

    public int getForwardProtection() { return forwardProtection; }
    public void setForwardProtection(int amount) { this.forwardProtection = Math.max(0, amount); }
    public void addForwardProtection(int amount) {
        this.forwardProtection += Math.max(0, amount);
    }

    public int getBackwardProtection() { return backwardProtection; }
    public void setBackwardProtection(int amount) { this.backwardProtection = Math.max(0, amount); }
    public void addBackwardProtection(int amount) {
        this.backwardProtection += Math.max(0, amount);
    }

    public Wolf getWolf() { return wolf; }
    public void setWolf(Wolf wolf) { this.wolf = wolf; }
    public boolean hasWolf() { return wolf != null && wolf.isValid(); }
    public void removeWolf() {
        if (wolf != null && wolf.isValid()) {
            wolf.remove();
        }
        wolf = null;
    }

    public void dismountWolf(Player player) {
        if (hasWolf() && wolf.getPassengers().contains(player)) {
            wolf.removePassenger(player);
        }
    }

    public int getCurrentBlockIndex() { return currentBlockIndex; }
    public void setCurrentBlockIndex(int index) { this.currentBlockIndex = Math.max(0, Math.min(index, allBlocks.size() - 1)); }

    public int findNearestBlockIndex() {
        Location playerLoc = player.getLocation();
        int nearestIndex = 0;
        double nearestDistance = Double.MAX_VALUE;

        for (int i = 0; i < allBlocks.size(); i++) {
            Location loc = allBlocks.get(i);
            Material mat = blockMaterials.getOrDefault(
                    loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ(),
                    Material.STONE
            );

            if (mat == Material.BARRIER) continue;

            double distance = playerLoc.distance(loc);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
            }
        }

        return nearestIndex;
    }

    private final List<Location> jumpBlocks = new ArrayList<>();

    public List<Location> getJumpBlocks() {
        return jumpBlocks;
    }

    public void addJumpBlock(Location loc) {
        jumpBlocks.add(loc);
    }

    public String getProtectionDisplay() {
        int net = forwardProtection - backwardProtection;

        String netColor;
        String netText;

        if (net > 0) {
            netColor = "§a";
            netText = "+" + net;
        } else if (net < 0) {
            netColor = "§c";
            netText = String.valueOf(net);
        } else {
            netColor = "§7";
            netText = "0";
        }

        return "§a➤ İleri: " + forwardProtection +
                " §c◄ Geri: " + backwardProtection +
                " §7| Net: " + netColor + netText;
    }
}