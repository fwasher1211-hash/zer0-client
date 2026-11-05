package dev.zer0.client.ui.core;

import dev.zer0.client.compat.Mc;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

/**
 * Anti-aliased vector primitives drawn at the screen's real pixel density.
 *
 * Minecraft's fill() works in GUI units (2-4 physical pixels each), which is what makes vanilla corners
 * look blocky. These helpers temporarily scale the matrix so one unit = one physical pixel, then build
 * rounded shapes from scanline spans with a fractional-alpha edge pixel. Result: smooth corners at every
 * GUI scale, using only the core fill() call (no shaders, so it works with any graphics setup).
 */
public final class Render {
    private Render() {}

    /** Physical pixels per GUI unit at the current matrix (accounts for HUD scaling). */
    public static float density(DrawContext g) {
        Matrix3x2fStack m = g.getMatrices();
        float s = (float) Math.sqrt(m.m00() * m.m00() + m.m01() * m.m01());
        return (float) (Mc.guiScale() * (s <= 0 ? 1 : s));
    }

    private static float begin(DrawContext g) {
        float d = density(g);
        Matrix3x2fStack m = g.getMatrices();
        m.pushMatrix();
        m.scale(1f / d, 1f / d);
        return d;
    }
    private static void end(DrawContext g) { g.getMatrices().popMatrix(); }

    // ------------------------------------------------------------------ rounded rectangles
    public static void rect(DrawContext g, float x, float y, float w, float h, float r, int color) {
        if (w <= 0 || h <= 0 || Col.a(color) == 0) return;
        float d = begin(g);
        rectPx(g, Math.round(x * d), Math.round(y * d), Math.round((x + w) * d), Math.round((y + h) * d), r * d, color, color);
        end(g);
    }

    /** Vertical gradient (top -> bottom) with rounded corners. */
    public static void rectV(DrawContext g, float x, float y, float w, float h, float r, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        float d = begin(g);
        rectPx(g, Math.round(x * d), Math.round(y * d), Math.round((x + w) * d), Math.round((y + h) * d), r * d, top, bottom);
        end(g);
    }

    private static void rectPx(DrawContext g, int x1, int y1, int x2, int y2, float R, int top, int bottom) {
        int W = x2 - x1, H = y2 - y1;
        if (W <= 0 || H <= 0) return;
        R = Math.max(0, Math.min(R, Math.min(W, H) / 2f));
        int rows = Math.min((int) Math.ceil(R), H / 2);
        boolean grad = top != bottom;
        for (int j = 0; j < rows; j++) {
            float dy = R - (j + 0.5f);
            float dx = (float) Math.sqrt(Math.max(0, R * R - dy * dy));
            float edge = R - dx;                       // inset of the curve on this row
            int full = (int) Math.ceil(edge);
            float cover = full - edge;                 // coverage of the partial pixel
            int ct = grad ? Col.mix(top, bottom, (j + 0.5f) / H) : top;
            int cb = grad ? Col.mix(top, bottom, (H - j - 0.5f) / H) : top;
            span(g, x1, x2, y1 + j, full, cover, ct);
            span(g, x1, x2, y2 - 1 - j, full, cover, cb);
        }
        if (H - 2 * rows > 0) {
            if (grad) g.fillGradient(x1, y1 + rows, x2, y2 - rows, Col.mix(top, bottom, (float) rows / H), Col.mix(top, bottom, (float) (H - rows) / H));
            else g.fill(x1, y1 + rows, x2, y2 - rows, top);
        }
    }

    private static void span(DrawContext g, int x1, int x2, int y, int inset, float cover, int c) {
        if (x2 - inset > x1 + inset) g.fill(x1 + inset, y, x2 - inset, y + 1, c);
        if (cover > 0.02f && inset > 0) {
            int ec = Col.mulAlpha(c, cover);
            g.fill(x1 + inset - 1, y, x1 + inset, y + 1, ec);
            g.fill(x2 - inset, y, x2 - inset + 1, y + 1, ec);
        }
    }

    /** Hairline (or thicker) rounded outline. */
    public static void stroke(DrawContext g, float x, float y, float w, float h, float r, float thickness, int color) {
        if (w <= 0 || h <= 0 || Col.a(color) == 0) return;
        float d = begin(g);
        int x1 = Math.round(x * d), y1 = Math.round(y * d), x2 = Math.round((x + w) * d), y2 = Math.round((y + h) * d);
        int W = x2 - x1, H = y2 - y1;
        float R = Math.max(0, Math.min(r * d, Math.min(W, H) / 2f));
        int t = Math.max(1, Math.round(thickness * d));
        float Ri = Math.max(0, R - t);
        for (int j = 0; j < H; j++) {
            int yy = y1 + j;
            int jj = Math.min(j, H - 1 - j);          // distance from nearest horizontal edge
            float outer = insetAt(R, jj);
            if (jj < t) {                              // top / bottom band: full span
                spanAA(g, x1, x2, yy, outer, color);
            } else {
                float inner = t + insetAt(Ri, jj - t);
                if (jj >= R && jj - t >= Ri && j > 0 && j < H - 1) {   // straight vertical walls
                    g.fill(x1, yy, x1 + t, yy + 1, color);
                    g.fill(x2 - t, yy, x2, yy + 1, color);
                } else {
                    segAA(g, x1, yy, outer, inner, color, false);
                    segAA(g, x2, yy, outer, inner, color, true);
                }
            }
        }
        end(g);
    }
    private static float insetAt(float R, int row) {
        if (row >= R) return 0;
        float dy = R - (row + 0.5f);
        return R - (float) Math.sqrt(Math.max(0, R * R - dy * dy));
    }
    private static void spanAA(DrawContext g, int x1, int x2, int y, float inset, int c) {
        int full = (int) Math.ceil(inset);
        span(g, x1, x2, y, full, full - inset, c);
    }
    /** Fills from {@code outer} to {@code inner} inset measured from edge x (left) or back from x (right). */
    private static void segAA(DrawContext g, int x, int y, float outer, float inner, int c, boolean right) {
        int a = (int) Math.ceil(outer), b = Math.max(a, (int) Math.floor(inner));
        float cover = a - outer;
        if (!right) {
            if (b > a) g.fill(x + a, y, x + b, y + 1, c);
            if (cover > 0.02f && a > 0) g.fill(x + a - 1, y, x + a, y + 1, Col.mulAlpha(c, cover));
        } else {
            if (b > a) g.fill(x - b, y, x - a, y + 1, c);
            if (cover > 0.02f && a > 0) g.fill(x - a, y, x - a + 1, y + 1, Col.mulAlpha(c, cover));
        }
    }

    public static void circle(DrawContext g, float cx, float cy, float radius, int color) {
        rect(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    /** Soft drop shadow made of a few expanding, fading rounded rects. */
    public static void shadow(DrawContext g, float x, float y, float w, float h, float r, float size, int color) {
        if (Col.a(color) == 0) return;
        int layers = 4;
        for (int i = layers; i >= 1; i--) {
            float e = size * i / layers;
            float f = 1f - (float) i / (layers + 1);
            rect(g, x - e, y - e * 0.6f + size * 0.35f, w + 2 * e, h + 2 * e, r + e, Col.mulAlpha(color, f * f * 0.55f));
        }
    }

    /** Frosted-glass panel: optional shadow, translucent body, faint top sheen and hairline edge. */
    public static void glass(DrawContext g, float x, float y, float w, float h, float r, int body, int edge, boolean shadow) {
        if (shadow) shadow(g, x, y, w, h, r, 10, Theme.shadow);
        rect(g, x, y, w, h, r, body);
        rectV(g, x, y, w, Math.min(h, 22), r, Col.withAlpha(0xFFFFFF, Theme.dark ? 14 : 40), Col.withAlpha(0xFFFFFF, 0));
        stroke(g, x, y, w, h, r, 0.5f, edge);
    }

    // ------------------------------------------------------------------ charts
    /** Smooth area chart of {@code ys} (null/short-safe). Values are auto-ranged unless min==max. */
    public static void areaChart(DrawContext g, float x, float y, float w, float h, double[] ys, int line, boolean fill) {
        if (ys == null || ys.length < 2 || w < 4 || h < 4) return;
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (double v : ys) { lo = Math.min(lo, v); hi = Math.max(hi, v); }
        if (hi - lo < 1e-9) { hi += 1; lo -= 1; }
        float d = begin(g);
        int X1 = Math.round(x * d), Y1 = Math.round(y * d), X2 = Math.round((x + w) * d), Y2 = Math.round((y + h) * d);
        int W = X2 - X1, H = Y2 - Y1, step = d >= 3 ? 2 : 1;
        int thick = Math.max(2, Math.round(d * 0.8f));
        float prevY = Float.NaN;
        for (int px = 0; px < W; px += step) {
            double f = (double) px / Math.max(1, W - 1) * (ys.length - 1);
            int i = Math.min(ys.length - 2, (int) f);
            double t = f - i, s = t * t * (3 - 2 * t);
            double v = ys[i] + (ys[i + 1] - ys[i]) * s;
            float py = (float) (Y2 - (v - lo) / (hi - lo) * (H - thick) - thick / 2f);
            int cx = X1 + px;
            if (fill) g.fillGradient(cx, Math.round(py), cx + step, Y2, Col.mulAlpha(line, 0.32f), Col.mulAlpha(line, 0.0f));
            float a = Float.isNaN(prevY) ? py : prevY;
            int top = Math.round(Math.min(a, py) - thick / 2f), bot = Math.round(Math.max(a, py) + thick / 2f);
            g.fill(cx, top, cx + step, Math.max(top + 1, bot), line);
            prevY = py;
        }
        end(g);
    }

    /** Thin rounded progress bar. */
    public static void bar(DrawContext g, float x, float y, float w, float h, float t, int track, int fillC) {
        rect(g, x, y, w, h, h / 2, track);
        t = Anim.clamp01(t);
        if (t > 0) rect(g, x, y, Math.max(h, w * t), h, h / 2, fillC);
    }

    // ------------------------------------------------------------------ icons
    /** Anti-aliased right-pointing (or left-pointing) triangle, e.g. play / skip icons. */
    public static void triangle(DrawContext g, float x, float y, float w, float h, boolean right, int color) {
        float d = begin(g);
        float X = x * d, Y = y * d, W = w * d, H = h * d;
        int y0 = Math.round(Y), y1 = Math.round(Y + H);
        for (int py = y0; py < y1; py++) {
            float t = (py + 0.5f - Y) / H;
            float len = W * (1 - Math.abs(2 * t - 1));
            int full = (int) Math.floor(len);
            float frac = len - full;
            if (right) {
                int sx = Math.round(X);
                if (full > 0) g.fill(sx, py, sx + full, py + 1, color);
                if (frac > 0.02f) g.fill(sx + full, py, sx + full + 1, py + 1, Col.mulAlpha(color, frac));
            } else {
                int ex = Math.round(X + W);
                if (full > 0) g.fill(ex - full, py, ex, py + 1, color);
                if (frac > 0.02f) g.fill(ex - full - 1, py, ex - full, py + 1, Col.mulAlpha(color, frac));
            }
        }
        end(g);
    }
}
