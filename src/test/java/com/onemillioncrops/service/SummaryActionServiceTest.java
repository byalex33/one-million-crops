package com.onemillioncrops.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class SummaryActionServiceTest {
    @Test
    void parsesActionTagsAndAllowsBlankMessages() {
        assertEquals(new SummaryActionService.ParsedAction("message", ""),
                SummaryActionService.parse("[message] "));
        assertEquals(new SummaryActionService.ParsedAction("sound", "BLOCK_NOTE_BLOCK_CHIME"),
                SummaryActionService.parse("[SOUND] BLOCK_NOTE_BLOCK_CHIME"));
        assertNull(SummaryActionService.parse("message without a tag"));
    }

    @Test
    void expandsContributorPlaceholdersInLeaderboardOrder() {
        List<String> expanded = SummaryActionService.expand(
                "%player%: %amount% of %total% in %minutes%m",
                List.of(
                        new SummaryActionService.SummaryEntry("Alex", 42L),
                        new SummaryActionService.SummaryEntry("Sam", 7L)
                ),
                Map.of("total", "49", "minutes", "30")
        );

        assertEquals(List.of("Alex: 42 of 49 in 30m", "Sam: 7 of 49 in 30m"), expanded);
    }

    @Test
    void expandsSummaryOnlyPlaceholdersOnce() {
        assertEquals(List.of("Total 1,234"), SummaryActionService.expand(
                "Total %total%",
                List.of(new SummaryActionService.SummaryEntry("Alex", 1_234L)),
                Map.of("total", "1,234", "minutes", "30")));
    }

    @Test
    void convertsConfiguredMinutesToSchedulerUnits() {
        assertEquals(36_000L, HarvestSummaryService.intervalTicks(30));
        assertEquals(1_800_000L, HarvestSummaryService.intervalMillis(30));
    }
}
