package me.arrowdev.arrowsParkour.commands;

import me.arrowdev.arrowsParkour.manager.ParkourManager;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class APCommand implements CommandExecutor {

    private final ParkourManager manager;

    public APCommand(ParkourManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cSadece oyuncular!");
            return true;
        }

        if (args.length == 0) {
            p.sendMessage("§6=== Arrow's Parkour ===\n§e/ap create §7- Parkur oluştur\n§e/ap clear §7- Parkuru temizle\n§e/ap tp §7- Ortaya ışınlan\n§e/ap reset §7- İlerlemeni sıfırla");
            return true;
        }

        // CREATE
        if (args[0].equalsIgnoreCase("create")) {
            manager.createFullParkour(p);
            return true;
        }

        // CLEAR
        if (args[0].equalsIgnoreCase("clear")) {
            manager.clearParkour(p);
            return true;
        }

        // TP (YENİ)
        if (args[0].equalsIgnoreCase("tp")) {

            FileConfiguration cfg = manager.getPlugin().getConfig();
            String path = "parkours." + p.getUniqueId();

            if (!cfg.contains(path)) {
                p.sendMessage("§cParkurun yok!");
                return true;
            }

            int baseX = cfg.getInt(path + ".baseX");
            int baseZ = cfg.getInt(path + ".baseZ");
            int baseY = cfg.getInt(path + ".baseY");

            int size = 17;

            Location tp = new Location(
                    p.getWorld(),
                    baseX + (size / 2.0),
                    baseY + 1,
                    baseZ + (size / 2.0)
            );

            p.teleport(tp);
            p.sendMessage("§aParkurun ortasına ışınlandın!");
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {

            FileConfiguration cfg = manager.getPlugin().getConfig();
            String path = "parkours." + p.getUniqueId();

            if (!cfg.contains(path)) {
                p.sendMessage("§cParkurun yok!");
                return true;
            }

            int baseX = cfg.getInt(path + ".baseX");
            int baseZ = cfg.getInt(path + ".baseZ");
            int baseY = cfg.getInt(path + ".baseY");

            int size = 17;

            Location tp = new Location(
                    p.getWorld(),
                    baseX + (size / 2.0),
                    baseY + 1,
                    baseZ + (size / 2.0)
            );

            p.teleport(tp);
            p.sendMessage("§4İlerlemen sıfırlandı!");
            return true;
        }

        p.sendMessage("§cBilinmeyen komut! /ap create, /ap clear, /ap tp, /ap reset");
        return true;
    }
}