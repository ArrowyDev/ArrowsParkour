package me.arrowdev.arrowsParkour.listener;

import me.arrowdev.arrowsParkour.manager.ParkourManager;
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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;


public class ParkourListener implements Listener {
    private final ParkourManager manager;
    private final java.util.Map<java.util.UUID, Integer> lastWinHeight;
    private final java.util.Map<java.util.UUID, Integer> lastDisplayHeight;

    public ParkourListener(ParkourManager manager) {
        this.manager = manager;
        this.lastWinHeight = new java.util.HashMap<>();
        this.lastDisplayHeight = new java.util.HashMap<>();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        manager.onPlayerJoin(p);

        Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
            manager.createOrUpdateBossBar(p);
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        manager.removeBossBar(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;

        if (manager.isFrozen(p)) {
            Location from = event.getFrom();
            if (to == null) return;
            to.setX(from.getX());
            to.setZ(from.getZ());
            event.setTo(to);
            return;
        }

        ParkourSession session = manager.getSession(p);
        if (session == null) return;

        // Kurta bindiyse inmesini engelle
        if (session.hasWolf() && session.getWolf().getPassengers().contains(p)) {
            if (p.isSneaking()) {
                p.sendMessage("§cHedef bloğa ulaşana kadar kurttan inemezsin!");
                event.setCancelled(true);
                return;
            }
        }

        int newBlockIndex = session.findNearestBlockIndex();
        session.setCurrentBlockIndex(newBlockIndex);

        int currentY = to.getBlockY();
        int startY = session.getStartY();
        int heightDifference = currentY - startY;
        java.util.UUID uuid = p.getUniqueId();

        int lastHeight = lastDisplayHeight.getOrDefault(uuid, 0);
        if (heightDifference != lastHeight) {
            lastDisplayHeight.put(uuid, heightDifference);

            if (heightDifference > lastHeight) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1f);
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

        if (session.hasWolf() && e.getDismounted() != null && e.getDismounted().equals(session.getWolf())) {
            e.setCancelled(true);

            Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
                if (session.hasWolf() && session.getWolf().isValid() && !p.isDead()) {
                    try {
                        if (!session.getWolf().getPassengers().contains(p)) {
                            session.getWolf().addPassenger(p);
                        }
                    } catch (Exception ignored) {}
                }
            }, 4L);
        }
    }

    @EventHandler
    public void onJump(PlayerMoveEvent e) {
        Player p = e.getPlayer();

        if (manager.isFrozen(p)) {
            if (e.getFrom().getY() < e.getTo().getY()) {
                e.setTo(e.getFrom());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
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

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof TNTPrimed) {
            event.blockList().clear();
            for (Player player : event.getEntity().getWorld().getPlayers()) {
                double distance = player.getLocation().distance(event.getEntity().getLocation());
                if (distance < 20) {
                    org.bukkit.util.Vector direction = player.getLocation().toVector().subtract(event.getEntity().getLocation().toVector()).normalize();
                    player.setVelocity(direction.multiply(3));
                }
            }
            event.setYield(0f);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                    event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                event.setCancelled(true);
            }
        }
    }
}