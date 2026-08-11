package com.onemillioncrops.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Locale;

public final class SoundResolver {
    private SoundResolver() {
    }

    public static Sound resolve(String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        String lower = configured.toLowerCase(Locale.ROOT);
        NamespacedKey direct = NamespacedKey.fromString(lower.contains(":") ? lower : "minecraft:" + lower);
        Sound sound = direct == null ? null : Registry.SOUNDS.get(direct);
        if (sound != null) {
            return sound;
        }

        String legacyName = configured.toUpperCase(Locale.ROOT);
        return Registry.SOUNDS.keyStream()
                .filter(key -> legacyName(key).equals(legacyName))
                .findFirst()
                .map(Registry.SOUNDS::get)
                .orElse(null);
    }

    private static String legacyName(NamespacedKey key) {
        return key.getKey().toUpperCase(Locale.ROOT).replace('.', '_').replace('/', '_');
    }
}
