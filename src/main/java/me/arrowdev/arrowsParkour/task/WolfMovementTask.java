package me.arrowdev.arrowsParkour.task;

import me.arrowdev.arrowsParkour.ArrowsParkour;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

public class WolfMovementTask extends AnimalMovementTask {

    public WolfMovementTask(ArrowsParkour plugin, Player player, Wolf wolf,
                            ParkourSession session, Location targetLoc, int targetBlockIndex) {
        super(plugin, player, wolf, session, targetLoc, targetBlockIndex);
    }
}