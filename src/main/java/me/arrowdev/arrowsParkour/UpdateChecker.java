package me.arrowdev.arrowsParkour;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private final ArrowsParkour plugin;
    private final String currentVersion;
    private final String repoOwner;
    private final String repoName;

    private String latestVersion = null;
    private String downloadUrl = null;

    public UpdateChecker(ArrowsParkour plugin, String repoOwner, String repoName) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.repoOwner = repoOwner;
        this.repoName = repoName;
    }

    public void checkAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String apiUrl = "https://api.github.com/repos/"
                        + repoOwner + "/" + repoName + "/releases/latest";

                HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setRequestProperty("User-Agent", "ArrowsParkour-UpdateChecker");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();

                if (responseCode != 200) {
                    plugin.getLogger().warning("⚠ Güncelleme kontrolü başarısız! HTTP: " + responseCode);
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // JSON'u manuel parse et — harici kütüphane gerektirmez
                String json = response.toString();

                latestVersion = extractJsonValue(json, "tag_name")
                        .replace("v", "")
                        .trim();

                downloadUrl = extractJsonValue(json, "html_url");

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (isUpdateAvailable()) {
                        plugin.getLogger().info("╔═════════════════════════════════════╗");
                        plugin.getLogger().info("║     🔔 YENİ SÜRÜM MEVCUT!           ║");
                        plugin.getLogger().info("║  Mevcut : v" + currentVersion);
                        plugin.getLogger().info("║  Yeni   : v" + latestVersion);
                        plugin.getLogger().info("║  İndir  : " + downloadUrl);
                        plugin.getLogger().info("╚═════════════════════════════════════╝");
                    } else {
                        plugin.getLogger().info("✅ Arrow's Parkour güncel! (v" + currentVersion + ")");
                    }
                });

            } catch (Exception e) {
                plugin.getLogger().warning("⚠ Güncelleme kontrolünde hata: " + e.getMessage());
            }
        });
    }


    public void notifyPlayer(Player player) {
        if (!player.hasPermission("arrowsparkour.admin")) return;
        if (!isUpdateAvailable()) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("§6╔═════════════════════════════════╗");
            player.sendMessage("§6║  §e🔔 Arrow's Parkour Güncellemesi §6║");
            player.sendMessage("§6╠═════════════════════════════════╣");
            player.sendMessage("§6║  §7Mevcut: §cv" + currentVersion);
            player.sendMessage("§6║  §7Yeni   : §av" + latestVersion);
            player.sendMessage("§6║  §7İndir  : §b" + downloadUrl);
            player.sendMessage("§6╚═════════════════════════════════╝");
        }, 40L);
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return "";

        int valueStart = json.indexOf("\"", colonIndex) + 1;
        int valueEnd = json.indexOf("\"", valueStart);

        if (valueStart <= 0 || valueEnd <= 0) return "";

        return json.substring(valueStart, valueEnd);
    }

    private boolean isUpdateAvailable() {
        if (latestVersion == null || latestVersion.isEmpty()) return false;
        if (latestVersion.equals(currentVersion)) return false;

        try {
            // Suffix'i ayır → "1.5.2-alpha" → ["1.5.2", "alpha"]
            String[] currentParts = splitVersion(currentVersion);
            String[] latestParts  = splitVersion(latestVersion);

            String currentNumbers = currentParts[0]; // "1.5.2"
            String latestNumbers  = latestParts[0];  // "1.5.3"
            String currentSuffix  = currentParts[1]; // "alpha"
            String latestSuffix   = latestParts[1];  // ""

            // Sayısal kısımları karşılaştır
            String[] current = currentNumbers.split("\\.");
            String[] latest  = latestNumbers.split("\\.");

            int length = Math.max(current.length, latest.length);

            for (int i = 0; i < length; i++) {
                int c = i < current.length ? Integer.parseInt(current[i]) : 0;
                int l = i < latest.length  ? Integer.parseInt(latest[i])  : 0;

                if (l > c) return true;
                if (l < c) return false;
            }

            // Sayılar eşitse suffix'e bak
            // Sıralama: release > rc > beta > alpha > dev
            int currentWeight = getSuffixWeight(currentSuffix);
            int latestWeight  = getSuffixWeight(latestSuffix);

            return latestWeight > currentWeight;

        } catch (NumberFormatException e) {
            return !latestVersion.equals(currentVersion);
        }
    }

    // Versiyonu sayı ve suffix olarak ikiye ayır
// "1.5.2-alpha" → ["1.5.2", "alpha"]
// "1.5.2"       → ["1.5.2", ""]
    private String[] splitVersion(String version) {
        if (version.contains("-")) {
            String[] parts = version.split("-", 2);
            return new String[]{ parts[0], parts[1].toLowerCase() };
        }
        return new String[]{ version, "" };
    }

    // Suffix ağırlıkları — büyük = daha stabil
// "1.5.2" (release) > "1.5.2-rc" > "1.5.2-beta" > "1.5.2-alpha" > "1.5.2-dev"
    private int getSuffixWeight(String suffix) {
        return switch (suffix) {
            case ""      -> 5; // release — en stabil
            case "rc"    -> 4;
            case "beta"  -> 3;
            case "alpha" -> 2;
            case "dev"   -> 1;
            default      -> 0;
        };
    }

    public String getLatestVersion() { return latestVersion; }
    public String getDownloadUrl()   { return downloadUrl; }
}