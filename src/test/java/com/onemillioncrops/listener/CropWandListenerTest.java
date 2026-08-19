package com.onemillioncrops.listener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
