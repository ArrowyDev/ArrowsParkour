package me.arrowdev.arrowsParkour.listener;

import me.arrowdev.arrowsParkour.manager.*;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParkourListener implements Listener {
    private final ParkourManager manager;
    private final PrisonManager prisonManager;
    private final LavaManager lavaManager;
    private final Map<UUID, Integer> lastWinHeight;
    private final Map<UUID, Integer> lastDisplayHeight;
    private final IceManager iceManager;
    private final InvisibleManager invisibleManager;
    private final ArrowRainManager arrowRainManager;
    private final ChaosManager chaosManager;

    public ParkourListener(ParkourManager manager, PrisonManager prisonManager,
                           LavaManager lavaManager, IceManager iceManager,
                           InvisibleManager invisibleManager,
                           ArrowRainManager arrowRainManager,ChaosManager chaosManager) {   // ★ YENİ parametre
        this.manager = manager;
        this.prisonManager = prisonManager;
        this.lavaManager = lavaManager;
        this.lastWinHeight = new HashMap<>();
        this.lastDisplayHeight = new HashMap<>();
        this.iceManager = iceManager;
        this.invisibleManager = invisibleManager;
        this.arrowRainManager = arrowRainManager;
        this.chaosManager = chaosManager;
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.LAVA) {
            if (manager.isParkourWorld(block.getWorld())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();

        if (manager.isInParkourWorld(p)) {
            manager.createOrUpdateBossBar(p);
            applySaturation(p);
        } else {
            manager.hideBossBar(p);
            manager.cancelCountdown(p);
            removeSaturation(p);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        manager.onPlayerJoin(p);

        applySaturation(p);

        Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
            if (manager.isInParkourWorld(p)) {
                manager.createOrUpdateBossBar(p);
                applySaturation(p);
            } else {
                manager.hideBossBar(p);
            }

            manager.getPlugin().getUpdateChecker().notifyPlayer(p);
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();

        lavaManager.onPlayerQuit(p);
        prisonManager.onPlayerQuit(p);
        iceManager.onPlayerQuit(p);
        invisibleManager.onPlayerQuit(p);
        arrowRainManager.onPlayerQuit(p);
        chaosManager.onPlayerQuit(p);
        manager.removeBossBar(p);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();

        lavaManager.onPlayerDeath(p);
        iceManager.onPlayerDeath(p);
        invisibleManager.onPlayerDeath(p);
        arrowRainManager.onPlayerDeath(p);
        chaosManager.onPlayerDeath(p);

        if (prisonManager.isInPrison(p)) {
            prisonManager.freePrisonerSilently(p);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
            if (!p.isOnline()) return;
            applySaturation(p);
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();

        if (!manager.isInParkourWorld(p)) return;

        Location to = event.getTo();
        if (to == null) return;

        if (manager.isFrozen(p)) {
            Location from = event.getFrom();
            to.setX(from.getX());
            to.setZ(from.getZ());
            event.setTo(to);
            return;
        }

        ParkourSession session = manager.getSession(p);
        if (session == null) return;

        if (session.hasMount() && session.getMount().getPassengers().contains(p)) {
            event.setCancelled(true);
            return;
        }

        int currentY = to.getBlockY();
        int startY = session.getStartY();
        int heightDifference = currentY - startY;
        int currentBlockY = to.getBlockY();
        UUID uuid = p.getUniqueId();

        int lastHeight = lastDisplayHeight.getOrDefault(uuid, 0);
        if (heightDifference != lastHeight) {
            lastDisplayHeight.put(uuid, heightDifference);

            if (heightDifference > lastHeight) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
            }
        }

        if (currentBlockY <= startY
                && (session.getForwardProtection() != 0
                || session.getBackwardProtection() != 0)) {
            session.setForwardProtection(0);
            session.setBackwardProtection(0);
            p.sendMessage("§7Başlangıç bloğuna döndün, korumalar sıfırlandı.");
        }

        if (heightDifference >= 100) {
            int level = heightDifference / 100;
            Integer lastWinLvl = lastWinHeight.getOrDefault(uuid, -1);

            if (lastWinLvl < level) {
                lastWinHeight.put(uuid, level);
                manager.startCountdownIfNeeded(p, heightDifference);
            }
        } else {
            if (lastWinHeight.containsKey(uuid) && lastWinHeight.get(uuid) >= 1) {
                manager.cancelCountdown(p);
                lastWinHeight.put(uuid, 0);
            }
        }
    }

    @EventHandler
    public void onBlockFade(org.bukkit.event.block.BlockFadeEvent event) {
        Block block = event.getBlock();

        // Sadece buz veya kar erimesini engelle
        if (block.getType() != Material.ICE
                && block.getType() != Material.FROSTED_ICE
                && block.getType() != Material.SNOW_BLOCK
                && block.getType() != Material.SNOW) return;

        // Parkur dünyasında değilse geç
        if (!manager.isParkourWorld(block.getWorld())) return;

        // Bu blok herhangi bir oyuncunun parkuruna ait mi?
        FileConfiguration cfg = manager.getPlugin().getConfig();
        ConfigurationSection parkourSection = cfg.getConfigurationSection("parkours");
        if (parkourSection == null) return;

        for (String uuidStr : parkourSection.getKeys(false)) {
            int baseX = cfg.getInt("parkours." + uuidStr + ".baseX");
            int baseZ = cfg.getInt("parkours." + uuidStr + ".baseZ");
            int baseY = cfg.getInt("parkours." + uuidStr + ".baseY");
            String worldName = cfg.getString("parkours." + uuidStr + ".world");

            if (worldName == null) continue;
            if (!block.getWorld().getName().equals(worldName)) continue;

            int bx = block.getX();
            int by = block.getY();
            int bz = block.getZ();

            // Blok parkur alanı içinde mi? (17x17 alan + yükseklik)
            if (bx >= baseX && bx < baseX + 17
                    && bz >= baseZ && bz < baseZ + 17
                    && by >= baseY) {
                event.setCancelled(true);   // ★ Erimesini engelle
                return;
            }
        }
    }

    @EventHandler
    public void onDismount(org.bukkit.event.entity.EntityDismountEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        ParkourSession session = manager.getSession(p);
        if (session == null) return;

        if (session.hasMount() && e.getDismounted() != null
                && e.getDismounted().equals(session.getMount())) {

            if (session.isMountFinishing()) return;

            e.setCancelled(true);

            Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
                if (session.hasMount() && session.getMount().isValid() && !p.isDead()) {
                    try {
                        if (!session.getMount().getPassengers().contains(p)) {
                            session.getMount().addPassenger(p);
                        }
                    } catch (Exception ignored) {}
                }
            }, 4L);
        }
    }

    @EventHandler
    public void onJump(PlayerMoveEvent e) {
        Player p = e.getPlayer();

        if (!manager.isInParkourWorld(p)) return;

        if (manager.isFrozen(p)) {
            if (e.getFrom().getY() < e.getTo().getY()) {
                e.setTo(e.getFrom());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();

        if (!manager.isInParkourWorld(p)) return;

        Block block = event.getBlock();
        ParkourSession session = manager.getSession(p);
        if (session == null) return;

        if (!session.isAreaEditEnabled()) {
            event.setCancelled(true);
            p.sendMessage("§c/ap area true komutunu kullanarak terrain düzenlemesini aç!");
            return;
        }

        Location loc = block.getLocation();
        session.getAllBlocks().remove(loc);
        String key = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        session.getBlockMaterials().remove(key);

        manager.getPlugin().getLogger().info("❌ Blok kırıldı: " + key);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();

        if (!manager.isInParkourWorld(p)) return;

        Block block = event.getBlock();
        ParkourSession session = manager.getSession(p);
        if (session == null) return;

        if (!session.isAreaEditEnabled()) {
            event.setCancelled(true);
            p.sendMessage("§c/ap area true komutunu kullanarak terrain düzenlemesini aç!");
            return;
        }

        Location loc = block.getLocation();
        Material material = block.getType();
        session.addBlock(loc, material);

        manager.getPlugin().getLogger().info("✅ Blok yerleştirildi: "
                + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                + " -> " + material.name());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!manager.isParkourWorld(event.getEntity().getWorld())) return;

        if (event.getEntity() instanceof TNTPrimed) {
            event.blockList().clear();

            Location explosionLoc = event.getEntity().getLocation();
            explosionLoc.getWorld().playSound(explosionLoc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);

            for (Player player : event.getEntity().getWorld().getPlayers()) {
                double distance = player.getLocation().distance(explosionLoc);
                if (distance < 30) {
                    org.bukkit.util.Vector direction = player.getLocation().toVector()
                            .subtract(explosionLoc.toVector())
                            .normalize();

                    double power = 5.0 * (1.0 - (distance / 30.0));
                    power = Math.max(power, 1.0);

                    direction.setY(direction.getY() + 0.6);
                    player.setVelocity(direction.multiply(power));
                }
            }
            event.setYield(0f);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (!manager.isInParkourWorld(player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            event.setCancelled(true);
        }
    }

    private void applySaturation(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0f);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SATURATION,
                Integer.MAX_VALUE,
                1,
                false,
                false,
                false
        ));
    }

    private void removeSaturation(Player player) {
        player.removePotionEffect(PotionEffectType.SATURATION);
    }
}