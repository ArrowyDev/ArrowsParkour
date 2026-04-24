package me.arrowdev.arrowsParkour.commands;

import me.arrowdev.arrowsParkour.manager.ParkourManager;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wolf;
import org.bukkit.scheduler.BukkitScheduler;
import java.util.Comparator;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.bukkit.util.Vector;
import org.bukkit.scheduler.BukkitRunnable;

import static java.util.Locale.filter;
import static java.util.stream.Collectors.toList;

public class APCommand implements CommandExecutor {

    private final ParkourManager manager;

    public APCommand(ParkourManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§6=== Arrow's Parkour ===\n§e/ap create §7- Parkur oluştur\n§e/ap clear §7- Parkuru temizle\n§e/ap tp §7- Ortaya ışınlan\n§e/ap tnt {username} §7- TNT yolla\n§e/ap reset §7- İlerlemeni sıfırla\n§e/ap win §7- Zirveye ışınlan\n§e/ap winc §7- Win sayısını göster\n§e/ap winadd <sayı> §7- Win ekle\n§e/ap winremove <sayı> §7- Win eksilt\n§e/ap winclear §7- Win'leri sıfırla\n§e/ap area <true/false> §7- Terrain düzenlemesini aç/kapat\n§e/ap save §7- WorldEdit değişikliklerini kaydet\n§e/ap ike <sayı> §7- İleri koruma ekle\n§e/ap gke <sayı> §7- Geri koruma ekle\n§e/ap prot [clear] §7- Koruma durumunu göster/temizle\n§e/ap dontmove <sayı> §7- Saniye boyunca hareketi engelle\n§e/ap wolf <up|down> <blok> §7- Kurt ile hareket et");
            return true;
        }

        Player p;

        if (sender instanceof Player) {
            if (args.length >= 2 && Bukkit.getPlayerExact(args[args.length - 1]) != null) {
                p = Bukkit.getPlayerExact(args[args.length - 1]);
            } else {
                p = (Player) sender;
            }
        } else {
            if (args.length < 2) {
                sender.sendMessage("§cKonsoldan kullanım: /ap <komut> <oyuncu>");
                return true;
            }
            p = Bukkit.getPlayerExact(args[args.length - 1]);
        }

        if (p == null) {
            sender.sendMessage("§cOyuncu bulunamadı!");
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

            ParkourSession session = manager.getSession(p);
            if (session != null) {
                session.setForwardProtection(0);
                session.setBackwardProtection(0);
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
            return true;
        }

        // WINC
        if (args[0].equalsIgnoreCase("winc")) {
            FileConfiguration cfg = manager.getPlugin().getConfig();
            String path = "parkours." + p.getUniqueId() + ".wins";

            int wins = cfg.getInt(path, 0);
            p.sendMessage("§6Toplam Win Sayısı: §a" + wins);
            return true;
        }

        // WINADD
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
                manager.createOrUpdateBossBar(p);
                p.sendMessage("§a" + amount + " win eklendi! Toplam: §6" + newWins);
                return true;
            } catch (NumberFormatException e) {
                p.sendMessage("§cGeçerli bir sayı gir!");
                return true;
            }
        }

        // WINREMOVE
        if (args[0].equalsIgnoreCase("winremove")) {
            if (args.length < 2) {
                p.sendMessage("§cKullanım: /ap winremove <sayı>");
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
                int newWins = currentWins - amount;

                cfg.set(path, newWins);
                manager.getPlugin().saveConfig();
                manager.createOrUpdateBossBar(p);
                p.sendMessage("§a" + amount + " win eksildi! Toplam: §6" + newWins);
                return true;
            } catch (NumberFormatException e) {
                p.sendMessage("§cGeçerli bir sayı gir!");
                return true;
            }
        }

        // WINCLEAR
        if (args[0].equalsIgnoreCase("winclear")) {
            FileConfiguration cfg = manager.getPlugin().getConfig();
            String path = "parkours." + p.getUniqueId() + ".wins";

            cfg.set(path, 0);
            manager.getPlugin().saveConfig();
            manager.createOrUpdateBossBar(p);
            p.sendMessage("§4Win'ler sıfırlandı!");
            return true;
        }

        // DONTMOVE
        if (args[0].equalsIgnoreCase("dontmove")) {
            if (args.length < 2) {
                p.sendMessage("§cKullanım: /ap dontmove <saniye>");
                return true;
            }

            try {
                int seconds = Integer.parseInt(args[1]);
                if (seconds <= 0) {
                    p.sendMessage("§cSüre 0'dan büyük olmalı!");
                    return true;
                }

                manager.freezePlayer(p, seconds);
                return true;
            } catch (NumberFormatException e) {
                p.sendMessage("§cGeçerli bir sayı gir!");
                return true;
            }
        }

        if (args[0].equalsIgnoreCase("wolf")) {

            ParkourSession session = manager.getSession(p);
            if (session == null) {
                p.sendMessage("§cParkur yok!");
                return true;
            }

            if (args.length < 3) {
                p.sendMessage("§cKullanım: /ap wolf <up|down> <blok>");
                return true;
            }

            try {

                String direction = args[1];
                int step = Integer.parseInt(args[2]);

                List<Location> path = new ArrayList<>(session.getJumpBlocks());

                if (path.size() < 2) {
                    p.sendMessage("§cJumpBlock yok!");
                    return true;
                }

                path.sort(Comparator.comparingInt(Location::getBlockY));

                Location playerLoc = p.getLocation();

                // en yakın nokta (SAĞLAM)
                int currentIndex = 0;
                double best = Double.MAX_VALUE;

                for (int i = 0; i < path.size(); i++) {
                    double d = path.get(i).distance(playerLoc);
                    if (d < best) {
                        best = d;
                        currentIndex = i;
                    }
                }

                int targetIndex;

                if (direction.equalsIgnoreCase("up")) {
                    targetIndex = Math.min(currentIndex + step, path.size() - 1);
                } else {
                    targetIndex = Math.max(currentIndex - step, 0);
                }

                if (targetIndex == currentIndex) {
                    p.sendMessage("§cHedef yok!");
                    return true;
                }

                Location target = path.get(targetIndex).clone().add(0.5, 1, 0.5);

                Wolf wolf;

                if (!session.hasWolf()) {
                    wolf = p.getWorld().spawn(p.getLocation(), Wolf.class);
                    wolf.setTamed(true);
                    wolf.setOwner(p);
                    wolf.setAI(false);
                    wolf.setGravity(false);
                    session.setWolf(wolf);
                } else {
                    wolf = session.getWolf();
                }

                Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
                    wolf.addPassenger(p);
                }, 2L);

                new BukkitRunnable() {

                    @Override
                    public void run() {

                        if (!p.isOnline() || !session.hasWolf()) {
                            cancel();
                            return;
                        }

                        Location wolfLoc = wolf.getLocation();

                        double dist = wolfLoc.distance(target);

                        // 🎯 hedefe ulaştı
                        if (dist < 1.5) {

                            session.setCurrentBlockIndex(targetIndex);
                            session.dismountWolf(p);

                            Bukkit.getScheduler().runTaskLater(manager.getPlugin(), session::removeWolf, 10L);

                            p.sendMessage("§a✓ Kurt hedefe ulaştı!");
                            cancel();
                            return;
                        }

                        Vector dir = target.toVector()
                                .subtract(wolfLoc.toVector())
                                .normalize()
                                .multiply(0.8);

                        wolf.teleport(wolfLoc.add(dir));
                    }

                }.runTaskTimer(manager.getPlugin(), 0L, 1L);

                p.sendMessage("§a🐺 Kurt hareket ediyor!");
                return true;

            } catch (Exception e) {
                e.printStackTrace();
                p.sendMessage("§cHata: " + e.getMessage());
                return true;
            }
        }

        // AREA
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
                manager.getPlugin().getLogger().info("🔓 " + p.getName() + " area düzenlemesini açtı");
            } else {
                p.sendMessage("§cArea düzenlemesi KAPANDI! Blokları kıramayacaksın.");
                manager.getPlugin().getLogger().info("🔐 " + p.getName() + " area düzenlemesini kapattı - kaydediliyor...");

                FileConfiguration cfg = manager.getPlugin().getConfig();
                String path = "parkours." + p.getUniqueId();

                int baseX = cfg.getInt(path + ".baseX");
                int baseZ = cfg.getInt(path + ".baseZ");
                int baseY = cfg.getInt(path + ".baseY");
                manager.saveParkourSession(p.getUniqueId(), session, baseX, baseZ, baseY);
            }
            return true;
        }

        // SAVE
        if (args[0].equalsIgnoreCase("save")) {
            ParkourSession session = manager.getSession(p);
            if (session == null) {
                p.sendMessage("§cParkurun yok!");
                return true;
            }

            FileConfiguration cfg = manager.getPlugin().getConfig();
            String path = "parkours." + p.getUniqueId();

            int baseX = cfg.getInt(path + ".baseX");
            int baseZ = cfg.getInt(path + ".baseZ");
            int baseY = cfg.getInt(path + ".baseY");

            int baseWidth = 17;
            int baseLength = 17;
            int maxSteps = 100;

            session.getAllBlocks().clear();
            session.getBlockMaterials().clear();

            int minX = baseX - 1;
            int maxX = baseX + baseWidth;
            int minZ = baseZ - 1;
            int maxZ = baseZ + baseLength;
            int minY = baseY;
            int maxY = baseY + maxSteps + 10;

            int blocksScanned = 0;

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        Location loc = new Location(p.getWorld(), x, y, z);
                        Material blockMat = loc.getBlock().getType();

                        if (blockMat != Material.AIR) {
                            session.addBlock(loc, blockMat);
                            blocksScanned++;

                            manager.getPlugin().getLogger().info("📍 Blok tarandı: " + x + "," + y + "," + z + " -> " + blockMat.name());
                        }
                    }
                }
            }

            manager.saveParkourSession(p.getUniqueId(), session, baseX, baseZ, baseY);
            p.sendMessage("§a✓ " + blocksScanned + " blok tarandı ve kaydedildi!");
            manager.getPlugin().getLogger().info("✅ SAVE tamamlandı: " + blocksScanned + " blok");
            return true;
        }

        // IKE
        if (args[0].equalsIgnoreCase("ike")) {
            if (args.length < 2) {
                p.sendMessage("§cKullanım: /ap ike <sayı>");
                return true;
            }

            try {
                int amount = Integer.parseInt(args[1]);
                if (amount <= 0) {
                    p.sendMessage("§cSayı 0'dan büyük olmalı!");
                    return true;
                }

                ParkourSession session = manager.getSession(p);
                if (session == null) {
                    p.sendMessage("§cParkurun yok!");
                    return true;
                }

                int backwardBefore = session.getBackwardProtection();
                session.addForwardProtection(amount);
                int backwardAfter = session.getBackwardProtection();
                int forwardAmount = session.getForwardProtection();

                p.sendMessage("§a✓ " + amount + " ileri koruma eklendi!");

                if (backwardBefore > backwardAfter) {
                    p.sendMessage("§7Geri korumasından §c" + (backwardBefore - backwardAfter) + " §7düşüldü!");
                }

                p.sendMessage("§6Mevcut koruma: " + session.getProtectionDisplay());

                manager.getPlugin().getLogger().info("✅ " + p.getName() + " ileri koruma: +" + amount + " (İleri: " + forwardAmount + ", Geri: " + backwardAfter + ")");
                return true;
            } catch (NumberFormatException e) {
                p.sendMessage("§cGeçerli bir sayı gir!");
                return true;
            }
        }

        // GKE
        if (args[0].equalsIgnoreCase("gke")) {
            if (args.length < 2) {
                p.sendMessage("§cKullanım: /ap gke <sayı>");
                return true;
            }

            try {
                int amount = Integer.parseInt(args[1]);
                if (amount <= 0) {
                    p.sendMessage("§cSayı 0'dan büyük olmalı!");
                    return true;
                }

                ParkourSession session = manager.getSession(p);
                if (session == null) {
                    p.sendMessage("§cParkurun yok!");
                    return true;
                }

                int forwardBefore = session.getForwardProtection();
                session.addBackwardProtection(amount);
                int forwardAfter = session.getForwardProtection();
                int backwardAmount = session.getBackwardProtection();

                p.sendMessage("§c✓ " + amount + " geri koruma eklendi!");

                if (forwardBefore > forwardAfter) {
                    p.sendMessage("§7İleri korumasından §a" + (forwardBefore - forwardAfter) + " §7düşüldü!");
                }

                p.sendMessage("§6Mevcut koruma: " + session.getProtectionDisplay());

                manager.getPlugin().getLogger().info("✅ " + p.getName() + " geri koruma: +" + amount + " (İleri: " + forwardAfter + ", Geri: " + backwardAmount + ")");
                return true;
            } catch (NumberFormatException e) {
                p.sendMessage("§cGeçerli bir sayı gir!");
                return true;
            }
        }

        // PROT
        if (args[0].equalsIgnoreCase("prot")) {
            ParkourSession session = manager.getSession(p);
            if (session == null) {
                p.sendMessage("§cParkurun yok!");
                return true;
            }

            if (args.length > 1 && args[1].equalsIgnoreCase("clear")) {
                session.setForwardProtection(0);
                session.setBackwardProtection(0);
                p.sendMessage("§4✓ Tüm korumalanız temizlendi!");
                manager.getPlugin().getLogger().info("🗑️ " + p.getName() + " korumalanını temizledi");
                return true;
            }

            p.sendMessage("§6════════════════════════");
            p.sendMessage("§6Koruma Durumu:");
            p.sendMessage(session.getProtectionDisplay());
            p.sendMessage("§6════════════════════════");
            return true;
        }

        // TNT
        if (args[0].equalsIgnoreCase("tnt")) {
            Player target = null;
            String displayName = null;

            if (args.length > 1) {
                target = p.getServer().getPlayer(args[1]);
                if (target != null) {
                    displayName = target.getName();
                    p.sendMessage("§e" + target.getName() + "§a'ya TNT gönderildi!");
                    target.sendMessage("§c" + p.getName() + "§e sana TNT gönderdi!");
                } else {
                    displayName = args[1];
                    p.sendMessage("§e" + displayName + "§a ismiyle TNT gönderildi!");
                }
            } else {
                displayName = generateRandomName();
                p.sendMessage("§aRastgele isimle TNT gönderildi!");
            }

            Location tntLoc;

            if (target != null) {
                tntLoc = target.getLocation().add(0, 1, 0);
            } else {
                tntLoc = p.getLocation().add(0, 1, 0);
            }

            TNTPrimed tnt = p.getWorld().spawn(tntLoc, TNTPrimed.class);
            tnt.setFuseTicks(80);
            tnt.setGravity(true);

            ArmorStand armorStand = p.getWorld().spawn(tntLoc.clone().add(0, -0.5, 0), ArmorStand.class);
            armorStand.setCustomName("§6🎁 " + displayName);
            armorStand.setCustomNameVisible(true);
            armorStand.setVisible(false);
            armorStand.setGravity(false);

            BukkitScheduler scheduler = manager.getPlugin().getServer().getScheduler();
            scheduler.runTaskTimer(manager.getPlugin(), () -> {
                if (!tnt.isValid()) {
                    if (armorStand.isValid()) {
                        armorStand.remove();
                    }
                    return;
                }
                armorStand.teleport(tnt.getLocation().add(0, -0.5, 0));
            }, 0L, 1L);

            p.playSound(p.getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 1f);

            return true;
        }

        sender.sendMessage("§cBilinmeyen komut!");
        return true;
    }

    private String generateRandomName() {
        String[] randomNames = {
                "testuser1","testuser2","testuser","arrowtesttnt"
        };
        return randomNames[(int) (Math.random() * randomNames.length)];
    }
}