package dev.zer0.client;

import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.ui.core.Col;
import dev.zer0.client.ui.core.Render;
import net.minecraft.client.gui.DrawContext;

/** Glass replacements for the original HUD boxes' background fill and 1px border. */
public final class GlassHud {
    private GlassHud() {}
    private static final int BOX_BG = 0xA0000000, PLOT_BG = 0x66000000;

    public static void fill(DrawContext g, int x1, int y1, int x2, int y2, int color) {
        if (!Zer0Settings.get().glassHud || (color != BOX_BG && color != PLOT_BG)) { g.fill(x1, y1, x2, y2, color); return; }
        float w = x2 - x1, h = y2 - y1;
        if (color == BOX_BG) {
            float r = Math.min(10, Math.min(w, h) / 3f);
            Render.shadow(g, x1, y1, w, h, r, 5, 0x40000000);
            Render.rect(g, x1, y1, w, h, r, 0xB0121218);
            Render.rectV(g, x1, y1, w, Math.min(h, 18), r, 0x16FFFFFF, 0x00FFFFFF);
        } else {
            Render.rect(g, x1, y1, w, h, Math.min(6, Math.min(w, h) / 3f), 0x50000000);
        }
    }

    public static void outline(DrawContext g, int x, int y, int w, int h, int color) {
        if (!Zer0Settings.get().glassHud) {   // exact original behaviour: 1px border
            g.fill(x, y, x + w, y + 1, color);
            g.fill(x, y + h - 1, x + w, y + h, color);
            g.fill(x, y + 1, x + 1, y + h - 1, color);
            g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
            return;
        }
        float r = Math.min(10, Math.min(w, h) / 3f);
        Render.stroke(g, x, y, w, h, r, 0.6f, Col.mulAlpha(color, 0.75f));
    }
}
