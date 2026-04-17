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

    public ParkourListener(ParkourManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;

        ParkourSession session = manager.getSession(p);
        if (session == null || session.isCompleted()) return;

        int currentY = to.getBlockY();
        int startY = session.getStartY();

        if (currentY >= startY + 100) {
            manager.winParkour(p);
        }
    }
}