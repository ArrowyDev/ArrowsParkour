package me.arrowdev.arrowsParkour.commands;

import me.arrowdev.arrowsParkour.manager.ParkourManager;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.scheduler.BukkitScheduler;

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
            p.sendMessage("§6=== Arrow's Parkour ===\n§e/ap create §7- Parkur oluştur\n§e/ap clear §7- Parkuru temizle\n§e/ap tp §7- Ortaya ışınlan\n§e/ap reset §7- İlerlemeni sıfırla\n§e/ap win §7- Zirveye ışınlan\n§e/ap tnt {username} §7- TNT yolla\n§e/ap winc §7- Win sayısını göster\n§e/ap winadd <sayı> §7- Win ekle\n§e/ap winclear §7- Win'leri sıfırla\n§e/ap area <true/false> §7- Terrain düzenlemesini aç/kapat");
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

        // TP
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

        // RESET
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

        // WIN
        if (args[0].equalsIgnoreCase("win")) {

            FileConfiguration cfg = manager.getPlugin().getConfig();
            String path = "parkours." + p.getUniqueId();

            if (!cfg.contains(path + ".winX")) {
                p.sendMessage("§cWin noktası yok!");
                return true;
            }

            int x = cfg.getInt(path + ".winX");
            int y = cfg.getInt(path + ".winY");
            int z = cfg.getInt(path + ".winZ");

            Location tp = new Location(
                    p.getWorld(),
                    x + 0.5,
                    y + 1,
                    z + 0.5
            );

            p.teleport(tp);
            p.sendMessage("§6Zirveye ışınlandın!");
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            p.getWorld().strikeLightningEffect(p.getLocation());

            return true;
        }

        // WINC - Win sayısını göster
        if (args[0].equalsIgnoreCase("winc")) {
            FileConfiguration cfg = manager.getPlugin().getConfig();
            String path = "parkours." + p.getUniqueId() + ".wins";

            int wins = cfg.getInt(path, 0);
            p.sendMessage("§6Toplam Win Sayısı: §a" + wins);
            return true;
        }

        // WINADD - Win ekle
        if (args[0].equalsIgnoreCase("winadd")) {
            if (args.length < 2) {
                p.sendMessage("§cKullanım: /ap winadd <sayı>");
                return true;
            }

            try {
                int amount = Integer.parseInt(args[1]);
                if (amount <= 0) {
                    p.sendMessage("§cSayı 0'dan büyük olmalı!");
                    return true;
                }

                FileConfiguration cfg = manager.getPlugin().getConfig();
                String path = "parkours." + p.getUniqueId() + ".wins";

                int currentWins = cfg.getInt(path, 0);
                int newWins = currentWins + amount;

                cfg.set(path, newWins);
                manager.getPlugin().saveConfig();

                p.sendMessage("§a" + amount + " win eklendi! Toplam: §6" + newWins);
                return true;
            } catch (NumberFormatException e) {
                p.sendMessage("§cGeçerli bir sayı gir!");
                return true;
            }
        }

        // WINCLEAR - Win'leri sıfırla
        if (args[0].equalsIgnoreCase("winclear")) {
            FileConfiguration cfg = manager.getPlugin().getConfig();
            String path = "parkours." + p.getUniqueId() + ".wins";

            cfg.set(path, 0);
            manager.getPlugin().saveConfig();

            p.sendMessage("§4Win'ler sıfırlandı!");
            return true;
        }

        // AREA - Terrain düzenlemesini aç/kapat
        if (args[0].equalsIgnoreCase("area")) {
            if (args.length < 2) {
                p.sendMessage("§cKullanım: /ap area <true/false>");
                return true;
            }

            ParkourSession session = manager.getSession(p);
            if (session == null) {
                p.sendMessage("§cParkurun yok!");
                return true;
            }

            boolean enable = Boolean.parseBoolean(args[1]);
            session.setAreaEditEnabled(enable);

            if (enable) {
                p.sendMessage("§aArea düzenlemesi AÇILDI! Blokları kırıp koya bilirsin.");
            } else {
                p.sendMessage("§cArea düzenlemesi KAPANDI! Blokları kıramayacaksın.");
                // Config'e kaydet
                FileConfiguration cfg = manager.getPlugin().getConfig();
                String path = "parkours." + p.getUniqueId();

                int baseX = cfg.getInt(path + ".baseX");
                int baseZ = cfg.getInt(path + ".baseZ");
                int baseY = cfg.getInt(path + ".baseY");
                manager.saveParkourSession(p.getUniqueId(), session, baseX, baseZ, baseY);
            }
            return true;
        }

        // TNT
        if (args[0].equalsIgnoreCase("tnt")) {
            Player target = null;
            String displayName = null;

            // Eğer playername belirtildiyse
            if (args.length > 1) {
                // Önce oyuncu olarak ara
                target = p.getServer().getPlayer(args[1]);
                if (target != null) {
                    // Oyuncu bulundu
                    displayName = target.getName();
                    p.sendMessage("§e" + target.getName() + "§a'ya TNT gönderildi!");
                    target.sendMessage("§c" + p.getName() + "§e sana TNT gönderdi!");
                } else {
                    // Oyuncu bulunamadı, custom isim olarak kullan
                    displayName = args[1];
                    p.sendMessage("§e" + displayName + "§a ismiyle TNT gönderildi!");
                }
            } else {
                // Hiç isim belirtilmemişse rastgele isim
                displayName = generateRandomName();
                p.sendMessage("§aRastgele isimle TNT gönderildi!");
            }

            // TNT oluştur
            Location tntLoc;

            if (target != null) {
                // Hedefe TNT gönder
                tntLoc = target.getLocation().add(0, 1, 0);
            } else {
                // Kendine TNT gönder
                tntLoc = p.getLocation().add(0, 1, 0);
            }

            // TNT'yi dünya'ya ekle
            TNTPrimed tnt = p.getWorld().spawn(tntLoc, TNTPrimed.class);
            tnt.setFuseTicks(80); // 4 saniye içinde patlasın
            tnt.setGravity(true); // TNT normal düşsün

            // TNT üstüne armor stand ekle (ad göstermek için)
            ArmorStand armorStand = p.getWorld().spawn(tntLoc.clone().add(0, -0.5, 0), ArmorStand.class);
            armorStand.setCustomName("§6🎁 " + displayName);
            armorStand.setCustomNameVisible(true);
            armorStand.setVisible(false);
            armorStand.setGravity(false); // Armor stand havada kalıyor

            // Her tick'te armor stand'ı TNT'nin konumuna ışınla
            BukkitScheduler scheduler = manager.getPlugin().getServer().getScheduler();
            scheduler.runTaskTimer(manager.getPlugin(), () -> {
                if (!tnt.isValid()) {
                    // TNT yok olmuşsa armor stand'ı sil
                    if (armorStand.isValid()) {
                        armorStand.remove();
                    }
                    return;
                }
                // Armor stand'ı TNT'nin konumuna ışınla (-0.5 aşağı)
                armorStand.teleport(tnt.getLocation().add(0, -0.5, 0));
            }, 0L, 1L); // Her tick çalış

            p.playSound(p.getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 1f);

            return true;
        }

        p.sendMessage("§cBilinmeyen komut! /ap create, /ap clear, /ap tp, /ap reset, /ap win, /ap tnt, /ap winc, /ap winadd, /ap winclear, /ap area");
        return true;
    }

    // Rastgele isim oluştur
    private String generateRandomName() {
        String[] randomNames = {
                "testuser1","testuser2","testuser","arrowtesttnt"
        };
        return randomNames[(int) (Math.random() * randomNames.length)];
    }
}