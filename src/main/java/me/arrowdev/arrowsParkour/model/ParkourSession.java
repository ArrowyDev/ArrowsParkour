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
    private int forwardProtection; // İleri koruma
    private int backwardProtection; // Geri koruma

    public ParkourSession(Player player) {
        this.player = player;
        this.allBlocks = new ArrayList<>();
        this.blockMaterials = new HashMap<>();
        this.completed = false;
        this.areaEditEnabled = false;
        this.forwardProtection = 0;
        this.backwardProtection = 0;
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

    // İleri Koruma (IKE) - Geri korumasından düşer
    public int getForwardProtection() { return forwardProtection; }
    public void setForwardProtection(int amount) { this.forwardProtection = Math.max(0, amount); }
    public void addForwardProtection(int amount) {
        this.forwardProtection += Math.max(0, amount);
    }

    // Geri Koruma (GKE) - İleri korumasından düşer
    public int getBackwardProtection() { return backwardProtection; }
    public void setBackwardProtection(int amount) { this.backwardProtection = Math.max(0, amount); }
    public void addBackwardProtection(int amount) {
        this.backwardProtection += Math.max(0, amount);
    }

    // Toplam koruma göster (UI için)
    public String getProtectionDisplay() {
        int net = forwardProtection - backwardProtection;

        String netColor;
        String netText;

        if (net > 0) {
            netColor = "§a"; // yeşil
            netText = "+" + net;
        } else if (net < 0) {
            netColor = "§c"; // kırmızı
            netText = String.valueOf(net);
        } else {
            netColor = "§7"; // gri
            netText = "0";
        }

        return "§a➤ İleri: " + forwardProtection +
                " §c◄ Geri: " + backwardProtection +
                " §7| Net: " + netColor + netText;
    }
}