/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package  coolcostupit.openjs.logging;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

// this class is smaller than my Johnson -coolcostupit

public class OpsLogger {
    private static final String permission = "openjs.use";

    public static boolean hasPerm(Player player) {
        return  (player.isOp() || player.hasPermission(permission));
    }

    public static void LogToOps(List<String> messages) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasPerm(player)) {
                for (String message : messages) {
                    player.sendMessage(message);
                }
            }
        }
    }

}
