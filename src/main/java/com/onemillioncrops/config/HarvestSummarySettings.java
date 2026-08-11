package com.onemillioncrops.config;

import java.util.List;

public record HarvestSummarySettings(
        boolean enabled,
        int intervalMinutes,
        List<String> actions
) {
}
