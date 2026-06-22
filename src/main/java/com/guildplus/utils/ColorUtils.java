package com.guildplus.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");
    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.legacySection();

    private ColorUtils() {}

    /**
     * &カラーコードおよびHEXカラー (#RRGGBB) を § 形式に変換する。
     */
    public static String colorize(String text) {
        if (text == null) return "";

        // HEXカラー変換 (#RRGGBB -> §x§R§R§G§G§B§B)
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);

        // & -> §
        String result = sb.toString();
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            if (c == '&' && i + 1 < result.length()) {
                char next = result.charAt(i + 1);
                if ("0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(next) >= 0) {
                    out.append('§').append(next);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * カラーコードを除去して純テキストを返す。
     */
    public static String strip(String text) {
        if (text == null) return "";
        return colorize(text).replaceAll("§[0-9A-Fa-fKkLlMmNnOoRrXx]", "");
    }

    /**
     * colorize した文字列を Adventure Component に変換する。
     */
    public static Component toComponent(String text) {
        return SERIALIZER.deserialize(colorize(text));
    }
}
