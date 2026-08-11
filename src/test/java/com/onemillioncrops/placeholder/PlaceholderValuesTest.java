package com.onemillioncrops.placeholder;

import com.onemillioncrops.model.CropDefinition;
import com.onemillioncrops.model.ProgressSnapshot;
import com.onemillioncrops.service.ProgressService;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlaceholderValuesTest {
    private static final UUID PLAYER = UUID.randomUUID();
    private static final UUID OTHER_PLAYER = UUID.randomUUID();

    @Test
    void resolvesChallengeAndPlayerPlaceholders() {
        ProgressService progress = progress(1_000L);
        progress.add(PLAYER, "wheat", 250L);
        progress.add(OTHER_PLAYER, "wheat", 100L);
        progress.add(PLAYER, "nether_wart", 550L);

        assertEquals("900", PlaceholderValues.resolve(progress, PLAYER, "total"));
        assertEquals("900", PlaceholderValues.resolve(progress, PLAYER, "total_formatted"));
        assertEquals("1000", PlaceholderValues.resolve(progress, PLAYER, "target"));
        assertEquals("1,000", PlaceholderValues.resolve(progress, PLAYER, "target_formatted"));
        assertEquals("2000", PlaceholderValues.resolve(progress, PLAYER, "goal"));
        assertEquals("1,100", PlaceholderValues.resolve(progress, PLAYER, "remaining_formatted"));
        assertEquals("45.00", PlaceholderValues.resolve(progress, PLAYER, "percent"));
        assertEquals("0", PlaceholderValues.resolve(progress, PLAYER, "completed_crops"));
        assertEquals("2", PlaceholderValues.resolve(progress, PLAYER, "crop_count"));
        assertEquals("false", PlaceholderValues.resolve(progress, PLAYER, "all_completed"));
        assertEquals("800", PlaceholderValues.resolve(progress, PLAYER, "player_total"));
        assertEquals("0", PlaceholderValues.resolve(progress, null, "player_total"));
    }

    @Test
    void resolvesCropIdsContainingUnderscoresAndFormattedValues() {
        ProgressService progress = progress(1_000_000L);
        progress.add(PLAYER, "nether_wart", 123_456L);

        assertEquals("123456", PlaceholderValues.resolve(progress, PLAYER, "crop_nether_wart"));
        assertEquals("123,456", PlaceholderValues.resolve(progress, PLAYER,
                "CROP_NETHER_WART_AMOUNT_FORMATTED"));
        assertEquals("876544", PlaceholderValues.resolve(progress, PLAYER,
                "crop_nether_wart_remaining"));
        assertEquals("12.35", PlaceholderValues.resolve(progress, PLAYER,
                "crop_nether_wart_percent"));
        assertEquals("false", PlaceholderValues.resolve(progress, PLAYER,
                "crop_nether_wart_completed"));
        assertEquals("123456", PlaceholderValues.resolve(progress, PLAYER,
                "crop_nether_wart_player_amount"));
        assertEquals("0", PlaceholderValues.resolve(progress, null,
                "crop_nether_wart_player_amount"));
    }

    @Test
    void returnsNullForUnknownPlaceholders() {
        ProgressService progress = progress(100L);

        assertNull(PlaceholderValues.resolve(progress, PLAYER, "not_a_placeholder"));
        assertNull(PlaceholderValues.resolve(progress, PLAYER, "crop_unknown_amount"));
        assertNull(PlaceholderValues.resolve(progress, PLAYER, "crop_wheat_unknown"));
    }

    private static ProgressService progress(long target) {
        Map<String, CropDefinition> crops = new LinkedHashMap<>();
        crops.put("wheat", new CropDefinition(
                "wheat", Material.WHEAT, Set.of(Material.WHEAT), "Wheat"));
        crops.put("nether_wart", new CropDefinition(
                "nether_wart", Material.NETHER_WART, Set.of(Material.NETHER_WART), "Nether Wart"));
        return new ProgressService(crops, target, List.of(25, 50, 75, 90),
                new ProgressSnapshot(Map.of(), Map.of(), Map.of()));
    }
}
