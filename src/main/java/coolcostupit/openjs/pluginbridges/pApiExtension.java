/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.pluginbridges;

import coolcostupit.openjs.BridgeLoaders.PlaceholderAPI;
import coolcostupit.openjs.modules.sharedClass;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class pApiExtension extends PlaceholderExpansion {

    public pApiExtension() {
    }

    @Override
    public @NotNull String getIdentifier() {
        return sharedClass.Identifier;
    }

    @Override
    public @NotNull String getAuthor() {
        String authors = sharedClass.PluginDescription.getAuthors().toString();
        return authors.substring(1, authors.length() - 1);
    }

    @Override
    public @NotNull String getVersion() {
        return sharedClass.PluginDescription.getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        String prefix;
        String param;

        int underscore = params.indexOf('_');
        if (underscore == -1) {
            // No params found
            prefix = params;
            param = "";
        } else {
            prefix = params.substring(0, underscore);
            param = params.substring(underscore + 1);
        }

        return PlaceholderAPI.placeholderApiJS.invokePrefix(prefix, player, param);
    }
}