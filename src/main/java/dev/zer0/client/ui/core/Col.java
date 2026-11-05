package dev.zer0.client.ui.core;

/** ARGB colour helpers. Minecraft 1.21.6+ treats alpha 0 as invisible, so every colour here carries alpha. */
public final class Col {
    private Col() {}
    public static int argb(int a, int r, int g, int b) { return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b); }
    public static int rgb(int rgb, float alpha) { return (clamp(Math.round(alpha * 255)) << 24) | (rgb & 0xFFFFFF); }
    public static int a(int c) { return c >>> 24; }
    public static int r(int c) { return (c >> 16) & 255; }
    public static int g(int c) { return (c >> 8) & 255; }
    public static int b(int c) { return c & 255; }
    public static int mulAlpha(int c, float m) { return (clamp(Math.round(a(c) * m)) << 24) | (c & 0xFFFFFF); }
    public static int withAlpha(int c, int a) { return (clamp(a) << 24) | (c & 0xFFFFFF); }
    public static int mix(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        return argb(Math.round(a(c1) + (a(c2) - a(c1)) * t), Math.round(r(c1) + (r(c2) - r(c1)) * t),
                Math.round(g(c1) + (g(c2) - g(c1)) * t), Math.round(b(c1) + (b(c2) - b(c1)) * t));
    }
    public static int brighten(int c, float f) {
        return argb(a(c), Math.round(r(c) * f), Math.round(g(c) * f), Math.round(b(c) * f));
    }
    public static float luminance(int c) { return (0.2126f * r(c) + 0.7152f * g(c) + 0.0722f * b(c)) / 255f; }
    public static int readableOn(int bg) { return luminance(bg) > 0.6f ? 0xFF15151C : 0xFFFFFFFF; }
    private static int clamp(int v) { return v < 0 ? 0 : v > 255 ? 255 : v; }

    public static int hsv(float h, float s, float v) {
        h = ((h % 1f) + 1f) % 1f;
        int i = (int) (h * 6); float f = h * 6 - i;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i % 6) { case 0 -> { r = v; g = t; b = p; } case 1 -> { r = q; g = v; b = p; } case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; } case 4 -> { r = t; g = p; b = v; } default -> { r = v; g = p; b = q; } }
        return argb(255, Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
    }
    public static String hex(int rgb) { return String.format("%06X", rgb & 0xFFFFFF); }
    /** Parses "RRGGBB" / "#RRGGBB"; returns -1 when invalid. */
    public static int parseHex(String s) {
        if (s == null) return -1;
        s = s.trim(); if (s.startsWith("#")) s = s.substring(1);
        if (!s.matches("[0-9a-fA-F]{6}")) return -1;
        return Integer.parseInt(s, 16);
    }
}
