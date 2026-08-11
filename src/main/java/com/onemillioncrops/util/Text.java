package com.onemillioncrops.util;

import com.onemillioncrops.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Text {
    private static final Pattern GRADIENT_TAG = Pattern.compile("<gradient:([^>]+)>");
    private static final Pattern NUMBER_ARGUMENT = Pattern.compile("-?(?:\\d+(?:\\.\\d*)?|\\.\\d+)");
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final ThreadLocal<NumberFormat> NUMBER = ThreadLocal.withInitial(
            () -> NumberFormat.getIntegerInstance(Locale.US));

    private final ConfigManager config;

    public Text(ConfigManager config) {
        this.config = config;
    }

    public Component parse(String miniMessage) {
        return MINI.deserialize(miniMessage);
    }

    public Component message(String key, Map<String, String> replacements) {
        String raw = config.message("prefix") + config.message(key);
        return parse(replace(raw, replacements));
    }

    public Component rawMessage(String key, Map<String, String> replacements) {
        return parse(replace(config.message(key), replacements));
    }

    public String legacy(String miniMessage) {
        return LEGACY.serialize(parse(miniMessage));
    }

    public static String replace(String input, Map<String, String> replacements) {
        String result = input;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
            // Read legacy configurations without forcing an immediate breaking change.
            result = result.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return result;
    }

    public static String number(long value) {
        return NUMBER.get().format(value);
    }

    public static String percent(long amount, long target) {
        if (target <= 0) {
            return "100.00";
        }
        return String.format(Locale.US, "%.2f", Math.min(100.0, amount * 100.0 / target));
    }

    public static String progressBar(long amount, long target, int width) {
        int full = target <= 0 ? width : (int) Math.min(width, amount * (double) width / target);
        return "<green>" + "■".repeat(full) + "<dark_gray>" + "■".repeat(width - full);
    }

    public static String escape(String input) {
        return MINI.escapeTags(input);
    }

    public static String gradientPhase(String input, double phase) {
        double safePhase = Math.clamp(Double.isFinite(phase) ? phase : 0.0, -1.0, 1.0);
        String formatted = String.format(Locale.US, "%.3f", safePhase);
        if (input.contains("{phase}")) {
            return input.replace("{phase}", formatted);
        }
        Matcher matcher = GRADIENT_TAG.matcher(input);
        if (!matcher.find()) {
            return input;
        }
        String arguments = matcher.group(1);
        int finalSeparator = arguments.lastIndexOf(':');
        String replacementArguments;
        if (finalSeparator >= 0 && NUMBER_ARGUMENT.matcher(arguments.substring(finalSeparator + 1)).matches()) {
            replacementArguments = arguments.substring(0, finalSeparator + 1) + formatted;
        } else {
            replacementArguments = arguments + ":" + formatted;
        }
        return matcher.replaceFirst(Matcher.quoteReplacement("<gradient:" + replacementArguments + ">"));
    }

    /**
     * Renders the first hex gradient directly as individually colored characters. This avoids
     * MiniMessage gradient phase remapping and makes every frame, including the loop boundary,
     * use the same cyclic interpolation path.
     */
    public static String animatedGradient(String input, double progress) {
        double normalized = Double.isFinite(progress) ? progress - Math.floor(progress) : 0.0;
        Matcher matcher = GRADIENT_TAG.matcher(input);
        if (!matcher.find()) {
            return input;
        }

        String[] arguments = matcher.group(1).split(":");
        int colorCount = arguments.length;
        if (colorCount > 0 && NUMBER_ARGUMENT.matcher(arguments[colorCount - 1]).matches()) {
            colorCount--;
        }
        if (colorCount < 2) {
            return gradientPhase(input, -1.0 + normalized * 2.0);
        }

        List<Rgb> colors = new ArrayList<>(colorCount);
        for (int index = 0; index < colorCount; index++) {
            Rgb color = Rgb.parse(arguments[index]);
            if (color == null) {
                return gradientPhase(input, -1.0 + normalized * 2.0);
            }
            colors.add(color);
        }

        int closingTag = input.indexOf("</gradient>", matcher.end());
        if (closingTag < 0) {
            return input;
        }
        String content = input.substring(matcher.end(), closingTag);
        int characters = visibleCharacters(content);
        if (characters == 0) {
            return input;
        }

        StringBuilder rendered = new StringBuilder(content.length() * 4);
        int characterIndex = 0;
        for (int offset = 0; offset < content.length();) {
            if (content.charAt(offset) == '<') {
                int tagEnd = content.indexOf('>', offset);
                if (tagEnd >= 0) {
                    rendered.append(content, offset, tagEnd + 1);
                    offset = tagEnd + 1;
                    continue;
                }
            }
            int codePoint = content.codePointAt(offset);
            double spread = characters <= 1 ? 0.0
                    : characterIndex * (colors.size() - 1.0) / (characters - 1.0);
            Rgb color = sample(colors, normalized * colors.size() + spread);
            rendered.append("<color:").append(color.hex()).append('>')
                    .appendCodePoint(codePoint).append("</color>");
            characterIndex++;
            offset += Character.charCount(codePoint);
        }
        return input.substring(0, matcher.start()) + rendered
                + input.substring(closingTag + "</gradient>".length());
    }

    private static int visibleCharacters(String input) {
        int count = 0;
        for (int offset = 0; offset < input.length();) {
            if (input.charAt(offset) == '<') {
                int tagEnd = input.indexOf('>', offset);
                if (tagEnd >= 0) {
                    offset = tagEnd + 1;
                    continue;
                }
            }
            int codePoint = input.codePointAt(offset);
            count++;
            offset += Character.charCount(codePoint);
        }
        return count;
    }

    private static Rgb sample(List<Rgb> colors, double position) {
        double wrapped = position - Math.floor(position / colors.size()) * colors.size();
        int currentIndex = (int) Math.floor(wrapped);
        double blend = wrapped - currentIndex;
        Rgb current = colors.get(currentIndex);
        Rgb next = colors.get((currentIndex + 1) % colors.size());
        return current.interpolate(next, blend);
    }

    private record Rgb(int red, int green, int blue) {
        private static Rgb parse(String configured) {
            String hex = configured.startsWith("#") ? configured.substring(1) : configured;
            if (!hex.matches("[0-9a-fA-F]{6}")) {
                return null;
            }
            return new Rgb(
                    Integer.parseInt(hex.substring(0, 2), 16),
                    Integer.parseInt(hex.substring(2, 4), 16),
                    Integer.parseInt(hex.substring(4, 6), 16)
            );
        }

        private Rgb interpolate(Rgb other, double amount) {
            return new Rgb(
                    channel(red, other.red, amount),
                    channel(green, other.green, amount),
                    channel(blue, other.blue, amount)
            );
        }

        private String hex() {
            return String.format(Locale.ROOT, "#%02X%02X%02X", red, green, blue);
        }

        private static int channel(int start, int end, double amount) {
            return (int) Math.round(start + (end - start) * amount);
        }
    }
}
