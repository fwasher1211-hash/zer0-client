package dev.zer0.client.ui.widgets;

import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.ui.core.*;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Frosted-white rounded button with hover lift, press squash, selected and disabled states. */
public class Button extends Widget {
    public enum Kind { FROST, ACCENT, GHOST, DANGER, SOLID }

    private final Supplier<String> label;
    private final Runnable action;
    public Kind kind = Kind.FROST;
    public BooleanSupplier selected = () -> false;
    public float textSize = 8.5f;
    public boolean confirm;            // dangerous action: needs a second click
    public String icon;                // optional leading glyph
    private final Anim.Smooth hover = new Anim.Smooth(0, 16), press = new Anim.Smooth(0, 24), sel = new Anim.Smooth(0, 14);
    private boolean down;
    private double armedAt = -10;

    public Button(String label, Runnable action) { this(() -> label, action); }
    public Button(Supplier<String> label, Runnable action) { this.label = label; this.action = action; prefH = 20; prefW = 0; }

    public Button kind(Kind k) { this.kind = k; return this; }
    public Button selected(BooleanSupplier s) { this.selected = s; return this; }
    public Button confirm() { this.confirm = true; return this; }
    public Button text(float s) { this.textSize = s; return this; }
    public Button icon(String i) { this.icon = i; return this; }
    @Override public Button when(BooleanSupplier b) { super.when(b); return this; }

    public float naturalWidth() { return Fonts.width(Fonts.Role.BUTTON, label.get(), textSize) + 26 + (icon != null ? 12 : 0); }

    private boolean armed() { return confirm && Zer0Settings.get().confirmDangerous && Anim.seconds() - armedAt < 3; }

    @Override public void render(DrawContext g, float mx, float my) {
        boolean en = isEnabled(), hov = en && over(mx, my);
        float hv = hover.to(hov ? 1 : 0), pr = press.to(down && hov ? 1 : 0), sl = sel.to(selected.getAsBoolean() ? 1 : 0);
        float inset = pr * 0.9f;
        float bx = x + inset, by = y + inset, bw = w - inset * 2, bh = h - inset * 2, r = Math.min(bh / 2f, Layout.current().radius - 2 + bh * 0.12f);

        int bg, fg;
        switch (kind) {
            case ACCENT -> { bg = Col.brighten(Theme.accent, 1 + hv * 0.12f - pr * 0.1f); fg = Theme.accentText; }
            case DANGER -> { bg = Col.withAlpha(Theme.bad, Math.round(50 + hv * 50 + pr * 40)); fg = Theme.dark ? 0xFFFFD4D4 : 0xFF8A1C1C; }
            case GHOST -> { bg = Col.withAlpha(Theme.dark ? 0xFFFFFF : 0x14141B, Math.round(hv * 22 + pr * 18)); fg = Theme.text; }
            case SOLID -> { bg = Col.mix(Theme.card, Theme.cardHover, hv); fg = Theme.text; }
            default -> { bg = Col.mix(Col.mix(Theme.btn, Theme.btnHover, hv), Theme.btnPress, pr); fg = Theme.btnText; }
        }
        if (sl > 0.01f) { bg = Col.mix(bg, Theme.btnSelected, sl); fg = Col.mix(fg, Theme.btnSelectedText, sl); }
        if (armed()) { bg = Col.withAlpha(Theme.bad, 200); fg = 0xFFFFFFFF; }
        if (!en) { bg = Theme.btnDisabled; fg = Theme.btnDisabledText; }

        if (kind != Kind.GHOST && en && Layout.current().shadows) Render.shadow(g, bx, by + 1, bw, bh, r, 3 + hv * 3, Col.mulAlpha(Theme.shadow, 0.55f + hv * 0.3f));
        Render.rect(g, bx, by, bw, bh, r, bg);
        if (kind == Kind.FROST && en) {   // glass sheen + hairline
            Render.rectV(g, bx, by, bw, bh * 0.55f, r, Col.withAlpha(0xFFFFFF, Math.round((Theme.dark ? 16 : 50) + hv * 10)), 0x00FFFFFF);
            Render.stroke(g, bx, by, bw, bh, r, 0.5f, Col.withAlpha(0xFFFFFF, Theme.dark ? Math.round(30 + hv * 25) : 120));
        }
        String s = armed() ? "Click again to confirm" : label.get();
        float ts = textSize * (1 - pr * 0.02f);
        float tw = Fonts.width(Fonts.Role.BUTTON, s, ts) + (icon != null ? 12 : 0);
        float ty = by + (bh - Fonts.px(Fonts.Role.BUTTON, ts)) / 2f;
        float tx = bx + (bw - tw) / 2f;
        if (icon != null) { Fonts.drawWith(g, "minecraft", false, icon, tx, ty, fg, Fonts.px(Fonts.Role.BUTTON, ts)); tx += 12; }
        Fonts.draw(g, Fonts.Role.BUTTON, Fonts.fit(Fonts.Role.BUTTON, s, ts, bw - 10), tx, ty, fg, ts);
        if (hov && tooltip != null) Ui.hoverTip(this, tooltip, mx, my);
    }

    @Override public boolean mouseDown(float mx, float my, int b) {
        if (b != 0 || !isEnabled() || !over(mx, my)) return false;
        down = true; Ui.captured = this; return true;
    }
    @Override public void mouseUp(float mx, float my, int b) {
        if (down && over(mx, my) && isEnabled()) {
            if (confirm && Zer0Settings.get().confirmDangerous && !armed()) armedAt = Anim.seconds();
            else { armedAt = -10; Sounds.click(); action.run(); }
        }
        down = false;
    }
}
