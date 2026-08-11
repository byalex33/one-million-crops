package com.onemillioncrops.listener;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerListenerTest {
    @Test
    void protectsPlayersInsideSweetBerryBushes() {
        assertTrue(PlayerListener.shouldCancelBerryBushCollision(true, Material.SWEET_BERRY_BUSH));
    }

    @Test
    void leavesOtherBlocksAndEntitiesUnchanged() {
        assertFalse(PlayerListener.shouldCancelBerryBushCollision(true, Material.COBWEB));
        assertFalse(PlayerListener.shouldCancelBerryBushCollision(false, Material.SWEET_BERRY_BUSH));
    }
}
