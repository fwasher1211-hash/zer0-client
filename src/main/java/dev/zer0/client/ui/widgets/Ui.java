package dev.zer0.client.ui.widgets;

import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.ui.core.*;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Per-screen interaction state shared by widgets: focus, the floating overlay (dropdown/popover), tooltips, toasts. */
public final class Ui {
    private Ui() {}

    public static Widget focused;     // keyboard focus
    public static Widget captured;    // receives drags until mouse up
    public static Popup overlay;      // drawn last, gets clicks first (open dropdowns)

    /** Something floating above the page (dropdown list). */
    public interface Popup {
        void renderPopup(DrawContext g, float mx, float my);
        /** Return true if the click was inside the popup (consumed). Clicks outside close it. */
        boolean popupMouseDown(float mx, float my, int button);
        boolean popupScroll(float mx, float my, double amount);
        void close();
    }
    private static String tip; private static float tipX, tipY; private static Object tipOwner; private static double tipSince;
    private static String toast = ""; private static double toastAt; private static boolean toastBad;

    public static void reset() { focused = null; captured = null; overlay = null; tip = null; tipOwner = null; }

    /** Offer a tooltip for this frame (shown after a short hover delay). */
    public static void hoverTip(Object owner, String text, float mx, float my) {
        if (text == null || text.isEmpty() || !Zer0Settings.get().tooltips) return;
        if (owner != tipOwner) { tipOwner = owner; tipSince = Anim.seconds(); }
        tip = text; tipX = mx; tipY = my;
    }
    public static void drawTooltip(DrawContext g, float sw, float sh) {
        String t = tip; tip = null;
        if (t == null) { tipOwner = null; return; }
        float a = Anim.clamp01((float) ((Anim.seconds() - tipSince - 0.45) / 0.15));
        if (a <= 0) return;
        float size = 7.5f, maxW = 190;
        List<String> lines = Fonts.wrap(Fonts.Role.UI, t, size, maxW);
        float lw = 0; for (String l : lines) lw = Math.max(lw, Fonts.width(Fonts.Role.UI, l, size));
        float lh = Fonts.px(Fonts.Role.UI, size) + 3, bw = lw + 14, bh = lines.size() * lh + 10;
        float x = Math.min(tipX + 10, sw - bw - 4), y = tipY + 14;
        if (y + bh > sh - 4) y = tipY - bh - 6;
        int body = Theme.dark ? 0xF01C1C24 : 0xF7FFFFFF;
        Render.shadow(g, x, y, bw, bh, 8, 8, Col.mulAlpha(Theme.shadow, a));
        Render.rect(g, x, y, bw, bh, 8, Col.mulAlpha(body, a));
        Render.stroke(g, x, y, bw, bh, 8, 0.5f, Col.mulAlpha(Theme.panelEdge, a));
        float ty = y + 6;
        for (String l : lines) { Fonts.draw(g, Fonts.Role.UI, l, x + 7, ty, Col.mulAlpha(Theme.text, a), size); ty += lh; }
    }

    public static void toast(String msg) { toast(msg, false); }
    public static void toast(String msg, boolean bad) { if (msg == null || msg.isEmpty()) return; toast = msg; toastBad = bad; toastAt = Anim.seconds(); }
    public static void drawToast(DrawContext g, float cx, float bottom) {
        double age = Anim.seconds() - toastAt;
        if (toast.isEmpty() || age > 4.2) return;
        float a = (float) Math.min(1, Math.min(age / 0.2, (4.2 - age) / 0.4));
        float slide = (1 - Anim.easeOutCubic((float) Math.min(1, age / 0.3))) * 10;
        float size = 8f, tw = Fonts.width(Fonts.Role.UI, toast, size), bw = tw + 30, bh = 22;
        float x = cx - bw / 2, y = bottom - bh - 12 + slide;
        Render.shadow(g, x, y, bw, bh, 11, 10, Col.mulAlpha(Theme.shadow, a));
        Render.rect(g, x, y, bw, bh, 11, Col.mulAlpha(Theme.dark ? 0xF2202029 : 0xF8FFFFFF, a));
        Render.circle(g, x + 11, y + bh / 2, 2.5f, Col.mulAlpha(toastBad ? Theme.bad : Theme.accent, a));
        Fonts.draw(g, Fonts.Role.UI, toast, x + 19, y + (bh - Fonts.px(Fonts.Role.UI, size)) / 2f, Col.mulAlpha(Theme.text, a), size);
    }
}
