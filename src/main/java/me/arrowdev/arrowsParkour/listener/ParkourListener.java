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

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;

        if (manager.isFrozen(p)) {
            Location from = event.getFrom();

            if (to == null) return;

            // X ve Z sabit → hareket yok
            to.setX(from.getX());
            to.setZ(from.getZ());

            // Y'ye dokunma → yer çekimi çalışır
            // Pitch/Yaw'a dokunma → mouse serbest

            event.setTo(to);
            return;
        }

        ParkourSession session = manager.getSession(p);
        if (session == null) return;

        int currentY = to.getBlockY();
        int startY = session.getStartY();
        int heightDifference = currentY - startY;
        java.util.UUID uuid = p.getUniqueId();

        // Oyuncu 0. yüksekliğe gelirse korumaları sıfırla
        if (heightDifference <= 0) {
            if (session.getForwardProtection() > 0 || session.getBackwardProtection() > 0) {
                session.setForwardProtection(0);
                session.setBackwardProtection(0);
                p.sendMessage("§c⚠Korumalanız sıfırlandı!");
                manager.getPlugin().getLogger().info("🗑️ " + p.getName() + " korumaları sıfırlandı (yükseklik: " + heightDifference + ")");
            }
        }

        // Mevcut yüksekliği ve korumasını göster
        int lastHeight = lastDisplayHeight.getOrDefault(uuid, 0);
        if (heightDifference != lastHeight) {
            lastDisplayHeight.put(uuid, heightDifference);

            // Action bar'da yükseklik ve koruma göster
            String protectionDisplay = session.getProtectionDisplay();
            //p.sendActionBar("§eMevcut Yükseklik: §a" + heightDifference + " §eBlok | " + protectionDisplay);

            // Her blok yükseldiğinde ding sesi çal
            if (heightDifference > lastHeight) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1f);
            }
        }

        // 100 bloğa ulaş
        if (heightDifference >= 100) {
            int level = heightDifference / 100;
            Integer lastWinLvl = lastWinHeight.getOrDefault(uuid, -1);

            if (lastWinLvl < level) {
                lastWinHeight.put(uuid, level);
                manager.startCountdownIfNeeded(p, heightDifference);
            }
        } else {
            // 100'ün altına düştü
            if (lastWinHeight.containsKey(uuid) && lastWinHeight.get(uuid) >= 1) {
                manager.cancelCountdown(p);
                lastWinHeight.put(uuid, 0);
            }
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

    // Blok kırma kontrolü
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        Block block = event.getBlock();

        ParkourSession session = manager.getSession(p);
        if (session == null) return;

        // Area edit kapalıysa engelle
        if (!session.isAreaEditEnabled()) {
            event.setCancelled(true);
            p.sendMessage("§c/ap area true komutunu kullanarak terrain düzenlemesini aç!");
            return;
        }

        // Blok listesinden kaldır
        Location loc = block.getLocation();
        session.getAllBlocks().remove(loc);
        String key = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        session.getBlockMaterials().remove(key);

        manager.getPlugin().getLogger().info("❌ Blok kırıldı: " + key);
    }

    // Blok koya kontrolü
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        Block block = event.getBlock();

        ParkourSession session = manager.getSession(p);
        if (session == null) return;

        // Area edit kapalıysa engelle
        if (!session.isAreaEditEnabled()) {
            event.setCancelled(true);
            p.sendMessage("§c/ap area true komutunu kullanarak terrain düzenlemesini aç!");
            return;
        }

        // Blok listesine ekle
        Location loc = block.getLocation();
        Material material = block.getType();
        session.addBlock(loc, material);

        manager.getPlugin().getLogger().info("✅ Blok yerleştirildi: " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + " -> " + material.name());
    }

    // TNT patlaması
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

    // TNT hasar kontrolü
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