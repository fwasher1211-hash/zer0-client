package dev.zer0.client.ui.widgets;

import dev.zer0.client.ui.core.*;
import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Segmented control with a sliding frosted pill (macOS-style tabs). */
public class Segmented extends Widget {
    private final List<Dropdown.Opt> opts;
    private final Supplier<String> get;
    private final Consumer<String> set;
    private final Anim.Smooth pos = new Anim.Smooth(-1, 16);

    public Segmented(List<Dropdown.Opt> opts, Supplier<String> get, Consumer<String> set) {
        this.opts = opts; this.get = get; this.set = set; prefW = 60f * opts.size(); prefH = 20;
    }
    private int idx() { String c = get.get(); for (int i = 0; i < opts.size(); i++) if (opts.get(i).id().equals(c)) return i; return 0; }

    @Override public void render(DrawContext g, float mx, float my) {
        float r = Math.min(h / 2, Layout.current().radius - 2);
        Render.rect(g, x, y, w, h, r, Theme.field);
        float cw = w / opts.size();
        if (pos.v < 0) pos.snap(idx());
        float p = pos.to(idx());
        Render.shadow(g, x + 2 + p * cw, y + 2, cw - 4, h - 4, r - 2, 3, Col.mulAlpha(Theme.shadow, 0.6f));
        Render.rect(g, x + 2 + p * cw, y + 2, cw - 4, h - 4, Math.max(2, r - 2), Theme.dark ? 0xE6FFFFFF : 0xFFFFFFFF);
        for (int i = 0; i < opts.size(); i++) {
            float cx = x + cw * i + cw / 2f;
            float closeness = 1 - Math.min(1, Math.abs(p - i));
            int c = Col.mix(Theme.textMuted, 0xFF14141B, closeness);
            Fonts.drawCentered(g, Fonts.Role.BUTTON, Fonts.fit(Fonts.Role.BUTTON, opts.get(i).label(), 7.5f, cw - 6), cx,
                    y + (h - Fonts.px(Fonts.Role.BUTTON, 7.5f)) / 2f, c, 7.5f);
        }
        if (over(mx, my) && tooltip != null) Ui.hoverTip(this, tooltip, mx, my);
    }
    @Override public boolean mouseDown(float mx, float my, int b) {
        if (b != 0 || !over(mx, my) || !isEnabled()) return false;
        int i = (int) ((mx - x) / (w / opts.size()));
        if (i >= 0 && i < opts.size() && i != idx()) { Sounds.click(); set.accept(opts.get(i).id()); }
        return true;
    }
}
