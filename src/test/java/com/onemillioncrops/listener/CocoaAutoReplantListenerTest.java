package com.onemillioncrops.listener;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CocoaAutoReplantListenerTest {
    @Test
    void followsThePistonInBothDirections() {
        assertEquals(BlockFace.UP, CocoaAutoReplantListener.pistonMovement(BlockFace.UP, true));
        assertEquals(BlockFace.DOWN, CocoaAutoReplantListener.pistonMovement(BlockFace.UP, false));
        assertEquals(BlockFace.WEST, CocoaAutoReplantListener.pistonMovement(BlockFace.EAST, false));
    }

    @Test
    void recognisesJungleLogsAsCocoaSupports() {
        assertTrue(CocoaAutoReplantListener.isJungleSupport(Material.JUNGLE_LOG));
        assertTrue(CocoaAutoReplantListener.isJungleSupport(Material.JUNGLE_WOOD));
        assertTrue(CocoaAutoReplantListener.isJungleSupport(Material.STRIPPED_JUNGLE_LOG));
        assertFalse(CocoaAutoReplantListener.isJungleSupport(Material.OAK_LOG));
    }

    @Test
    void recognisesThatCocoaFacesBackTowardItsSupportingLog() {
        assertTrue(CocoaAutoReplantListener.isAttachedToSupport(BlockFace.NORTH, BlockFace.SOUTH));
        assertTrue(CocoaAutoReplantListener.isAttachedToSupport(BlockFace.WEST, BlockFace.EAST));
        assertFalse(CocoaAutoReplantListener.isAttachedToSupport(BlockFace.NORTH, BlockFace.NORTH));
    }

    @Test
    void chargesExactlyOneBeanForReplanting() {
        assertEquals(2, CocoaAutoReplantListener.remainingAfterReplantCost(Material.COCOA_BEANS, 3));
        assertEquals(0, CocoaAutoReplantListener.remainingAfterReplantCost(Material.COCOA_BEANS, 1));
        assertEquals(-1, CocoaAutoReplantListener.remainingAfterReplantCost(Material.COCOA_BEANS, 0));
        assertEquals(-1, CocoaAutoReplantListener.remainingAfterReplantCost(Material.WHEAT, 3));
    }
}
