package dev.zer0.client.spotify;

import dev.zer0.client.compat.Mc;
import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.ui.core.*;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

/**
 * The Spotify player: a frosted-glass now-playing card in three layouts (Card, Compact, Cover).
 * The same drawing is used on the HUD, the title screen and the Spotify page; controls are clickable
 * wherever the mouse is free (title screen, hub, chat, HUD editor).
 */
public final class SpotifyHud {
    private SpotifyHud() {}

    public enum Hit { NONE, PREV, TOGGLE, NEXT, VOLUME }
    private static final Anim.Smooth appear = new Anim.Smooth(0, 8), playIcon = new Anim.Smooth(0, 14);
    private static String lastTitle = ""; private static double titleChanged;

    static Zer0Settings.Spotify s() { return Zer0Settings.get().spotify; }

    /** Unscaled size of the current layout. */
    public static float[] base() {
        return switch (s().layout) {
            case "compact" -> new float[]{200, 30};
            case "cover" -> new float[]{124, 172};
            default -> new float[]{232, 72};
        };
    }
    /** On-screen size (GUI px) including the user's scale. */
    public static float[] size() { float[] b = base(); return new float[]{b[0] * s().scale, b[1] * s().scale}; }

    public static boolean visibleNow() {
        SpotifyClient c = SpotifyClient.get();
        if (!s().hudEnabled || c.state() != SpotifyClient.State.CONNECTED) return false;
        return c.track() != null || !s().hideWhenIdle;
    }
    private static int accent() { return s().useThemeAccent ? Theme.accent : (0xFF000000 | s().accent); }

    /** HUD entry point (Fabric HUD element). */
    public static void renderHud(DrawContext g) {
        float a = appear.to(visibleNow() ? 1 : 0);
        if (a < 0.01f) return;
        Theme.refresh();
        draw(g, s().x, s().y, s().scale, s().alpha * a, -9999, -9999);
    }

    /** Draws the player with its top-left at (x, y). */
    public static void draw(DrawContext g, float x, float y, float scale, float alpha, float mx, float my) {
        Matrix3x2fStack m = g.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        float lmx = (mx - x) / scale, lmy = (my - y) / scale;
        switch (s().layout) {
            case "compact" -> compact(g, alpha, lmx, lmy);
            case "cover" -> cover(g, alpha, lmx, lmy);
            default -> card(g, alpha, lmx, lmy);
        }
        m.popMatrix();
    }

    // ---------------------------------------------------------------- layouts
    private static void body(DrawContext g, float w, float h, float r, float alpha) {
        int b = Theme.dark ? 0x00141419 : 0x00F6F7FB;
        Render.shadow(g, 0, 0, w, h, r, 8, Col.mulAlpha(0x60000000, alpha));
        Render.rect(g, 0, 0, w, h, r, Col.withAlpha(b, Math.round(205 * alpha)));
        Render.rectV(g, 0, 0, w, Math.min(h, 24), r, Col.withAlpha(0xFFFFFF, Math.round(16 * alpha)), 0x00FFFFFF);
        Render.stroke(g, 0, 0, w, h, r, 0.5f, Col.withAlpha(0xFFFFFF, Math.round(34 * alpha)));
    }

    private static void card(DrawContext g, float alpha, float mx, float my) {
        float W = 232, H = 72;
        body(g, W, H, 16, alpha);
        SpotifyClient.Track t = SpotifyClient.get().track();
        float tx = 10;
        if (s().showArt) { art(g, 10, 10, 52, alpha); tx = 70; }
        float right = W - 10;
        if (s().showLogo) logo(g, W - 17, 13, 5.5f, alpha);
        int text = Col.mulAlpha(Theme.text, alpha), muted = Col.mulAlpha(Theme.textMuted, alpha);
        float ty = 11;
        if (t == null) {
            Fonts.draw(g, Fonts.Role.HUD, "Nothing playing", tx, ty + 6, text, 9f);
            Fonts.draw(g, Fonts.Role.HUD, Fonts.fit(Fonts.Role.HUD, SpotifyClient.get().message(), 7f, right - tx - 14), tx, ty + 20, muted, 7f);
            return;
        }
        if (s().showTitle) { marquee(g, t.title(), tx, ty, right - tx - (s().showLogo ? 14 : 0), 9f, text, true); ty += Fonts.px(Fonts.Role.HUD, 9f) + 3; }
        if (s().showArtist) { Fonts.draw(g, Fonts.Role.HUD, Fonts.fit(Fonts.Role.HUD, t.artists(), 7.4f, right - tx), tx, ty, muted, 7.4f); }
        float by = H - 22;
        if (s().showProgress) progress(g, tx, H - 30, right - tx, t, alpha, true);
        if (s().showControls) controls(g, tx, by + 1, alpha, mx, my, t.playing());
        if (s().showVolume && t.supportsVolume() && t.volume() >= 0) volume(g, right - 54, by + 7, 54, t.volume(), alpha, mx, my);
    }

    private static void compact(DrawContext g, float alpha, float mx, float my) {
        float W = 200, H = 30;
        body(g, W, H, 15, alpha);
        SpotifyClient.Track t = SpotifyClient.get().track();
        float tx = 9;
        if (s().showArt) { art(g, 5, 5, 20, alpha); tx = 31; }
        int text = Col.mulAlpha(Theme.text, alpha);
        float ctrlW = s().showControls ? 68 : 0, logoW = s().showLogo ? 16 : 0;
        String line = t == null ? "Nothing playing" : (s().showTitle ? t.title() : "") + (s().showArtist && s().showTitle ? "  ·  " : "") + (s().showArtist ? t.artists() : "");
        marquee(g, line, tx, (H - Fonts.px(Fonts.Role.HUD, 7.8f)) / 2f, W - tx - ctrlW - logoW - 8, 7.8f, text, false);
        if (t != null && s().showProgress) {
            float p = t.durationMs() > 0 ? (float) t.progressNow() / t.durationMs() : 0;
            Render.rect(g, 14, H - 2.2f, (W - 28) * p, 1.4f, 0.7f, Col.mulAlpha(accent(), alpha));
        }
        if (t != null && s().showControls) controls(g, W - ctrlW - logoW - 4, 7, alpha, mx, my, t.playing());
        if (s().showLogo) logo(g, W - 14, H / 2f, 5f, alpha);
    }

    private static void cover(DrawContext g, float alpha, float mx, float my) {
        float W = 124, H = 172;
        body(g, W, H, 18, alpha);
        SpotifyClient.Track t = SpotifyClient.get().track();
        if (s().showArt) art(g, 10, 10, 104, alpha);
        if (s().showLogo) logo(g, W - 20, 20, 6f, alpha);
        int text = Col.mulAlpha(Theme.text, alpha), muted = Col.mulAlpha(Theme.textMuted, alpha);
        float ty = s().showArt ? 120 : 14;
        if (t == null) { Fonts.drawCentered(g, Fonts.Role.HUD, "Nothing playing", W / 2, ty + 6, text, 8f); return; }
        if (s().showTitle) { marquee(g, t.title(), 10, ty, W - 20, 8.4f, text, true); ty += Fonts.px(Fonts.Role.HUD, 8.4f) + 2; }
        if (s().showArtist) Fonts.draw(g, Fonts.Role.HUD, Fonts.fit(Fonts.Role.HUD, t.artists(), 7f, W - 20), 10, ty, muted, 7f);
        if (s().showProgress) progress(g, 10, H - 26, W - 20, t, alpha, false);
        if (s().showControls) controls(g, (W - 64) / 2f, H - 21, alpha, mx, my, t.playing());
    }

    // ---------------------------------------------------------------- pieces
    private static void art(DrawContext g, float x, float y, float size, float alpha) {
        if (!SpotifyArt.hasArt()) {
            Render.rect(g, x, y, size, size, size * 0.11f, Col.withAlpha(0xFFFFFF, Math.round(24 * alpha)));
            logo(g, x + size / 2, y + size / 2, size * 0.16f, alpha * 0.6f);
            return;
        }
        float f = SpotifyArt.fade();
        int need = Math.round(size * Render.density(g));            // exact physical pixels on screen
        if (SpotifyArt.hasPrevious() && f < 1) {
            SpotifyArt.Tex p = SpotifyArt.get(true, need);
            if (p != null) Mc.drawImage(g, p.id(), Math.round(x), Math.round(y), Math.round(size), Math.round(size), p.px(), p.px(), Col.withAlpha(0xFFFFFF, Math.round(255 * alpha)));
        }
        SpotifyArt.Tex t = SpotifyArt.get(false, need);
        if (t == null) return;
        Mc.drawImage(g, t.id(), Math.round(x), Math.round(y), Math.round(size), Math.round(size), t.px(), t.px(),
                Col.withAlpha(0xFFFFFF, Math.round(255 * alpha * (SpotifyArt.hasPrevious() ? f : 1))));
    }

    /** Spotify mark (green disc with three sound-wave arcs) - shown for attribution as Spotify's design guidelines ask. */
    private static void logo(DrawContext g, float cx, float cy, float r, float alpha) {
        Render.circle(g, cx, cy, r, Col.withAlpha(0x1ED760, Math.round(255 * alpha)));
        int w = Col.withAlpha(0x0B0B0B, Math.round(235 * alpha));
        float[][] arcs = {{-0.33f, 0.62f, 0.17f}, {0.03f, 0.50f, 0.14f}, {0.34f, 0.38f, 0.12f}};
        for (float[] a : arcs) {
            float ay = cy + a[0] * r, half = a[1] * r, th = Math.max(0.55f, a[2] * r);
            int steps = 10;
            for (int i = 0; i <= steps; i++) {
                float t = -1 + 2f * i / steps;
                float px = cx + t * half, py = ay - (1 - t * t) * r * 0.13f + (a[0] < 0 ? 0 : 0);
                Render.circle(g, px, py, th / 2, w);
            }
        }
    }

    private static void progress(DrawContext g, float x, float y, float w, SpotifyClient.Track t, float alpha, boolean times) {
        float p = t.durationMs() > 0 ? (float) t.progressNow() / t.durationMs() : 0;
        Render.bar(g, x, y, w, 3, p, Col.withAlpha(0xFFFFFF, Math.round(38 * alpha)), Col.mulAlpha(accent(), alpha));
    }
    static String mmss(long ms) { long s = ms / 1000; return (s / 60) + ":" + String.format("%02d", s % 60); }

    private static void controls(DrawContext g, float x, float y, float alpha, float mx, float my, boolean playing) {
        int c = Col.mulAlpha(Theme.text, alpha);
        float p = playIcon.to(playing ? 1 : 0);
        // prev
        hoverDot(g, x + 8, y + 8, mx, my, alpha);
        Render.rect(g, x + 3.5f, y + 4.5f, 1.4f, 7, 0.7f, c);
        Render.triangle(g, x + 5.2f, y + 4.5f, 6.5f, 7, false, c);
        // play / pause (morphs)
        float px = x + 24;
        Render.circle(g, px + 8, y + 8, 8.5f + (over(px + 8, y + 8, 9, mx, my) ? 0.6f : 0), Col.mulAlpha(Theme.dark ? 0xF0FFFFFF : 0xFF1B1B24, alpha));
        int ic = Col.mulAlpha(Theme.dark ? 0xFF111116 : 0xFFFFFFFF, alpha);
        if (p > 0.5f) { Render.rect(g, px + 4.8f, y + 4.5f, 2.2f, 7, 1.1f, ic); Render.rect(g, px + 9f, y + 4.5f, 2.2f, 7, 1.1f, ic); }
        else Render.triangle(g, px + 5.8f, y + 4.2f, 6.6f, 7.6f, true, ic);
        // next
        float nx = x + 48;
        hoverDot(g, nx + 8, y + 8, mx, my, alpha);
        Render.triangle(g, nx + 4.2f, y + 4.5f, 6.5f, 7, true, c);
        Render.rect(g, nx + 11.1f, y + 4.5f, 1.4f, 7, 0.7f, c);
    }
    private static void hoverDot(DrawContext g, float cx, float cy, float mx, float my, float alpha) {
        if (over(cx, cy, 8, mx, my)) Render.circle(g, cx, cy, 8, Col.withAlpha(0xFFFFFF, Math.round(30 * alpha)));
    }
    private static boolean over(float cx, float cy, float r, float mx, float my) { return (mx - cx) * (mx - cx) + (my - cy) * (my - cy) <= r * r; }

    private static void volume(DrawContext g, float x, float y, float w, int vol, float alpha, float mx, float my) {
        Render.bar(g, x, y, w, 3, vol / 100f, Col.withAlpha(0xFFFFFF, Math.round(38 * alpha)), Col.mulAlpha(Theme.text, alpha));
    }

    private static void marquee(DrawContext g, String s, float x, float y, float w, float size, int color, boolean heading) {
        if (!s.equals(lastTitle) && heading) { lastTitle = s; titleChanged = Anim.seconds(); }
        float tw = Fonts.width(Fonts.Role.HUD, s, size);
        if (tw <= w || !Anim.enabled()) { Fonts.draw(g, Fonts.Role.HUD, Fonts.fit(Fonts.Role.HUD, s, size, w), x, y, color, size); return; }
        double t = Anim.seconds() - titleChanged, travel = tw - w + 12, cycle = 2.5 + travel / 22 + 2.5;
        double ph = t % (cycle * 2);
        float off = (float) (ph < 2.5 ? 0 : ph < 2.5 + travel / 22 ? (ph - 2.5) * 22 : ph < cycle ? travel : ph < cycle + travel / 22 ? travel - (ph - cycle) * 22 : 0);
        // DrawContext (1.21.6+) transforms scissor rectangles by the current matrix, so local coordinates are used.
        g.enableScissor((int) Math.floor(x), (int) Math.floor(y - 1), (int) Math.ceil(x + w), (int) Math.ceil(y + Fonts.px(Fonts.Role.HUD, size) + 2));
        Fonts.draw(g, Fonts.Role.HUD, s, x - off, y, color, size);
        g.disableScissor();
    }

    // ---------------------------------------------------------------- input
    /** Which control is under (mx, my) for a player drawn at (x, y, scale). */
    public static Hit hit(float x, float y, float scale, float mx, float my) {
        if (SpotifyClient.get().track() == null || !s().showControls) return Hit.NONE;
        float lx = (mx - x) / scale, ly = (my - y) / scale;
        float cx, cy;
        switch (s().layout) {
            case "compact" -> { float ctrlW = 68, logoW = s().showLogo ? 16 : 0; cx = 200 - ctrlW - logoW - 4; cy = 7; }
            case "cover" -> { cx = (124 - 64) / 2f; cy = 172 - 21; }
            default -> {
                float tx = s().showArt ? 70 : 10; cx = tx; cy = 72 - 21;
                if (s().showVolume && lx >= 232 - 64 && lx <= 222 && ly >= cy && ly <= cy + 16) return Hit.VOLUME;
            }
        }
        if (over(cx + 8, cy + 8, 8, lx, ly)) return Hit.PREV;
        if (over(cx + 32, cy + 8, 9.5f, lx, ly)) return Hit.TOGGLE;
        if (over(cx + 56, cy + 8, 8, lx, ly)) return Hit.NEXT;
        return Hit.NONE;
    }
    /** Performs the control under the mouse; returns true if something was clicked. */
    public static boolean click(float x, float y, float scale, float mx, float my) {
        Hit h = hit(x, y, scale, mx, my);
        SpotifyClient c = SpotifyClient.get();
        switch (h) {
            case PREV -> c.previous();
            case TOGGLE -> c.togglePlay();
            case NEXT -> c.next();
            case VOLUME -> c.setVolume(Math.round(Math.max(0, Math.min(1, ((mx - x) / scale - (232 - 64)) / 54f)) * 100));
            default -> { return false; }
        }
        Sounds.click();
        return true;
    }
}
