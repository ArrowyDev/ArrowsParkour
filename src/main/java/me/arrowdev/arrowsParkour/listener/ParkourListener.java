package me.arrowdev.arrowsParkour.listener;

import me.arrowdev.arrowsParkour.manager.*;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
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
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

    public ParkourListener(ParkourManager manager, PrisonManager prisonManager,
                           LavaManager lavaManager,IceManager iceManager,InvisibleManager invisibleManager) {
        this.manager = manager;
        this.prisonManager = prisonManager;
        this.lavaManager = lavaManager;
        this.lastWinHeight = new HashMap<>();
        this.lastDisplayHeight = new HashMap<>();
        this.iceManager = iceManager;
        this.invisibleManager = invisibleManager;
    }

    // ★ Lav akmasını engelle — parkur dünyasında lav akmaz
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
            // ★ Parkur dünyasına girince sonsuz doygunluk ver
            applySaturation(p);
        } else {
            manager.hideBossBar(p);
            manager.cancelCountdown(p);
            // ★ Parkur dünyasından çıkınca doygunluğu kaldır
            removeSaturation(p);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        manager.onPlayerJoin(p);

        Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
            if (manager.isInParkourWorld(p)) {
                manager.createOrUpdateBossBar(p);
                // ★ Girişte parkur dünyasındaysa doygunluk ver
                applySaturation(p);
            } else {
                manager.hideBossBar(p);
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();

        lavaManager.onPlayerQuit(p);
        prisonManager.onPlayerQuit(p);
        iceManager.onPlayerQuit(p);
        invisibleManager.onPlayerQuit(p);
        manager.removeBossBar(p);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();

        lavaManager.onPlayerDeath(p);
        iceManager.onPlayerDeath(p);
        invisibleManager.onPlayerDeath(p);

        if (prisonManager.isInPrison(p)) {
            prisonManager.freePrisonerSilently(p);
        }
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

        int newBlockIndex = session.findNearestBlockIndex();
        session.setCurrentBlockIndex(newBlockIndex);

        int currentY = to.getBlockY();
        int startY = session.getStartY();
        int heightDifference = currentY - startY;
        UUID uuid = p.getUniqueId();

        int lastHeight = lastDisplayHeight.getOrDefault(uuid, 0);
        if (heightDifference != lastHeight) {
            lastDisplayHeight.put(uuid, heightDifference);

            if (heightDifference > lastHeight) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
            }
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
    public void onDismount(org.bukkit.event.entity.EntityDismountEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        ParkourSession session = manager.getSession(p);
        if (session == null) return;

        if (session.hasMount() && e.getDismounted() != null
                && e.getDismounted().equals(session.getMount())) {

            if (session.isMountFinishing()) {
                return;
            }

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

        manager.getPlugin().getLogger().info("✅ Blok yerleştirildi: " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + " -> " + material.name());
    }

    // ★ TNT gücü azıcık azaltıldı: 6.0 → 5.0, Y boost 0.8 → 0.6
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

                    // ★ Güç: 6.0 → 5.0 (azıcık azaltıldı)
                    double power = 5.0 * (1.0 - (distance / 30.0));
                    power = Math.max(power, 1.0);

                    // ★ Yukarı boost: 0.8 → 0.6 (azıcık azaltıldı)
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

    // ★ Doygunluk efekti yardımcı methodları
    private void applySaturation(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SATURATION,
                Integer.MAX_VALUE,  // sonsuz süre
                0,                   // seviye 0 (yeterli)
                false,              // partiküller gizli
                false,              // ikon gizli
                false               // ambient yok
        ));
    }

    private void removeSaturation(Player player) {
        player.removePotionEffect(PotionEffectType.SATURATION);
    }
}