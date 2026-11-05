package dev.zer0.client.ui.widgets;

import dev.zer0.client.ui.core.*;
import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Select box with an animated, scrollable floating list. Optional per-option preview renderer (used by the font picker). */
public class Dropdown extends Widget implements Ui.Popup {
    public record Opt(String id, String label) {}
    public interface OptRenderer { void draw(DrawContext g, Opt o, float x, float y, float w, float h, int color); }

    private final Supplier<List<Opt>> options;
    private final Supplier<String> get;
    private final Consumer<String> set;
    public OptRenderer optRenderer;
    private boolean open;
    private final Anim.Smooth openA = new Anim.Smooth(0, 18), hover = new Anim.Smooth(0, 16);
    private float scroll;
    private static final float ROW = 18, MAX_ROWS = 8;

    public Dropdown(Supplier<List<Opt>> options, Supplier<String> get, Consumer<String> set) {
        this.options = options; this.get = get; this.set = set; prefW = 150; prefH = 20;
    }
    public Dropdown render(OptRenderer r) { this.optRenderer = r; return this; }

    private String currentLabel() {
        String id = get.get();
        for (Opt o : options.get()) if (o.id.equals(id)) return o.label;
        return id;
    }

    @Override public void render(DrawContext g, float mx, float my) {
        boolean en = isEnabled(), hov = en && over(mx, my);
        float hv = hover.to(hov || open ? 1 : 0);
        float r = Math.min(h / 2, Layout.current().radius - 2);
        Render.rect(g, x, y, w, h, r, Col.mix(Theme.btn, Theme.btnHover, hv));
        Render.stroke(g, x, y, w, h, r, 0.5f, Col.withAlpha(0xFFFFFF, Theme.dark ? 30 : 120));
        float size = 8f, ty = y + (h - Fonts.px(Fonts.Role.UI, size)) / 2f;
        Opt cur = new Opt(get.get(), currentLabel());
        if (optRenderer != null) optRenderer.draw(g, cur, x + 9, y, w - 28, h, en ? Theme.btnText : Theme.btnDisabledText);
        else Fonts.draw(g, Fonts.Role.UI, Fonts.fit(Fonts.Role.UI, cur.label, size, w - 30), x + 9, ty, en ? Theme.btnText : Theme.btnDisabledText, size);
        chevron(g, x + w - 13, y + h / 2f, openA.v, Theme.textMuted);
        if (!open) openA.to(0);
        if (hov && tooltip != null) Ui.hoverTip(this, tooltip, mx, my);
    }

    private static void chevron(DrawContext g, float cx, float cy, float rot, int c) {
        // two tiny rounded strokes forming a "v" that flips when open
        float s = 2.6f, dir = 1 - 2 * Anim.easeInOutCubic(rot);
        for (int i = 0; i < 4; i++) {
            float t = i / 3f;
            Render.rect(g, cx - s + t * s - 0.7f, cy - dir * s * 0.5f + dir * t * s - 0.7f, 1.4f, 1.4f, 0.7f, c);
            Render.rect(g, cx + s - t * s - 0.7f, cy - dir * s * 0.5f + dir * t * s - 0.7f, 1.4f, 1.4f, 0.7f, c);
        }
    }

    private float listH() { return Math.min(options.get().size(), MAX_ROWS) * ROW + 8; }
    private float listY() {
        float lh = listH();
        float below = y + h + 4;
        return below + lh > dev.zer0.client.compat.Mc.sh() - 6 ? y - lh - 4 : below;
    }

    @Override public void renderPopup(DrawContext g, float mx, float my) {
        float a = Anim.easeOutCubic(openA.to(open ? 1 : 0));
        if (a <= 0.01f && !open) { if (Ui.overlay == this) Ui.overlay = null; return; }
        List<Opt> opts = options.get();
        float lh = listH() * (0.85f + 0.15f * a), ly = listY() + (1 - a) * -6;
        int body = Theme.dark ? 0xF41C1C24 : 0xFAFFFFFF;
        Render.shadow(g, x, ly, w, lh, 10, 12, Col.mulAlpha(Theme.shadow, a * 1.4f));
        Render.rect(g, x, ly, w, lh, 10, Col.mulAlpha(body, a));
        Render.stroke(g, x, ly, w, lh, 10, 0.5f, Col.mulAlpha(Theme.panelEdge, a));
        float maxScroll = Math.max(0, opts.size() * ROW - (listH() - 8));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        g.enableScissor((int) x, (int) ly + 4, (int) (x + w), (int) (ly + lh - 4));
        String cur = get.get();
        for (int i = 0; i < opts.size(); i++) {
            float ry = ly + 4 + i * ROW - scroll;
            if (ry + ROW < ly || ry > ly + lh) continue;
            Opt o = opts.get(i);
            boolean hv = mx >= x && mx < x + w && my >= ry && my < ry + ROW && my >= ly && my < ly + lh;
            boolean sel = o.id.equals(cur);
            if (sel || hv) Render.rect(g, x + 4, ry + 1, w - 8, ROW - 2, 6, Col.mulAlpha(sel ? Theme.accentSoft : Theme.cardHover, a));
            int c = Col.mulAlpha(Theme.text, a);
            if (optRenderer != null) optRenderer.draw(g, o, x + 11, ry, w - 30, ROW, c);
            else Fonts.draw(g, Fonts.Role.UI, Fonts.fit(Fonts.Role.UI, o.label, 8f, w - 30), x + 11, ry + (ROW - Fonts.px(Fonts.Role.UI, 8f)) / 2f, c, 8f);
            if (sel) Render.circle(g, x + w - 13, ry + ROW / 2f, 2.2f, Col.mulAlpha(Theme.accent, a));
        }
        g.disableScissor();
        if (maxScroll > 0) {   // slim scroll indicator
            float track = lh - 12, th = Math.max(16, track * (listH() - 8) / (opts.size() * ROW));
            Render.rect(g, x + w - 4, ly + 6 + (track - th) * (scroll / maxScroll), 2, th, 1, Col.mulAlpha(Theme.textFaint, a));
        }
    }

    @Override public boolean mouseDown(float mx, float my, int b) {
        if (b != 0 || !isEnabled() || !over(mx, my)) return false;
        Sounds.click();
        if (open) { close(); return true; }
        if (Ui.overlay != null) Ui.overlay.close();
        open = true; Ui.overlay = this;
        // scroll so the current option is visible
        List<Opt> opts = options.get();
        for (int i = 0; i < opts.size(); i++) if (opts.get(i).id.equals(get.get())) scroll = Math.max(0, (i - 3) * ROW);
        return true;
    }
    @Override public boolean popupMouseDown(float mx, float my, int b) {
        float ly = listY(), lh = listH();
        if (mx >= x && mx < x + w && my >= ly && my < ly + lh) {
            int i = (int) ((my - ly - 4 + scroll) / ROW);
            List<Opt> opts = options.get();
            if (i >= 0 && i < opts.size()) { Sounds.click(); set.accept(opts.get(i).id); close(); }
            return true;
        }
        if (over(mx, my)) { close(); return true; }
        close();
        return false;
    }
    @Override public boolean popupScroll(float mx, float my, double amount) {
        float ly = listY();
        if (mx >= x && mx < x + w && my >= ly && my < ly + listH()) { scroll -= (float) amount * ROW * 1.5f; return true; }
        return false;
    }
    @Override public void close() { open = false; }
}
