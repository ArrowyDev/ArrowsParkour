package me.arrowdev.arrowsParkour;

import me.arrowdev.arrowsParkour.commands.APCommand;
import me.arrowdev.arrowsParkour.listener.ParkourListener;
import me.arrowdev.arrowsParkour.manager.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArrowsParkour extends JavaPlugin {
    private static ArrowsParkour instance;
    private ParkourManager parkourManager;
    private PrisonManager prisonManager;
    private LavaManager lavaManager;
    private IceManager iceManager;
    private InvisibleManager invisibleManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        parkourManager = new ParkourManager(this);
        prisonManager = new PrisonManager(this);
        lavaManager = new LavaManager(this);
        iceManager = new IceManager(this);
        invisibleManager = new InvisibleManager(this);

        getCommand("ap").setExecutor(
                new APCommand(parkourManager, prisonManager, lavaManager, iceManager, invisibleManager));
        getServer().getPluginManager().registerEvents(
                new ParkourListener(parkourManager, prisonManager, lavaManager, iceManager, invisibleManager), this);

        parkourManager.initialize();
        parkourManager.startActionBarTask();
        getLogger().info("✅ Arrow's Parkour başlatıldı!");
    }

    @Override
    public void onDisable() {
        if (lavaManager != null) lavaManager.clearAll();
        if (prisonManager != null) prisonManager.clearAll();
        if (iceManager != null) iceManager.clearAll();
        if (parkourManager != null) {
            parkourManager.saveAll();
            parkourManager.clearAllSessions();
        }
        if (invisibleManager != null) invisibleManager.clearAll();
        getLogger().info("Arrow's Parkour durduruldu.");
    }

    public static ArrowsParkour getInstance() { return instance; }
    public ParkourManager getParkourManager() { return parkourManager; }
    public PrisonManager getPrisonManager() { return prisonManager; }
    public LavaManager getLavaManager() { return lavaManager; }
    public IceManager getIceManager() { return iceManager; }
    public InvisibleManager getInvisibleManager() { return invisibleManager; }
}