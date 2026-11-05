package dev.zer0.client.ui.core;

import dev.zer0.client.config.Zer0Settings;

/** The live colour palette, derived from light/dark, accent and UI transparency. Recomputed every frame (cheap). */
public final class Theme {
    private Theme() {}

    public static boolean dark;
    public static int accent, accentSoft, accentText;
    public static int text, textMuted, textFaint;
    public static int panel, panelEdge, card, cardHover, field, fieldFocus, divider;
    public static int btn, btnHover, btnPress, btnText, btnSelected, btnSelectedText, btnDisabled, btnDisabledText;
    public static int shadow, overlay, good, warn, bad;

    public static void refresh() {
        Zer0Settings s = Zer0Settings.get();
        dark = s.dark;
        float ua = s.uiAlpha;
        accent = Col.withAlpha(s.accent, 255);
        accentSoft = Col.withAlpha(s.accent, 70);
        accentText = Col.readableOn(accent);
        good = 0xFF4ADE80; warn = 0xFFFBBF24; bad = 0xFFF87171;
        if (dark) {
            text = 0xF5FFFFFF; textMuted = 0xA6FFFFFF; textFaint = 0x66FFFFFF;
            panel = Col.argb(Math.round(150 * ua), 18, 18, 24);
            panelEdge = 0x1FFFFFFF;
            card = Col.argb(Math.round(26 * ua + 4), 255, 255, 255);
            cardHover = Col.argb(Math.round(40 * ua + 6), 255, 255, 255);
            field = Col.argb(Math.round(70 * ua + 20), 0, 0, 0);
            fieldFocus = Col.argb(Math.round(100 * ua + 20), 0, 0, 0);
            divider = 0x14FFFFFF;
            // frosted white buttons: translucent white with white text; selected = solid frost with dark text
            btn = Col.argb(Math.round(38 * ua + 6), 255, 255, 255);
            btnHover = Col.argb(Math.round(62 * ua + 8), 255, 255, 255);
            btnPress = Col.argb(Math.round(84 * ua + 8), 255, 255, 255);
            btnText = 0xFFFFFFFF;
            btnSelected = 0xEFFFFFFF; btnSelectedText = 0xFF14141B;
            btnDisabled = 0x12FFFFFF; btnDisabledText = 0x52FFFFFF;
            shadow = 0x50000000; overlay = 0x8C000000;
        } else {
            text = 0xF014141B; textMuted = 0xA314141B; textFaint = 0x6614141B;
            panel = Col.argb(Math.round(175 * ua), 250, 250, 253);
            panelEdge = 0x40FFFFFF;
            card = Col.argb(Math.round(120 * ua + 20), 255, 255, 255);
            cardHover = Col.argb(Math.round(170 * ua + 20), 255, 255, 255);
            field = Col.argb(Math.round(18 * ua + 8), 0, 0, 30);
            fieldFocus = Col.argb(Math.round(28 * ua + 10), 0, 0, 30);
            divider = 0x1414141B;
            btn = Col.argb(Math.round(150 * ua + 30), 255, 255, 255);
            btnHover = Col.argb(Math.round(200 * ua + 30), 255, 255, 255);
            btnPress = Col.argb(255, 240, 240, 246);
            btnText = 0xFF1B1B24;
            btnSelected = 0xFF1B1B24; btnSelectedText = 0xFFFFFFFF;
            btnDisabled = 0x50FFFFFF; btnDisabledText = 0x5014141B;
            shadow = 0x28000020; overlay = 0x66000000;
        }
    }
}
