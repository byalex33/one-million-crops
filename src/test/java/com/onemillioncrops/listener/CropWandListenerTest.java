package com.onemillioncrops.listener;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CropWandListenerTest {
    @Test
    void capsDepositsAtTheRemainingCropGoal() {
        assertEquals(40, CropWandListener.depositLimit(64, 60, 100));
        assertEquals(64, CropWandListener.depositLimit(64, 10, 100));
    }

    @Test
    void leavesItemsWhenTheGoalIsCompleteOrInputsAreInvalid() {
        assertEquals(0, CropWandListener.depositLimit(64, 100, 100));
        assertEquals(0, CropWandListener.depositLimit(-1, 0, 100));
    }

    @Test
    void recognisesSeedItemsWithoutTreatingPlantableCropsAsSeeds() {
        assertTrue(CropWandListener.isSeed(Material.WHEAT_SEEDS));
        assertTrue(CropWandListener.isSeed(Material.BEETROOT_SEEDS));
        assertTrue(CropWandListener.isSeed(Material.MELON_SEEDS));
        assertTrue(CropWandListener.isSeed(Material.PUMPKIN_SEEDS));
        assertTrue(CropWandListener.isSeed(Material.TORCHFLOWER_SEEDS));
        assertFalse(CropWandListener.isSeed(Material.WHEAT));
        assertFalse(CropWandListener.isSeed(Material.NETHER_WART));
        assertFalse(CropWandListener.isSeed(null));
    }
}
