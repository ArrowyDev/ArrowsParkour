package me.arrowdev.arrowsParkour.listener;

import me.arrowdev.arrowsParkour.manager.ParkourManager;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

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
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;

        ParkourSession session = manager.getSession(p);
        if (session == null) return;

        int currentY = to.getBlockY();
        int startY = session.getStartY();
        int heightDifference = currentY - startY;
        java.util.UUID uuid = p.getUniqueId();

        // Mevcut yüksekliği göster
        int lastHeight = lastDisplayHeight.getOrDefault(uuid, 0);
        if (heightDifference != lastHeight) {
            lastDisplayHeight.put(uuid, heightDifference);

            // Subtitle'da yüksekliği göster
            p.sendActionBar("§eMevcut Yükseklik: §a" + heightDifference + " §eBlok");

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
}