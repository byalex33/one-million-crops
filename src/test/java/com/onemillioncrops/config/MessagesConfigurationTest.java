package com.onemillioncrops.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MessagesConfigurationTest {
    private static final Pattern LEGACY_PLACEHOLDER = Pattern.compile(
            "<(?:crop|file|completed|total|amount|target|percent|time|players|player|minutes|url|entries|pitch)>"
    );

    @Test
    void everyConfiguredOutputUsesAnEnabledActionSection() {
        var resource = Objects.requireNonNull(getClass().getResourceAsStream("/messages.yml"));
        var yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(resource, StandardCharsets.UTF_8));

        for (String key : yaml.getKeys(false)) {
            if (key.equals("prefix")) {
                continue;
            }
            var section = yaml.getConfigurationSection(key);
            assertNotNull(section, key + " must be an action section");
            assertTrue(section.isBoolean("enabled"), key + " must have an enabled switch");
            assertFalse(section.getStringList("actions").isEmpty(), key + " must have actions");
        }
    }

    @Test
    void configuredMessagesUsePercentPlaceholders() {
        var resource = Objects.requireNonNull(getClass().getResourceAsStream("/messages.yml"));
        var yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(resource, StandardCharsets.UTF_8));

        for (Object value : yaml.getValues(true).values()) {
            if (value instanceof String text) {
                assertFalse(LEGACY_PLACEHOLDER.matcher(text).find(), text);
            } else if (value instanceof List<?> values) {
                for (Object entry : values) {
                    if (entry instanceof String text) {
                        assertFalse(LEGACY_PLACEHOLDER.matcher(text).find(), text);
                    }
                }
            }
        }
    }
}
