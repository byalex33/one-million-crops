package com.onemillioncrops.placeholder;

import com.onemillioncrops.OneMillionCropsPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PlaceholderApiHook {
    private PlaceholderApiHook() {
    }

    public static Runnable register(OneMillionCropsPlugin plugin) {
        PlaceholderExpansion expansion = new Expansion(plugin);
        if (!expansion.register()) {
            plugin.getLogger().warning("Could not register the OneMillionCrops PlaceholderAPI expansion.");
            return () -> { };
        }
        plugin.getLogger().info("Registered PlaceholderAPI expansion: %onemillioncrops_...%");
        return expansion::unregister;
    }

    private static final class Expansion extends PlaceholderExpansion {
        private final OneMillionCropsPlugin plugin;

        private Expansion(OneMillionCropsPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public @NotNull String getIdentifier() {
            return "onemillioncrops";
        }

        @Override
        public @NotNull String getAuthor() {
            return String.join(", ", plugin.getPluginMeta().getAuthors());
        }

        @Override
        public @NotNull String getVersion() {
            return plugin.getPluginMeta().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String onRequest(OfflinePlayer player, @NotNull String params) {
            UUID playerId = player == null ? null : player.getUniqueId();
            return PlaceholderValues.resolve(plugin.progress(), playerId, params);
        }
    }
}
