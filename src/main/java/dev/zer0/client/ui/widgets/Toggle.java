package dev.zer0.client.ui.widgets;

import dev.zer0.client.ui.core.*;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** iOS-style switch with a sliding, slightly stretching knob. */
public class Toggle extends Widget {
    private final Supplier<Boolean> get;
    private final Consumer<Boolean> set;
    private final Anim.Smooth pos, hover = new Anim.Smooth(0, 16);
    private boolean down;

    public Toggle(Supplier<Boolean> get, Consumer<Boolean> set) {
        this.get = get; this.set = set; prefW = 30; prefH = 17;
        pos = new Anim.Smooth(get.get() ? 1 : 0, 14);
    }

    @Override public void render(DrawContext g, float mx, float my) {
        boolean on = get.get(), en = isEnabled(), hov = en && over(mx, my);
        float p = pos.to(on ? 1 : 0), hv = hover.to(hov ? 1 : 0);
        float tw = 30, th = 17, tx = x + w - tw, ty = y + (h - th) / 2f;
        int off = Theme.dark ? Col.withAlpha(0xFFFFFF, Math.round(38 + hv * 18)) : Col.withAlpha(0x14141B, Math.round(30 + hv * 12));
        int track = Col.mix(off, Theme.accent, Anim.easeInOutCubic(p));
        if (!en) track = Col.mulAlpha(track, 0.4f);
        Render.rect(g, tx, ty, tw, th, th / 2, track);
        float stretch = down ? 3 : 0, kd = th - 4;
        float kx = tx + 2 + (tw - kd - 4 - stretch) * p;
        Render.shadow(g, kx, ty + 2, kd + stretch, kd, kd / 2, 2, 0x40000000);
        Render.rect(g, kx, ty + 2, kd + stretch, kd, kd / 2, en ? 0xFFFFFFFF : 0xB0FFFFFF);
        if (hov && tooltip != null) Ui.hoverTip(this, tooltip, mx, my);
    }
    @Override public boolean mouseDown(float mx, float my, int b) {
        if (b != 0 || !isEnabled() || !over(mx, my)) return false;
        down = true; Ui.captured = this; return true;
    }
    @Override public void mouseUp(float mx, float my, int b) {
        if (down && over(mx, my)) { Sounds.click(); set.accept(!get.get()); }
        down = false;
    }
}
