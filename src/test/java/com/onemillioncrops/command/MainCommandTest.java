package com.onemillioncrops.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MainCommandTest {
    @Test
    void formatsSummaryCountdownAndRoundsUpPartialSeconds() {
        assertEquals("30m 0s", MainCommand.formatDuration(1_800_000L));
        assertEquals("1m 1s", MainCommand.formatDuration(60_001L));
        assertEquals("0m 0s", MainCommand.formatDuration(0L));
    }
}
