package com.onemillioncrops.model;

import java.util.Map;
import java.util.UUID;

public record ProgressSnapshot(
        Map<String, Long> totals,
        Map<UUID, Map<String, Long>> contributions,
        Map<String, Boolean> completed
) {
}
