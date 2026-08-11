package com.onemillioncrops.listener;

import org.junit.jupiter.api.Test;

import static com.onemillioncrops.listener.CropPickupListener.HopperPickupPolicy.BLOCK;
import static com.onemillioncrops.listener.CropPickupListener.HopperPickupPolicy.DEFER_UNTIL_PLAYER;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CropPickupListenerTest {
    @Test
    void defersEligibleHopperPickupsUntilAPlayerCollectsThem() {
        assertEquals(DEFER_UNTIL_PLAYER, CropPickupListener.hopperPickupPolicy(false, true, false));
    }

    @Test
    void defersUnclaimedHopperPickupsWhenAutomaticFarmsAreAllowed() {
        assertEquals(DEFER_UNTIL_PLAYER, CropPickupListener.hopperPickupPolicy(false, false, true));
    }

    @Test
    void blocksIneligibleOrUntraceableHopperPickups() {
        assertEquals(BLOCK, CropPickupListener.hopperPickupPolicy(true, true, true));
        assertEquals(BLOCK, CropPickupListener.hopperPickupPolicy(false, false, false));
    }
}
