package me.arrowdev.arrowsParkour.commands;

import me.arrowdev.arrowsParkour.manager.*;
import me.arrowdev.arrowsParkour.model.ParkourSession;
import me.arrowdev.arrowsParkour.task.AnimalMovementTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class APCommand implements CommandExecutor {

    private final ParkourManager manager;
    private final PrisonManager prisonManager;
    private final LavaManager lavaManager;
    private final Random random = new Random();
    private final IceManager iceManager;
    private final InvisibleManager invisibleManager;
    private final ArrowRainManager arrowRainManager;
    private final ChaosManager chaosManager;
    private final GravityManager gravityManager;

    private final Map<UUID, BukkitTask> rrTasks = new HashMap<>();

    // ★ Kaos cleanup task'larını izlemek için
    private final Map<UUID, BukkitTask> chaosTasks = new HashMap<>();

    public APCommand(ParkourManager manager, PrisonManager prisonManager,
                     LavaManager lavaManager, IceManager iceManager,
                     InvisibleManager invisibleManager,
                     ArrowRainManager arrowRainManager,ChaosManager chaosManager,
                     GravityManager gravityManager) {
        this.manager = manager;
        this.prisonManager = prisonManager;
        this.lavaManager = lavaManager;
        this.iceManager = iceManager;
        this.invisibleManager = invisibleManager;
        this.arrowRainManager = arrowRainManager;
        this.chaosManager = chaosManager;
        this.gravityManager = gravityManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§6=== Arrow's Parkour ===\n"
                    + "§e/ap create §7- Parkur oluştur\n"
                    + "§e/ap clear §7- Parkuru temizle\n"
                    + "§e/ap tp §7- Ortaya ışınlan\n"
                    + "§e/ap rtp §7- Rastgele bloğa ışınlan\n"
                    + "§e/ap tnt <username> §7- TNT yolla\n"
                    + "§e/ap reset §7- İlerlemeni sıfırla\n"
                    + "§e/ap win §7- Zirveye ışınlan\n"
                    + "§e/ap winc §7- Win sayısını göster\n"
                    + "§e/ap winadd <sayı> §7- Win ekle\n"
                    + "§e/ap winremove <sayı> §7- Win eksilt\n"
                    + "§e/ap winclear §7- Win'leri sıfırla\n"
                    + "§e/ap area <true/false> §7- Terrain düzenlemesini aç/kapat\n"
                    + "§e/ap save §7- WorldEdit değişikliklerini kaydet\n"
                    + "§e/ap ike <sayı> §7- İleri koruma ekle\n"
                    + "§e/ap gke <sayı> §7- Geri koruma ekle\n"
                    + "§e/ap prot [clear] §7- Koruma durumunu göster/temizle\n"
                    + "§e/ap dontmove <sayı> §7- Saniye boyunca hareketi engelle\n"
                    + "§e/ap wolf <up|down> <blok> §7- Kurt ile hareket et\n"
                    + "§e/ap chicken <up|down> <blok> §7- Tavuk ile hareket et\n"
                    + "§e/ap cat <up|down> <blok> §7- Kedi ile hareket et\n"
                    + "§e/ap prison <saniye> <username> [oyuncu] §7- Oyuncuyu hapise at\n"
                    + "§e/ap lava <saniye> §7- Lav yükseltmeyi başlat\n"
                    + "§e/ap lavastop §7- Lavayı durdur\n"
                    + "§e/ap blind <saniye> [oyuncu] §7- Oyuncuyu kör et\n"
                    + "§e/ap rr [oyuncu] §7- Kafayı rastgele döndür\n"
                    + "§e/ap jump [oyuncu] §7- Oyuncuyu zıplat\n"
                    + "§e/ap drunk <saniye> [oyuncu] §7- Oyuncuyu sarhoş et\n"
                    + "§e/ap ice <saniye> [oyuncu] §7- Parkuru buza çevir\n"
                    + "§e/ap invisible <saniye> [oyuncu] §7- Parkuru görünmez yap\n"
                    + "§e/ap arrowrain <saniye> [oyuncu] §7- Ok yağmuru başlat\n"
                    + "§e/ap chaos <saniye> <username> [oyuncu] §7- Kaos modu aktifleştir\n"
                    + "§e/ap ppickaxe [oyuncu] §7- Hapishaneye kazma ver\n"
                    + "§e/ap ppickaxe [oyuncu] §7- Hapishaneye kazma ver\n"
            );
            return true;
        }

        //Chaos Mode
        if (args[0].equalsIgnoreCase("chaos")) {
            if (args.length < 4) {
                sender.sendMessage("§cKullanım: /ap chaos <saniye> <username> <oyuncu>");
                return true;
            }

            int seconds;
            try {
                seconds = Integer.parseInt(args[1]);
                if (seconds <= 0) {
                    sender.sendMessage("§cSüre 0'dan büyük olmalı!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cGeçerli bir saniye gir!");
                return true;
            }

            String username   = args[2];
            String playerName = args[3];

            Player target = Bukkit.getPlayerExact(playerName);
            if (target == null) {
                sender.sendMessage("§cOyuncu bulunamadı: " + playerName);
                return true;
            }

            if (manager.getSession(target) == null) {
                sender.sendMessage("§cOyuncunun parkuru yok!");
                return true;
            }

            // ★ Tüm kaos mantığı artık ChaosManager'da
            chaosManager.startChaos(target, seconds, username);

            sender.sendMessage("§a☠ " + target.getName()
                    + " için KAOS MODU başlatıldı! §7(" + seconds + "s)");
            return true;
        }

        // ================================================================
        // ★ ARROWRAIN KOMUTU — YENİ
        // ================================================================
        if (args[0].equalsIgnoreCase("arrowrain")) {
            if (args.length < 2) {
                sender.sendMessage("§cKullanım: /ap arrowrain <saniye> [oyuncu]");
                return true;
            }

            int seconds;
            try {
                seconds = Integer.parseInt(args[1]);
                if (seconds <= 0) {
                    sender.sendMessage("§cSüre 0'dan büyük olmalı!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cGeçerli bir saniye gir!");
                return true;
            }

            Player target;
            if (args.length >= 3) {
                target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage("§cOyuncu bulunamadı: " + args[2]);
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cKonsoldan kullanım: /ap arrowrain <saniye> <oyuncu>");
                return true;
            }

            arrowRainManager.startArrowRain(target, seconds);

            if (!sender.equals(target)) {
                sender.sendMessage("§c🏹 " + target.getName()
                        + " §e" + seconds + " §csaniye ok yağmuru başlatıldı!");
            }

            manager.getPlugin().getLogger().info(
                    "🏹 ArrowRain: " + target.getName() + " | " + seconds + "s | by: " + sender.getName()
            );
            return true;
        }

        // ================================================================
        // ★ ARROWRAINSTOP KOMUTU — YENİ
        // ================================================================
        if (args[0].equalsIgnoreCase("arrowrainstop")) {
            Player target;
            if (args.length >= 2) {
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cOyuncu bulunamadı: " + args[1]);
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cKonsoldan kullanım: /ap arrowrainstop <oyuncu>");
                return true;
            }

            if (!arrowRainManager.hasArrowRain(target)) {
                sender.sendMessage("§cBu oyuncunun aktif ok yağmuru yok!");
                return true;
            }

            arrowRainManager.stopArrowRain(target);
            sender.sendMessage("§a🏹 " + target.getName() + " için ok yağmuru durduruldu!");
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

            if (seconds < 0 && !prisonManager.isInPrison(target)) {
                sender.sendMessage("§cOyuncu zaten hapiste değil, süreden eksiltme yapılamaz!");
                return true;
            }

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

            manager.getPlugin().getLogger().info("⛓ Prison: " + target.getName()
                    + " | " + (seconds > 0 ? "+" : "") + seconds + "s | by: " + username);
            return true;
        }

        // ======================== BLIND KOMUTU ========================
        if (args[0].equalsIgnoreCase("blind")) {
            if (args.length < 2) {
                sender.sendMessage("§cKullanım: /ap blind <saniye> [oyuncu]");
                return true;
            }

            int seconds;
            try {
                seconds = Integer.parseInt(args[1]);
                if (seconds <= 0) {
                    sender.sendMessage("§cSüre 0'dan büyük olmalı!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cGeçerli bir saniye gir!");
                return true;
            }

            Player target;
            if (args.length >= 3) {
                target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage("§cOyuncu bulunamadı: " + args[2]);
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cKonsoldan kullanım: /ap blind <saniye> <oyuncu>");
                return true;
            }

            int durationTicks = seconds * 20;

            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.BLINDNESS,
                    durationTicks, 0, false, false, false
            ));

            target.sendTitle(
                    "§8🕶 KÖRLEŞME",
                    "§7" + seconds + " saniye boyunca göremezsin!",
                    10, 50, 10
            );
            target.sendMessage("§8🕶 " + seconds + " saniye körleştin!");
            sender.sendMessage("§a🕶 " + target.getName() + " §e" + seconds + " §asaniye körleştirildi!");

            manager.getPlugin().getLogger().info(
                    "🕶 Blind: " + target.getName() + " | " + seconds + "s | by: " + sender.getName()
            );
            return true;
        }

        // ======================== RR KOMUTU ========================
        if (args[0].equalsIgnoreCase("rr")) {
            Player target;

            if (args.length >= 2) {
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cOyuncu bulunamadı: " + args[1]);
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cKonsoldan kullanım: /ap rr <oyuncu>");
                return true;
            }

            Location loc = target.getLocation().clone();
            float randomYaw = (random.nextFloat() * 360f) - 180f;
            loc.setYaw(randomYaw);
            loc.setPitch(0f);
            target.teleport(loc);

            target.sendMessage("§c🌀 Kafan aniden başka bir yöne döndü!");

            if (!sender.equals(target)) {
                sender.sendMessage("§a🌀 " + target.getName() + " adlı oyuncunun kafası rastgele döndürüldü!");
            }

            manager.getPlugin().getLogger().info(
                    "🌀 RR: " + target.getName() + " | by: " + sender.getName()
            );
            return true;
        }

        // ======================== JUMP KOMUTU ========================
        if (args[0].equalsIgnoreCase("jump")) {
            Player target;

            if (args.length >= 2) {
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cOyuncu bulunamadı: " + args[1]);
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cKonsoldan kullanım: /ap jump <oyuncu>");
                return true;
            }

            if (!target.isOnGround()) {
                sender.sendMessage("§cOyuncu havada, zıplatılamaz!");
                return true;
            }

            org.bukkit.util.Vector velocity = target.getVelocity();
            velocity.setY(0.92);
            target.setVelocity(velocity);

            if (!sender.equals(target)) {
                sender.sendMessage("§a⬆ " + target.getName() + " zıplatıldı!");
            }

            manager.getPlugin().getLogger().info(
                    "⬆ Jump: " + target.getName() + " | by: " + sender.getName()
            );
            return true;
        }

        // ======================== DRUNK KOMUTU ========================
        if (args[0].equalsIgnoreCase("drunk")) {
            if (args.length < 2) {
                sender.sendMessage("§cKullanım: /ap drunk <saniye> [oyuncu]");
                return true;
            }

            int seconds;
            try {
                seconds = Integer.parseInt(args[1]);
                if (seconds <= 0) {
                    sender.sendMessage("§cSüre 0'dan büyük olmalı!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cGeçerli bir saniye gir!");
                return true;
            }

            Player target;
            if (args.length >= 3) {
                target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage("§cOyuncu bulunamadı: " + args[2]);
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cKonsoldan kullanım: /ap drunk <saniye> <oyuncu>");
                return true;
            }

            int durationTicks = seconds * 20;

            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.NAUSEA,
                    durationTicks, 0, false, false, false
            ));

            target.sendTitle(
                    "§2🍺 SARHOŞ",
                    "§7" + seconds + " saniye boyunca kafan dönüyor!",
                    10, 50, 10
            );
            target.sendMessage("§2🍺 " + seconds + " saniye sarhoş oldun!");

            if (!sender.equals(target)) {
                sender.sendMessage("§a🍺 " + target.getName() + " §e" + seconds + " §asaniye sarhoş edildi!");
            }

            manager.getPlugin().getLogger().info(
                    "🍺 Drunk: " + target.getName() + " | " + seconds + "s | by: " + sender.getName()
            );
            return true;
        }

        // ======================== ICE KOMUTU ========================
        if (args[0].equalsIgnoreCase("ice")) {
            if (args.length < 2) {
                sender.sendMessage("§cKullanım: /ap ice <saniye> [oyuncu]");
                return true;
            }

            int seconds;
            try {
                seconds = Integer.parseInt(args[1]);
                if (seconds <= 0) {
                    sender.sendMessage("§cSüre 0'dan büyük olmalı!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cGeçerli bir saniye gir!");
                return true;
            }

            Player target;
            if (args.length >= 3) {
                target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage("§cOyuncu bulunamadı: " + args[2]);
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cKonsoldan kullanım: /ap ice <saniye> <oyuncu>");
                return true;
            }

            iceManager.startIce(target, seconds);

            if (!sender.equals(target)) {
                sender.sendMessage("§b🧊 " + target.getName()
                        + " §e" + seconds + " §bsaniye parkuru buza döndürüldü!");
            }

            manager.getPlugin().getLogger().info(
                    "🧊 Ice: " + target.getName() + " | " + seconds + "s | by: " + sender.getName()
            );
            return true;
        }

        // ======================== GRAVITY KOMUTU ========================
        if (args[0].equalsIgnoreCase("gravity")) {
            if (args.length < 3) {
                sender.sendMessage("§cKullanım: /ap gravity <low|high> <saniye> [oyuncu]");
                return true;
            }

            String mode = args[1].toLowerCase();
            if (!mode.equals("low") && !mode.equals("high")) {
                sender.sendMessage("§cMod 'low' ya da 'high' olmalı!");
                return true;
            }

            int seconds;
            try {
                seconds = Integer.parseInt(args[2]);
                if (seconds <= 0) {
                    sender.sendMessage("§cSüre 0'dan büyük olmalı!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cGeçerli bir saniye gir!");
                return true;
            }

            Player target;
            if (args.length >= 4) {
                target = Bukkit.getPlayerExact(args[3]);
                if (target == null) {
                    sender.sendMessage("§cOyuncu bulunamadı: " + args[3]);
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cKonsoldan kullanım: /ap gravity <low|high> <saniye> <oyuncu>");
                return true;
            }

            gravityManager.startGravity(target, mode, seconds);

            if (!sender.equals(target)) {
                sender.sendMessage("§a🪶 " + target.getName() + " için "
                        + (mode.equals("low") ? "düşük" : "yüksek")
                        + " yerçekimi başlatıldı! §7(" + seconds + "s)");
            }

            manager.getPlugin().getLogger().info(
                    "🪶 Gravity: " + target.getName() + " | " + mode + " | " + seconds + "s | by: " + sender.getName()
            );
            return true;
        }

        // ======================== GRAVITYSTOP KOMUTU ========================
        if (args[0].equalsIgnoreCase("gravitystop")) {
            Player target;
            if (args.length >= 2) {
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cOyuncu bulunamadı: " + args[1]);
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cKonsoldan kullanım: /ap gravitystop <oyuncu>");
                return true;
            }

            if (!gravityManager.hasGravity(target)) {
                sender.sendMessage("§cBu oyuncunun aktif yerçekimi etkisi yok!");
                return true;
            }

            gravityManager.stopGravity(target, true);
            sender.sendMessage("§a🪶 " + target.getName() + " için yerçekimi durduruldu!");
            return true;
        }

        // ======================== PPICKAXE KOMUTU ========================
        if (args[0].equalsIgnoreCase("ppickaxe")) {
            Player target;
            if (args.length >= 2) {
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cOyuncu bulunamadı: " + args[1]);
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cKonsoldan kullanım: /ap ppickaxe <oyuncu>");
                return true;
            }

            prisonManager.givePrisonPickaxe(target);

            if (!sender.equals(target)) {
                sender.sendMessage("§a⛓ " + target.getName() + " için hapishane kazması verildi!");
            }

            manager.getPlugin().getLogger().info(
                    "⛓ Pickaxe: " + target.getName() + " | by: " + sender.getName()
            );
            return true;
        }



        // ======================== INVISIBLE KOMUTU ========================
        if (args[0].equalsIgnoreCase("invisible")) {
            if (args.length < 2) {
                sender.sendMessage("§cKullanım: /ap invisible <saniye> [oyuncu]");
                return true;
            }

            int seconds;
            try {
                seconds = Integer.parseInt(args[1]);
                if (seconds <= 0) {
                    sender.sendMessage("§cSüre 0'dan büyük olmalı!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cGeçerli bir saniye gir!");
                return true;
            }

            Player target;
            if (args.length >= 3) {
                target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage("§cOyuncu bulunamadı: " + args[2]);
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cKonsoldan kullanım: /ap invisible <saniye> <oyuncu>");
                return true;
            }

            invisibleManager.startInvisible(target, seconds);

            if (!sender.equals(target)) {
                sender.sendMessage("§7👻 " + target.getName()
                        + " §e" + seconds + " §7saniye parkuru görünmez yapıldı!");
            }
            return true;
        }

        // ======================== LAVA KOMUTU ========================
        if (args[0].equalsIgnoreCase("lava")) {
            if (args.length < 2) {
                sender.sendMessage("§cKullanım: /ap lava <saniye> [oyuncu]");
                sender.sendMessage("§7Saniye = her kaç saniyede bir lav 1 seviye yükselir");
                return true;
            }

            int intervalSeconds;
            try {
                intervalSeconds = Integer.parseInt(args[1]);
                if (intervalSeconds <= 0) {
                    sender.sendMessage("§cSaniye 0'dan büyük olmalı!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cGeçerli bir saniye gir!");
                return true;
            }

            Player target;
            if (args.length >= 3) {
                target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage("§cOyuncu bulunamadı: " + args[2]);
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cKonsoldan kullanım: /ap lava <saniye> <oyuncu>");
                return true;
            }

            int intervalTicks = intervalSeconds * 20;
            lavaManager.startLava(target, intervalTicks);
            sender.sendMessage("§c🌋 " + target.getName() + " için lav yükseltme başlatıldı! §7(Her "
                    + intervalSeconds + " saniyede 1 seviye)");
            return true;
        }

        // ======================== LAVASTOP KOMUTU ========================
        if (args[0].equalsIgnoreCase("lavastop")) {
            Player target;
            if (args.length >= 2) {
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cOyuncu bulunamadı: " + args[1]);
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cKonsoldan kullanım: /ap lavastop <oyuncu>");
                return true;
            }

            if (!lavaManager.hasLava(target)) {
                sender.sendMessage("§cBu oyuncunun aktif lav yükseltmesi yok!");
                return true;
            }

            lavaManager.stopLava(target);
            sender.sendMessage("§a🌋 " + target.getName() + " için lav durduruldu ve temizlendi!");
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
            if (lavaManager.hasLava(p)) lavaManager.stopLava(p);
            if (arrowRainManager.hasArrowRain(p)) arrowRainManager.stopArrowRain(p);  // ★ YENİ
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
            p.sendMessage("§a🎲 Rastgele bloğa ışınlandın! §7(Blok: §e"
                    + (randomIndex + 1) + "§7/§e" + jumpBlocks.size() + "§7)");
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

            Location tp = new Location(p.getWorld(), x + 0.5, y + 1, z + 0.5);

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

                manager.getPlugin().getLogger().info("🐾 " + args[0].toUpperCase()
                        + " → Mevcut: " + currentBlockIndex + " | Hedef: " + targetBlockIndex);

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
                manager.getPlugin().getLogger().info("🔐 " + p.getName()
                        + " area düzenlemesini kapattı - kaydediliyor...");

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
                            manager.getPlugin().getLogger().info("📍 Blok tarandı: "
                                    + x + "," + y + "," + z + " -> " + blockMat.name());
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

            Player tntTarget = (target != null) ? target : p;

            Location tntLoc = tntTarget.getLocation().add(0, 1, 0);
            if (tntLoc.getBlock().getType().isSolid()) {
                tntLoc = tntTarget.getEyeLocation();
            }
            if (tntLoc.getBlock().getType().isSolid()) {
                tntLoc = tntTarget.getLocation();
            }

            TNTPrimed tnt = p.getWorld().spawn(tntLoc, TNTPrimed.class);
            tnt.setFuseTicks(10);
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

    // ================================================================
    // ★ KAOS CLEANUP METHODU
    // ================================================================
    private void cleanupChaos(Player target, BukkitTask actionBarTask) {
        UUID uuid = target.getUniqueId();

        // Action bar task'ı durdur
        if (actionBarTask != null) {
            try { actionBarTask.cancel(); } catch (Exception ignored) {}
        }

        // Kaos countdown task'ı durdur
        BukkitTask chaosTask = chaosTasks.remove(uuid);
        if (chaosTask != null) {
            try { chaosTask.cancel(); } catch (Exception ignored) {}
        }

        // Lavı durdur
        if (lavaManager.hasLava(target)) {
            lavaManager.stopLava(target);
        }

        // Ok yağmurunu durdur
        if (arrowRainManager.hasArrowRain(target)) {
            arrowRainManager.stopArrowRain(target);
        }

        // Kırmızı atmosferi kaldır
        try {
            target.setWorldBorder(null);
        } catch (Exception ignored) {}

        // İksir efektlerini kaldır (blind + nausea)
        target.removePotionEffect(PotionEffectType.BLINDNESS);
        target.removePotionEffect(PotionEffectType.NAUSEA);

        // Ice ve Invisible kendi timer'larıyla durur, ama garantiye alalım
        // (Zaten stopIce/stopInvisible idempotent)

        manager.getPlugin().getLogger().info("☠ Kaos temizlendi: " + target.getName());
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