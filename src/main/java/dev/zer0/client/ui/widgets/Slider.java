package dev.zer0.client.ui.widgets;

import dev.zer0.client.ui.core.*;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;

/** Smooth slider: animated fill, growing knob on hover/drag, value bubble, wheel adjust, step snapping. */
public class Slider extends Widget {
    private final double min, max, step;
    private final Supplier<Double> get;
    private final Consumer<Double> set;
    private final DoubleFunction<String> fmt;
    private final Anim.Smooth vis, hover = new Anim.Smooth(0, 16), drag = new Anim.Smooth(0, 18);
    private boolean dragging;
    public Runnable onRelease;

    public Slider(double min, double max, double step, Supplier<Double> get, Consumer<Double> set, DoubleFunction<String> fmt) {
        this.min = min; this.max = max; this.step = step; this.get = get; this.set = set; this.fmt = fmt;
        prefW = 150; prefH = 18;
        vis = new Anim.Smooth((float) norm(get.get()), 20);
    }
    private double norm(double v) { return Math.max(0, Math.min(1, (v - min) / (max - min))); }
    private double snap(double v) { v = Math.max(min, Math.min(max, v)); return step > 0 ? Math.round((v - min) / step) * step + min : v; }

    @Override public void render(DrawContext g, float mx, float my) {
        boolean en = isEnabled(), hov = en && over(mx, my);
        float hv = hover.to(hov || dragging ? 1 : 0), dr = drag.to(dragging ? 1 : 0);
        float t = vis.to((float) norm(get.get()));
        String label = fmt.apply(get.get());
        float lw = 34, tx = x, tw = w - lw - 6, cy = y + h / 2f, th = 4 + hv;
        int track = Theme.dark ? 0x26FFFFFF : 0x1F14141B;
        Render.rect(g, tx, cy - th / 2, tw, th, th / 2, track);
        int fill = en ? Theme.accent : Col.mulAlpha(Theme.accent, 0.4f);
        Render.rect(g, tx, cy - th / 2, Math.max(th, tw * t), th, th / 2, fill);
        float kr = 5.5f + hv * 1.0f + dr * 0.8f, kx = tx + tw * t;
        Render.shadow(g, kx - kr, cy - kr, kr * 2, kr * 2, kr, 3, 0x45000000);
        Render.circle(g, kx, cy, kr, 0xFFFFFFFF);
        Render.circle(g, kx, cy, kr * 0.36f * (0.6f + dr * 0.4f + hv * 0.2f), fill);
        Fonts.drawRight(g, Fonts.Role.UI, label, x + w, cy - Fonts.px(Fonts.Role.UI, 7.5f) / 2f, Theme.textMuted, 7.5f);
        if (dr > 0.05f) {   // value bubble while dragging
            float bw = Fonts.width(Fonts.Role.UI, label, 7f) + 10, bh = 13, by = cy - kr - bh - 4 - (1 - dr) * 4;
            Render.rect(g, kx - bw / 2, by, bw, bh, 6.5f, Col.mulAlpha(Theme.dark ? 0xF2FFFFFF : 0xF21B1B24, dr));
            Fonts.drawCentered(g, Fonts.Role.UI, label, kx, by + (bh - Fonts.px(Fonts.Role.UI, 7f)) / 2f, Col.mulAlpha(Theme.dark ? 0xFF14141B : 0xFFFFFFFF, dr), 7f);
        }
        if (hov && tooltip != null) Ui.hoverTip(this, tooltip, mx, my);
    }
    private void setFrom(float mx) {
        float tw = w - 40;
        double v = snap(min + (max - min) * Math.max(0, Math.min(1, (mx - x) / tw)));
        if (Math.abs(v - get.get()) > 1e-9) set.accept(v);
    }
    @Override public boolean mouseDown(float mx, float my, int b) {
        if (b != 0 || !isEnabled() || !over(mx, my) || mx > x + w - 38) return false;
        dragging = true; Ui.captured = this; setFrom(mx); return true;
    }
    @Override public void mouseDrag(float mx, float my, int b) { if (dragging) setFrom(mx); }
    @Override public void mouseUp(float mx, float my, int b) { if (dragging && onRelease != null) onRelease.run(); dragging = false; }
}
