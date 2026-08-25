package com.onemillioncrops.listener;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class PlantWandListenerTest {
    private static final UUID WORLD = UUID.randomUUID();

    @Test
    void calculatesInclusiveSelectionVolume() {
        var first = new PlantWandListener.BlockPosition(WORLD, 10, 64, -5);
        var second = new PlantWandListener.BlockPosition(WORLD, 19, 65, 4);

        assertEquals(200L, PlantWandListener.selectionVolume(first, second));
        assertEquals(1L, PlantWandListener.selectionVolume(first, first));
    }

    @Test
    void mapsEveryFarmlandCropToItsPlantingItemAndBlock() {
        assertCrop("wheat", Material.WHEAT_SEEDS, Material.WHEAT);
        assertCrop("carrot", Material.CARROT, Material.CARROTS);
        assertCrop("potato", Material.POTATO, Material.POTATOES);
        assertCrop("beetroot", Material.BEETROOT_SEEDS, Material.BEETROOTS);
        assertCrop("pumpkin", Material.PUMPKIN_SEEDS, Material.PUMPKIN_STEM);
        assertCrop("melon", Material.MELON_SEEDS, Material.MELON_STEM);
        assertCrop("torchflower", Material.TORCHFLOWER_SEEDS, Material.TORCHFLOWER_CROP);
        assertCrop("pitcher", Material.PITCHER_POD, Material.PITCHER_CROP);
        assertNull(PlantWandListener.crop("kelp"));
    }

    @Test
    void capsParticleLocationsWithoutDroppingSmallSelections() {
        assertEquals(1, PlantWandListener.effectStride(20, 240));
        assertEquals(2, PlantWandListener.effectStride(241, 240));
        assertEquals(5, PlantWandListener.effectStride(1_000, 240));
    }

    private static void assertCrop(String id, Material seed, Material block) {
        var crop = PlantWandListener.crop(id);
        assertEquals(seed, crop.seed());
        assertEquals(block, crop.block());
    }
}
