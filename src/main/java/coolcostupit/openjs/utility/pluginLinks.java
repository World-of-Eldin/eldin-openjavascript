/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.utility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;

import java.util.HashMap;
import java.util.Map;

public class pluginLinks {

    private static final Map<String, TextComponent> cachedLinks = new HashMap<>();
    private static final Map<String, String> links = Map.of(
            "wiki", "https://docs-mc-1.gitbook.io/openjs-docs",
            "download", "https://modrinth.com/plugin/openjavascript/version/latest"
    );

    public static TextComponent getLink(String linkName) {
        return cachedLinks.computeIfAbsent(linkName, key -> {
            String url = links.get(key);
            if (url == null) return null;
            return Component.text(url).clickEvent(ClickEvent.openUrl(url));
        });
    }
}

