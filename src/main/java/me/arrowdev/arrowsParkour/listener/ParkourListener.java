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
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParkourListener implements Listener {
    private final ParkourManager manager;
    private final Map<UUID, Integer> lastWinHeight;
    private final Map<UUID, Integer> lastDisplayHeight;

    public ParkourListener(ParkourManager manager) {
        this.manager = manager;
        this.lastWinHeight = new HashMap<>();
        this.lastDisplayHeight = new HashMap<>();
    }

    // =====================================================================
    // DÜNYA DEĞİŞİMİ
    // =====================================================================

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();

        if (manager.isInParkourWorld(p)) {
            manager.createOrUpdateBossBar(p);
        } else {
            manager.hideBossBar(p);
            manager.cancelCountdown(p);
        }
    }

    // =====================================================================
    // GİRİŞ / ÇIKIŞ
    // =====================================================================

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        manager.onPlayerJoin(p);

        Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
            if (manager.isInParkourWorld(p)) {
                manager.createOrUpdateBossBar(p);
            } else {
                manager.hideBossBar(p);
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        manager.removeBossBar(e.getPlayer());
    }

    // =====================================================================
    // HAREKET
    // =====================================================================

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

        if (session.hasWolf() && session.getWolf().getPassengers().contains(p)) {
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

        if (!manager.isInParkourWorld(p)) return;

        if (manager.isFrozen(p)) {
            if (e.getFrom().getY() < e.getTo().getY()) {
                e.setTo(e.getFrom());
            }
        }
    }

    // =====================================================================
    // BLOK KIRMA / KOYMA
    // =====================================================================

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

    // =====================================================================
    // ★ DÜZELTİLDİ: TNT PATLAMASI — SADECE PARKUR DÜNYASINDA ÇALIŞIR
    // =====================================================================

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        // ★ DÜNYA FİLTRESİ: Patlama parkur dünyasında değilse hiçbir şey yapma
        if (!manager.isParkourWorld(event.getEntity().getWorld())) return;

        if (event.getEntity() instanceof TNTPrimed) {
            event.blockList().clear();
            for (Player player : event.getEntity().getWorld().getPlayers()) {
                double distance = player.getLocation().distance(event.getEntity().getLocation());
                if (distance < 20) {
                    org.bukkit.util.Vector direction = player.getLocation().toVector()
                            .subtract(event.getEntity().getLocation().toVector())
                            .normalize();
                    player.setVelocity(direction.multiply(3));
                }
            }
            event.setYield(0f);
        }
    }

    // =====================================================================
    // ★ DÜZELTİLDİ: PATLAMA HASARI — SADECE PARKUR DÜNYASINDA İPTAL EDİLİR
    // =====================================================================

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // ★ DÜNYA FİLTRESİ: Parkur dünyasında değilse normal hasar devam etsin
        if (!manager.isInParkourWorld(player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            event.setCancelled(true);
        }
    }
}