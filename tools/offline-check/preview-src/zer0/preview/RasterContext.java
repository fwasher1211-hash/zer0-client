package dev.zer0.preview;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.Window;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;

/** Software DrawContext: rasterises exactly the calls ZER0 makes, at a real GUI scale, to verify the look offline. */
public class RasterContext extends DrawContext {
    public final BufferedImage img;
    final int S = Window.SCALE;
    final ArrayDeque<int[]> sc = new ArrayDeque<>();
    public RasterContext(int w, int h) { img = new BufferedImage(w * Window.SCALE, h * Window.SCALE, BufferedImage.TYPE_INT_ARGB); }

    float X(float x, float y) { return (m.a * x + m.c * y + m.tx) * S; }
    float Y(float x, float y) { return (m.b * x + m.d * y + m.ty) * S; }

    private int[] clip() { return sc.isEmpty() ? new int[]{0, 0, img.getWidth(), img.getHeight()} : sc.peek(); }
    private void blend(int px, int py, int c) {
        int[] cl = clip();
        if (px < cl[0] || py < cl[1] || px >= cl[2] || py >= cl[3]) return;
        int a = c >>> 24; if (a == 0) return;
        int d = img.getRGB(px, py);
        float fa = a / 255f, da = (d >>> 24) / 255f, oa = fa + da * (1 - fa);
        int r = Math.round((((c >> 16) & 255) * fa + ((d >> 16) & 255) * da * (1 - fa)) / Math.max(oa, 1e-6f));
        int g = Math.round((((c >> 8) & 255) * fa + ((d >> 8) & 255) * da * (1 - fa)) / Math.max(oa, 1e-6f));
        int b = Math.round(((c & 255) * fa + (d & 255) * da * (1 - fa)) / Math.max(oa, 1e-6f));
        img.setRGB(px, py, (Math.round(oa * 255) << 24) | (r << 16) | (g << 8) | b);
    }
    @Override public void fill(int x1, int y1, int x2, int y2, int col) { fillGradient(x1, y1, x2, y2, col, col); }
    @Override public void fillGradient(int x1, int y1, int x2, int y2, int c1, int c2) {
        int px1 = Math.round(X(x1, y1)), py1 = Math.round(Y(x1, y1)), px2 = Math.round(X(x2, y2)), py2 = Math.round(Y(x2, y2));
        if (px2 < px1) { int t = px1; px1 = px2; px2 = t; } if (py2 < py1) { int t = py1; py1 = py2; py2 = t; }
        for (int y = Math.max(0, py1); y < Math.min(img.getHeight(), py2); y++) {
            float t = py2 - py1 <= 1 ? 0 : (y - py1) / (float) (py2 - py1 - 1);
            int c = c1 == c2 ? c1 : mix(c1, c2, t);
            for (int x = Math.max(0, px1); x < Math.min(img.getWidth(), px2); x++) blend(x, y, c);
        }
    }
    static int mix(int a, int b, float t) {
        int r = 0; for (int s = 0; s < 32; s += 8) { int ca = (a >>> s) & 255, cb = (b >>> s) & 255; r |= Math.round(ca + (cb - ca) * t) << s; } return r;
    }
    @Override public void enableScissor(int a, int b, int c, int d) {
        int[] n = {Math.round(X(a, b)), Math.round(Y(a, b)), Math.round(X(c, d)), Math.round(Y(c, d))};
        if (!sc.isEmpty()) { int[] p = sc.peek(); n = new int[]{Math.max(n[0], p[0]), Math.max(n[1], p[1]), Math.min(n[2], p[2]), Math.min(n[3], p[3])}; }
        sc.push(n);
    }
    @Override public void disableScissor() { sc.pop(); }

    @Override public void drawText(TextRenderer t, Text s, int x, int y, int c, boolean sh) {
        MutableText mt = (MutableText) s;
        float px = 10 * m.a * S;
        Graphics2D g = gfx();
        g.setFont(TextRenderer.font(mt.style.bold, px));
        g.setColor(new Color(c, true));
        g.drawString(mt.s, X(x, y), Y(x, y) + px * 0.82f + m.d * S * 0.5f);
        g.dispose();
    }
    @Override public void drawText(TextRenderer t, String s, int x, int y, int c, boolean sh) { drawText(t, Text.literal(s), x, y, c, sh); }

    @Override public void drawTexture(com.mojang.blaze3d.pipeline.RenderPipeline p, Identifier id, int x, int y, float u, float v, int w, int h, int rw, int rh, int tw, int th, int color) {
        var tex = MinecraftClient.TM.map.get(id);
        if (!(tex instanceof NativeImageBackedTexture n) || n.image.img == null) return;
        int dx = Math.round(X(x, y)), dy = Math.round(Y(x, y)), dw = Math.round(X(x + w, y + h)) - dx, dh = Math.round(Y(x + w, y + h)) - dy;
        BufferedImage src = n.image.img;
        float alpha = (color >>> 24) / 255f;
        Graphics2D g = gfx();
        int[] cl = clip();
        g.setClip(cl[0], cl[1], cl[2] - cl[0], cl[3] - cl[1]);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);  // like Minecraft's default sampler
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.drawImage(src, dx, dy, dx + dw, dy + dh, 0, 0, src.getWidth(), src.getHeight(), null);
        g.dispose();
    }
    private Graphics2D gfx() {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        int[] cl = clip(); g.setClip(cl[0], cl[1], cl[2] - cl[0], cl[3] - cl[1]);
        return g;
    }
}
