package me.arrowdev.arrowsParkour;

import me.arrowdev.arrowsParkour.commands.APCommand;
import me.arrowdev.arrowsParkour.listener.ParkourListener;
import me.arrowdev.arrowsParkour.manager.ParkourManager;
import me.arrowdev.arrowsParkour.manager.PrisonManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArrowsParkour extends JavaPlugin {
    private static ArrowsParkour instance;
    private ParkourManager parkourManager;
    private PrisonManager prisonManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        parkourManager = new ParkourManager(this);
        prisonManager = new PrisonManager(this);

        getCommand("ap").setExecutor(new APCommand(parkourManager, prisonManager));
        getServer().getPluginManager().registerEvents(new ParkourListener(parkourManager, prisonManager), this);

        parkourManager.initialize();
        parkourManager.startActionBarTask();
        getLogger().info("✅ Arrow's Parkour başlatıldı!");
    }

    @Override
    public void onDisable() {
        if (prisonManager != null) {
            prisonManager.clearAll();
        }
        if (parkourManager != null) {
            parkourManager.saveAll();
            parkourManager.clearAllSessions();
        }
        getLogger().info("Arrow's Parkour durduruldu.");
    }

    public static ArrowsParkour getInstance() { return instance; }
    public ParkourManager getParkourManager() { return parkourManager; }
    public PrisonManager getPrisonManager() { return prisonManager; }
}