package me.arrowdev.arrowsParkour.commands;

import me.arrowdev.arrowsParkour.manager.ParkourManager;
import me.arrowdev.arrowsParkour.manager.PrisonManager;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import me.arrowdev.arrowsParkour.task.AnimalMovementTask;
import me.arrowdev.arrowsParkour.task.WolfMovementTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wolf;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

public class APCommand implements CommandExecutor {

    private final ParkourManager manager;
    private final PrisonManager prisonManager;
    private final Random random = new Random();

    public APCommand(ParkourManager manager, PrisonManager prisonManager) {
        this.manager = manager;
        this.prisonManager = prisonManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§6=== Arrow's Parkour ===\n§e/ap create §7- Parkur oluştur\n§e/ap clear §7- Parkuru temizle\n§e/ap tp §7- Ortaya ışınlan\n§e/ap rtp §7- Rastgele bloğa ışınlan\n§e/ap tnt {username} §7- TNT yolla\n§e/ap reset §7- İlerlemeni sıfırla\n§e/ap win §7- Zirveye ışınlan\n§e/ap winc §7- Win sayısını göster\n§e/ap winadd <sayı> §7- Win ekle\n§e/ap winremove <sayı> §7- Win eksilt\n§e/ap winclear §7- Win'leri sıfırla\n§e/ap area <true/false> §7- Terrain düzenlemesini aç/kapat\n§e/ap save §7- WorldEdit değişikliklerini kaydet\n§e/ap ike <sayı> §7- İleri koruma ekle\n§e/ap gke <sayı> §7- Geri koruma ekle\n§e/ap prot [clear] §7- Koruma durumunu göster/temizle\n§e/ap dontmove <sayı> §7- Saniye boyunca hareketi engelle\n§e/ap wolf <up|down> <blok> §7- Kurt ile hareket et\n§e/ap chicken <up|down> <blok> §7- Tavuk ile hareket et\n§e/ap cat <up|down> <blok> §7- Kedi ile hareket et\n§e/ap prison <saniye> <username> <oyuncu> §7- Oyuncuyu hapise at");
            return true;
        }

        // ======================== PRİSON KOMUTU ========================
        if (args[0].equalsIgnoreCase("prison")) {
            if (args.length < 4) {
                sender.sendMessage("§cKullanım: /ap prison <saniye> <username> <oyuncu>");
                sender.sendMessage("§7Not: Negatif sayı ile süreden eksilt. Örn: /ap prison -10 Steve oyuncu");
                return true;
            }

            int seconds;
            try {
                seconds = Integer.parseInt(args[1]);
                // ★ DÜZELTİLDİ: 0 kontrolü kaldırıldı, negatif sayılara izin verildi
                // Sadece 0'a eşitse anlamsız olduğu için engelle
                if (seconds == 0) {
                    sender.sendMessage("§c0 girilemez! Pozitif = süre ekle, Negatif = süreden eksilt.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cGeçerli bir saniye sayısı gir! (Negatif de olabilir)");
                return true;
            }

            String username = args[2];
            String playerName = args[3];

            Player target = Bukkit.getPlayerExact(playerName);
            if (target == null) {
                sender.sendMessage("§cOyuncu bulunamadı: " + playerName);
                return true;
            }

            // ★ Negatif sayı girildiyse ama oyuncu hapiste değilse anlamsız
            if (seconds < 0 && !prisonManager.isInPrison(target)) {
                sender.sendMessage("§cOyuncu zaten hapiste değil, süreden eksiltme yapılamaz!");
                return true;
            }

            // Pozitif sayı ise parkur session kontrolü gerekli (hapishane inşası için)
            if (seconds > 0) {
                ParkourSession session = manager.getSession(target);
                if (session == null && !prisonManager.isInPrison(target)) {
                    sender.sendMessage("§cOyuncunun parkuru yok, hapishane inşa edilemez!");
                    return true;
                }
            }

            prisonManager.startPrison(target, seconds, username);

            if (seconds > 0) {
                sender.sendMessage("§a⛓ " + target.getName() + " §e+" + seconds + " §asaniye hapise atıldı/eklendi!");
            } else {
                sender.sendMessage("§e⛓ " + target.getName() + " §csüresinden §e" + Math.abs(seconds) + " §csaniye eksildi!");
            }

            manager.getPlugin().getLogger().info("⛓ Prison: " + target.getName() + " | " + (seconds > 0 ? "+" : "") + seconds + "s | by: " + username);
            return true;
        }

        // Prison olmayan komutlar için normal player parsing
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

        // RTP
        if (args[0].equalsIgnoreCase("rtp")) {
            ParkourSession session = manager.getSession(p);
            if (session == null) {
                p.sendMessage("§cParkurun yok!");
                return true;
            }

            List<Location> jumpBlocks = session.getJumpBlocks();
            if (jumpBlocks.isEmpty()) {
                p.sendMessage("§cJumpBlock listesi boş!");
                return true;
            }

            if (session.hasMount() && session.getMount().getPassengers().contains(p)) {
                session.setMountFinishing(true);
                session.getMount().eject();
                session.removeMount();
                session.setMountFinishing(false);
            }

            manager.cancelCountdown(p);

            int randomIndex = random.nextInt(jumpBlocks.size());
            Location randomBlock = jumpBlocks.get(randomIndex);

            Location teleportLoc = new Location(
                    p.getWorld(),
                    randomBlock.getBlockX() + 0.5,
                    randomBlock.getBlockY() + 1.0,
                    randomBlock.getBlockZ() + 0.5,
                    p.getLocation().getYaw(),
                    p.getLocation().getPitch()
            );

            p.teleport(teleportLoc);
            p.sendMessage("§a🎲 Rastgele bloğa ışınlandın! §7(Blok: §e" + (randomIndex + 1) + "§7/§e" + jumpBlocks.size() + "§7)");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            manager.getPlugin().getLogger().info("🎲 RTP: " + p.getName() + " → Blok " + (randomIndex + 1));
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

            manager.cancelCountdown(p);

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

            ParkourSession session = manager.getSession(p);
            if (session != null) {
                int heightDiff = (y + 1) - session.getStartY();
                manager.startCountdownIfNeeded(p, heightDiff);
            }

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

        // ======================== WOLF / CHICKEN / CAT ========================
        if (args[0].equalsIgnoreCase("wolf")
                || args[0].equalsIgnoreCase("chicken")
                || args[0].equalsIgnoreCase("cat")) {

            if (args.length < 3) {
                p.sendMessage("§cKullanım: /ap " + args[0] + " <up|down> <blok-sayısı>");
                return true;
            }

            try {
                String direction = args[1].toLowerCase();
                int blockCount = Integer.parseInt(args[2]);

                if (blockCount <= 0) {
                    p.sendMessage("§cBlok sayısı 0'dan büyük olmalı!");
                    return true;
                }

                if (!direction.equals("up") && !direction.equals("down")) {
                    p.sendMessage("§cYönerge 'up' ya da 'down' olmalı!");
                    return true;
                }

                ParkourSession session = manager.getSession(p);
                if (session == null) {
                    p.sendMessage("§cParkurun yok!");
                    return true;
                }

                List<Location> jumpBlocks = session.getJumpBlocks();
                if (jumpBlocks.isEmpty()) {
                    p.sendMessage("§cJumpBlock listesi boş!");
                    return true;
                }

                // Önceki mount varsa temizle
                if (session.hasMount()) {
                    session.setMountFinishing(true);
                    session.getMount().eject();
                    session.removeMount();
                    session.setMountFinishing(false);
                }

                if (direction.equals("down")) {
                    manager.cancelCountdown(p);
                }

                Location playerLoc = p.getLocation();
                int detectedIndex = findNearestBlockIndex(playerLoc, jumpBlocks);

                boolean isOnBaseGround = playerLoc.getY() < (jumpBlocks.get(0).getY() + 0.6)
                        || detectedIndex == 0;

                int currentBlockIndex;
                Location animalSpawnLoc;
                int targetBlockIndex;

                if (isOnBaseGround) {
                    currentBlockIndex = 0;
                    Location firstBlock = jumpBlocks.get(0).clone().add(0.5, 1.0, 0.5);
                    p.teleport(firstBlock);
                    p.sendMessage("§a🐾 Başlangıçtan 1. bloğa ışınlandınız.");
                    animalSpawnLoc = firstBlock;

                    if (direction.equals("up")) {
                        targetBlockIndex = Math.min(blockCount, jumpBlocks.size() - 1);
                    } else {
                        targetBlockIndex = 0;
                    }

                } else {
                    currentBlockIndex = detectedIndex;
                    animalSpawnLoc = p.getLocation();

                    if (direction.equals("up")) {
                        targetBlockIndex = Math.min(currentBlockIndex + blockCount + 1, jumpBlocks.size() - 1);
                    } else {
                        targetBlockIndex = Math.max(currentBlockIndex - blockCount, 0);
                    }
                }

                if (targetBlockIndex == currentBlockIndex) {
                    p.sendMessage("§cHedef bloğa ulaşılamadı!");
                    return true;
                }

                Location targetBlock = jumpBlocks.get(targetBlockIndex);
                Location targetLoc = targetBlock.clone().add(0.5, 1.2, 0.5);

                manager.getPlugin().getLogger().info("🐾 " + args[0].toUpperCase() + " → Mevcut: " + currentBlockIndex + " | Hedef: " + targetBlockIndex);

                LivingEntity animal;
                String animalEmoji;

                switch (args[0].toLowerCase()) {
                    case "chicken" -> {
                        Chicken chicken = p.getWorld().spawn(animalSpawnLoc, Chicken.class);
                        chicken.setAI(true);
                        chicken.setGravity(true);
                        chicken.setInvulnerable(true);
                        chicken.setCollidable(false);
                        chicken.setBaby();
                        animal = chicken;
                        animalEmoji = "🐔";
                    }
                    case "cat" -> {
                        Cat cat = p.getWorld().spawn(animalSpawnLoc, Cat.class);
                        cat.setTamed(true);
                        cat.setOwner(p);
                        cat.setAI(true);
                        cat.setGravity(true);
                        cat.setInvulnerable(true);
                        cat.setCollidable(false);
                        animal = cat;
                        animalEmoji = "🐱";
                    }
                    default -> {
                        Wolf wolf = p.getWorld().spawn(animalSpawnLoc, Wolf.class);
                        wolf.setOwner(p);
                        wolf.setTamed(true);
                        wolf.setAI(true);
                        wolf.setGravity(true);
                        wolf.setInvulnerable(true);
                        wolf.setCollidable(false);
                        animal = wolf;
                        animalEmoji = "🐺";
                    }
                }

                session.setMount(animal);

                final LivingEntity finalAnimal = animal;
                Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
                    try {
                        finalAnimal.addPassenger(p);
                    } catch (Exception e) {
                        manager.getPlugin().getLogger().warning("Bind hatası: " + e.getMessage());
                    }
                }, 1L);

                new AnimalMovementTask(manager.getPlugin(), p, animal, session, targetLoc, targetBlockIndex)
                        .runTaskTimer(manager.getPlugin(), 0L, 1L);

                p.sendMessage("§a" + animalEmoji + " " + blockCount + " blok " +
                        (direction.equals("up") ? "yukarı" : "aşağı") + " gidiyoruz...");
                return true;

            } catch (NumberFormatException e) {
                p.sendMessage("§cGeçerli bir sayı gir!");
                return true;
            }
        }

        // AREA SYSTEM
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
                p.sendMessage("§cArea düzenlemesi KAPANDI! Blokları kıramazsın.");
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

                session.addForwardProtection(amount);
                p.sendMessage("§a✓ " + amount + " ileri koruma eklendi!");
                p.sendMessage("§6Mevcut koruma: " + session.getProtectionDisplay());
                manager.getPlugin().getLogger().info("✅ " + p.getName() + " ileri koruma: +" + amount);
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

                session.addBackwardProtection(amount);
                p.sendMessage("§c✓ " + amount + " geri koruma eklendi!");
                p.sendMessage("§6Mevcut koruma: " + session.getProtectionDisplay());
                manager.getPlugin().getLogger().info("✅ " + p.getName() + " geri koruma: +" + amount);
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
                p.sendMessage("§4✓ Tüm korumalar temizlendi!");
                manager.getPlugin().getLogger().info("🗑️ " + p.getName() + " korumalarını temizledi");
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
                }
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

    private int findNearestBlockIndex(Location playerLoc, List<Location> jumpBlocks) {
        int bx = playerLoc.getBlockX();
        int by = playerLoc.getBlockY() - 1;
        int bz = playerLoc.getBlockZ();

        for (int i = 0; i < jumpBlocks.size(); i++) {
            Location jb = jumpBlocks.get(i);
            if (jb.getBlockX() == bx && jb.getBlockY() == by && jb.getBlockZ() == bz) {
                return i;
            }
        }

        int nearestIndex = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < jumpBlocks.size(); i++) {
            Location jb = jumpBlocks.get(i);

            double dy = Math.abs(playerLoc.getY() - (jb.getY() + 1));
            double dx = playerLoc.getX() - (jb.getX() + 0.5);
            double dz = playerLoc.getZ() - (jb.getZ() + 0.5);

            double dist = Math.sqrt(dx * dx + dz * dz) + (dy * 10);

            if (dist < minDistance) {
                minDistance = dist;
                nearestIndex = i;
            }
        }

        return nearestIndex;
    }
}