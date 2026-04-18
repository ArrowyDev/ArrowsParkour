package me.arrowdev.arrowsParkour.listener;

import me.arrowdev.arrowsParkour.manager.ParkourManager;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class ParkourListener implements Listener {
    private final ParkourManager manager;
    private final java.util.Map<java.util.UUID, Integer> lastWinHeight;

    public ParkourListener(ParkourManager manager) {
        this.manager = manager;
        this.lastWinHeight = new java.util.HashMap<>();
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

        // +100 blokta olup, bir önceki rekordu geç
        if (heightDifference >= 100) {
            int level = heightDifference / 100;
            Integer lastHeight = lastWinHeight.getOrDefault(uuid, -1);

            // Yeni level'e ulaştıysa
            if (lastHeight < heightDifference) {
                lastWinHeight.put(uuid, heightDifference);
                manager.startCountdownIfNeeded(p, heightDifference);
            }
        } else {
            // 100'ün altına düştü, countdown'u iptal et
            if (lastWinHeight.containsKey(uuid) && lastWinHeight.get(uuid) >= 100) {
                manager.cancelCountdown(p);
                lastWinHeight.put(uuid, heightDifference);
            }
        }
    }
}