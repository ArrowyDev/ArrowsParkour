package me.arrowdev.arrowsParkour.commands;

import me.arrowdev.arrowsParkour.manager.ParkourManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class APCommand implements CommandExecutor {
    private final ParkourManager manager;

    public APCommand(ParkourManager manager) { this.manager = manager; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cSadece oyuncular!");
            return true;
        }
        if (args.length == 0) {
            p.sendMessage("§6=== Arrow's Parkour ===\n§e/ap create §7- Parkur oluştur\n§e/ap clear §7- Parkuru temizle");
            return true;
        }
        if (args[0].equalsIgnoreCase("create")) {
            manager.createFullParkour(p);
        } else if (args[0].equalsIgnoreCase("clear")) {
            manager.clearParkour(p);
        } else {
            p.sendMessage("§cBilinmeyen komut! /ap create veya /ap clear");
        }
        return true;
    }
}