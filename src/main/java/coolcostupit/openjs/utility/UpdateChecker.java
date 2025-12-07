/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.utility;

import coolcostupit.openjs.logging.pluginLogger;

import coolcostupit.openjs.modules.FoliaSupport;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class UpdateChecker {
    private final JavaPlugin plugin;
    private static final String VERSION_URL = "https://api.spigotmc.org/legacy/update.php?resource=117328";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final long CHECK_INTERVAL = 20L * 60L * 10L; // 20 ticks * 60 seconds * 10 minutes
    private boolean UpdateAvailable;
    private final pluginLogger Logger;
    private final configurationUtil config;
    public String CurrentVersion;
    public String LatestVersion;
    public UpdateChecker(JavaPlugin plugin, pluginLogger Logger, configurationUtil config) {
        this.plugin = plugin;
        this.Logger = Logger;
        this.config = config;
    }

    public void startChecking() {
        FoliaSupport.ScheduleRepeatingTask(plugin, new BukkitRunnable() {
            @Override
            public void run() {
                CheckForUpdates();
            }}, 0L, CHECK_INTERVAL);

        //}.runTaskTimerAsynchronously(plugin, 0L, CHECK_INTERVAL);
    }
    public void CheckForUpdates() {
        if (config.getConfigFromBuffer("UpdateNotifications", true)) {
            executorService.submit(() -> {
                String currentVersion = plugin.getDescription().getVersion();
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    HttpGet request = new HttpGet(VERSION_URL+"t="+System.currentTimeMillis());
                    HttpResponse response = httpClient.execute(request);
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String latestVersion = EntityUtils.toString(entity).trim();
                        if (compareVersions(currentVersion, latestVersion) < 0) {
                            UpdateAvailable = true;
                            CurrentVersion = currentVersion;
                            LatestVersion = latestVersion;
                            Logger.log(Level.INFO, "A new version of OpenJS is available!", pluginLogger.ORANGE);
                            Logger.log(Level.INFO, "Current version: " + pluginLogger.RED + currentVersion, pluginLogger.ORANGE);
                            Logger.log(Level.INFO, "Latest version: " + pluginLogger.GREEN + latestVersion, pluginLogger.ORANGE);
                            Logger.log(Level.INFO, "Download it here: " + pluginLogger.LIGHT_BLUE + "https://www.spigotmc.org/resources/117328/", pluginLogger.ORANGE);
                        }
                    }
                } catch (IOException e) {
                    Logger.log(Level.SEVERE, "Failed to check for updates: " + e.getMessage(), pluginLogger.RED);
                }
            });
        }
    }

    public boolean UpdatesAvailable() { //from buffer
        return this.UpdateAvailable;
    }

    private int compareVersions(String currentVersion, String latestVersion) {
        // Strip out non-numeric characters
        String[] currentParts = currentVersion.replaceAll("[^0-9.]", "").split("\\.");
        String[] latestParts = latestVersion.replaceAll("[^0-9.]", "").split("\\.");

        int length = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            if (currentPart < latestPart) {
                return -1;
            }
            if (currentPart > latestPart) {
                return 1;
            }
        }
        return 0;
    }
}
