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
    private ArrowRainManager arrowRainManager;
    private ChaosManager chaosManager;
    private UpdateChecker updateChecker;
    private GravityManager gravityManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        parkourManager    = new ParkourManager(this);
        prisonManager     = new PrisonManager(this);
        lavaManager       = new LavaManager(this);
        iceManager        = new IceManager(this);
        invisibleManager  = new InvisibleManager(this);
        arrowRainManager  = new ArrowRainManager(this);
        gravityManager    = new GravityManager(this);

        // ★ ChaosManager en son oluşturuluyor — diğer manager'lara bağımlı
        chaosManager = new ChaosManager(
                this,
                lavaManager,
                iceManager,
                invisibleManager,
                arrowRainManager,
                parkourManager
        );

        getCommand("ap").setExecutor(
                new APCommand(
                        parkourManager, prisonManager, lavaManager,
                        iceManager, invisibleManager, arrowRainManager,
                        chaosManager, gravityManager
                ));

        getServer().getPluginManager().registerEvents(
                new ParkourListener(
                        parkourManager, prisonManager, lavaManager,
                        iceManager, invisibleManager, arrowRainManager,
                        chaosManager, gravityManager
                ), this);

        parkourManager.initialize();
        parkourManager.startActionBarTask();
        getLogger().info("✅ Arrow's Parkour başlatıldı!");

        updateChecker = new UpdateChecker(
                this,
                "ArrowyDev",
                "ArrowsParkour"
        );
        updateChecker.checkAsync();
    }

    @Override
    public void onDisable() {
        if (chaosManager != null)    chaosManager.clearAll();      // ★ YENİ — önce kaos
        if (arrowRainManager != null) arrowRainManager.clearAll();
        if (lavaManager != null)     lavaManager.clearAll();
        if (prisonManager != null)   prisonManager.clearAll();
        if (iceManager != null)      iceManager.clearAll();
        if (invisibleManager != null) invisibleManager.clearAll();
        if (parkourManager != null) {
            parkourManager.saveAll();
            parkourManager.clearAllSessions();
        }
        getLogger().info("Arrow's Parkour durduruldu.");
    }

    public static ArrowsParkour getInstance()          { return instance; }
    public ParkourManager getParkourManager()          { return parkourManager; }
    public PrisonManager getPrisonManager()            { return prisonManager; }
    public LavaManager getLavaManager()                { return lavaManager; }
    public IceManager getIceManager()                  { return iceManager; }
    public InvisibleManager getInvisibleManager()      { return invisibleManager; }
    public ArrowRainManager getArrowRainManager()      { return arrowRainManager; }
    public ChaosManager getChaosManager()              { return chaosManager; }
    public UpdateChecker getUpdateChecker() { return updateChecker; }
}