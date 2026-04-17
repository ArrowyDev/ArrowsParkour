package me.arrowdev.arrowsParkour;

import me.arrowdev.arrowsParkour.commands.APCommand;
import me.arrowdev.arrowsParkour.listener.ParkourListener;
import me.arrowdev.arrowsParkour.manager.ParkourManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArrowsParkour extends JavaPlugin {
    private static ArrowsParkour instance;
    private ParkourManager parkourManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        parkourManager = new ParkourManager(this);
        getCommand("ap").setExecutor(new APCommand(parkourManager));
        getServer().getPluginManager().registerEvents(new ParkourListener(parkourManager), this);
        getLogger().info("Arrow's Parkour başlatıldı!");
    }

    @Override
    public void onDisable() {
        if (parkourManager != null) {
            parkourManager.saveAll();
            parkourManager.clearAllSessions();
        }
        getLogger().info("Arrow's Parkour durduruldu.");
    }

    public static ArrowsParkour getInstance() { return instance; }
    public ParkourManager getParkourManager() { return parkourManager; }
}