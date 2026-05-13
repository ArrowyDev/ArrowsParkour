package me.arrowdev.arrowsParkour.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
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
    private int forwardProtection;
    private int backwardProtection;
    private int currentBlockIndex;

    // ★ Wolf yerine generic LivingEntity mount
    // Wolf, Chicken, Cat hepsi buraya atanır
    private LivingEntity mount;
    private boolean mountFinishing = false;

    public ParkourSession(Player player) {
        this.player = player;
        this.allBlocks = new ArrayList<>();
        this.blockMaterials = new HashMap<>();
        this.completed = false;
        this.areaEditEnabled = false;
        this.forwardProtection = 0;
        this.backwardProtection = 0;
        this.mount = null;
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

    // =====================================================================
    // ★ GENERIC MOUNT METHODları (Wolf, Chicken, Cat hepsi için)
    // =====================================================================

    public LivingEntity getMount() { return mount; }
    public void setMount(LivingEntity mount) { this.mount = mount; }

    public boolean hasMount() {
        return mount != null && mount.isValid();
    }

    public void removeMount() {
        if (mount != null && mount.isValid()) {
            mount.remove();
        }
        mount = null;
    }

    public void dismountAnimal(Player player) {
        if (hasMount() && mount.getPassengers().contains(player)) {
            mount.removePassenger(player);
        }
    }

    public boolean isMountFinishing() { return mountFinishing; }
    public void setMountFinishing(boolean mountFinishing) { this.mountFinishing = mountFinishing; }

    // =====================================================================
    // ★ GERIYE DÖNÜK UYUMLULUK — Wolf methodları mount'a yönlendiriyor
    // APCommand içindeki eski wolf kodu çalışmaya devam eder
    // =====================================================================

    public LivingEntity getWolf() { return mount; }
    public void setWolf(LivingEntity wolf) { this.mount = wolf; }
    public boolean hasWolf() { return hasMount(); }
    public void removeWolf() { removeMount(); }
    public void dismountWolf(Player player) { dismountAnimal(player); }
    public boolean isWolfFinishing() { return mountFinishing; }
    public void setWolfFinishing(boolean finishing) { this.mountFinishing = finishing; }

    // =====================================================================

    public int getCurrentBlockIndex() { return currentBlockIndex; }
    public void setCurrentBlockIndex(int index) {
        this.currentBlockIndex = Math.max(0, Math.min(index, allBlocks.size() - 1));
    }

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

    public List<Location> getJumpBlocks() { return jumpBlocks; }

    public void addJumpBlock(Location loc) { jumpBlocks.add(loc); }

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