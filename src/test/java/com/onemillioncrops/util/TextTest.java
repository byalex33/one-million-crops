package com.onemillioncrops.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextTest {
    @Test
    void rendersProgressBarWithoutOverflow() {
        assertEquals("<green>■■■■■■■■■■<dark_gray>■■■■■■■■■■",
                Text.progressBar(Long.MAX_VALUE / 2, Long.MAX_VALUE, 20));
        assertEquals("<green>■■■■■■■■■■■■■■■■■■■■<dark_gray>",
                Text.progressBar(Long.MAX_VALUE, Long.MAX_VALUE, 20));
    }

    @Test
    void escapesUserSuppliedMiniMessageTags() {
        assertEquals("\\<click:run_command:/op me>crop\\</click>",
                Text.escape("<click:run_command:/op me>crop</click>"));
    }

    @Test
    void addsAndReplacesGradientAnimationPhases() {
        assertEquals("<gradient:#55ff55:#ffd54a:-0.500>Crops</gradient>",
                Text.gradientPhase("<gradient:#55ff55:#ffd54a>Crops</gradient>", -0.5));
        assertEquals("<gradient:red:blue:1.000>Crops</gradient>",
                Text.gradientPhase("<gradient:red:blue:0.2>Crops</gradient>", 2));
        assertEquals("<green>Crops</green>",
                Text.gradientPhase("<green>Crops</green>", 0.25));
    }

    @Test
    void replacesPercentPlaceholdersAndReadsLegacyPlaceholders() {
        assertEquals("<green>42</green>", Text.replace("<green>%amount%</green>", Map.of("amount", "42")));
        assertEquals("<green>42</green>", Text.replace("<green><amount></green>", Map.of("amount", "42")));
    }

    @Test
    void rotatesHexGradientStopsInASeamlessLoop() {
        String gradient = "<b><gradient:#FF0000:#00FF00>CROPS</gradient></b>";
        String start = "<b><color:#FF0000>C</color><color:#BF4000>R</color>"
                + "<color:#808000>O</color><color:#40BF00>P</color><color:#00FF00>S</color></b>";

        assertEquals(start, Text.animatedGradient(gradient, 0.0));
        assertEquals("<b><color:#00FF00>C</color><color:#40BF00>R</color>"
                        + "<color:#808000>O</color><color:#BF4000>P</color><color:#FF0000>S</color></b>",
                Text.animatedGradient(gradient, 0.5));
        assertEquals(start, Text.animatedGradient(gradient, 1.0));
    }

    @Test
    void interpolatesBetweenRainbowStopsInsteadOfJumping() {
        assertEquals("<color:#808000>A</color><color:#808000>B</color>",
                Text.animatedGradient("<gradient:#FF0000:#00FF00>AB</gradient>", 0.25));
        assertEquals("<bold>CROPS</bold>", Text.animatedGradient("<bold>CROPS</bold>", 0.5));
    }

    @Test
    void finalRainbowFrameApproachesFirstFrameThroughTheSameColorPath() {
        String rainbow = "<gradient:#FF4B4B:#FF8B2D:#FFD031:#DCFF39:#6DFF45:#3BFFC7:#58D0FF:#5489FF>C</gradient>";

        assertEquals("<color:#F44F57>C</color>", Text.animatedGradient(rainbow, 119.0 / 120.0));
        assertEquals("<color:#FF4B4B>C</color>", Text.animatedGradient(rainbow, 0.0));
    }
}
