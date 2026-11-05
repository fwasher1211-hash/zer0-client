package dev.zer0.client.ui.widgets;

import dev.zer0.client.ui.core.*;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Colour control: preset swatches + live hex field. Works on 0xRRGGBB values. */
public class ColorPicker extends Widget {
    public static final int[] PRESETS = {0x8B7CFF, 0x5AA9FF, 0x3DDC97, 0xFFC857, 0xFF7A59, 0xFF5C8A, 0xFFFFFF, 0x1DB954};
    private final Supplier<Integer> get;
    private final Consumer<Integer> set;
    private final TextField hex;
    private final Anim.Smooth[] hov = new Anim.Smooth[PRESETS.length];

    public ColorPicker(Supplier<Integer> get, Consumer<Integer> set) {
        this.get = get; this.set = set;
        hex = new TextField("RRGGBB").hexOnly().value(Col.hex(get.get()));
        hex.onChange = s -> { int v = Col.parseHex(s); if (v >= 0) set.accept(v); };
        for (int i = 0; i < hov.length; i++) hov[i] = new Anim.Smooth(0, 16);
        prefW = 262; prefH = 20;
    }

    @Override public void render(DrawContext g, float mx, float my) {
        int cur = get.get() & 0xFFFFFF;
        if (!hex.isFocused() && Col.parseHex(hex.text) != cur) hex.value(Col.hex(cur));
        float s = 14, gap = 4.5f, sx = x;
        for (int i = 0; i < PRESETS.length; i++) {
            float cx = sx + s / 2, cy = y + h / 2f;
            boolean over = Math.hypot(mx - cx, my - cy) <= s / 2 + 1;
            float hv = hov[i].to(over ? 1 : 0);
            float rad = s / 2 - 0.8f + hv * 0.8f;
            if (cur == PRESETS[i]) Render.circle(g, cx, cy, rad + 2.2f, Col.withAlpha(Theme.dark ? 0xFFFFFF : 0x14141B, 200));
            Render.circle(g, cx, cy, rad + (cur == PRESETS[i] ? 0.6f : 0), cur == PRESETS[i] ? (Theme.dark ? 0xFF14141B : 0xFFFFFFFF) : 0);
            Render.circle(g, cx, cy, rad - (cur == PRESETS[i] ? 1.2f : 0), 0xFF000000 | PRESETS[i]);
            sx += s + gap;
        }
        float hx = sx + 4;
        hex.place(hx, y, x + w - hx - 22, h);
        hex.render(g, mx, my);
        Render.rect(g, x + w - 18, y + 1, 18, h - 2, 6, 0xFF000000 | cur);
        Render.stroke(g, x + w - 18, y + 1, 18, h - 2, 6, 0.5f, 0x40FFFFFF);
        if (over(mx, my) && tooltip != null) Ui.hoverTip(this, tooltip, mx, my);
    }
    @Override public boolean mouseDown(float mx, float my, int b) {
        if (!over(mx, my) || !isEnabled()) return false;
        float s = 14, gap = 4.5f, sx = x;
        for (int p : PRESETS) {
            if (Math.hypot(mx - (sx + s / 2), my - (y + h / 2f)) <= s / 2 + 1.5) { Sounds.click(); set.accept(p); hex.value(Col.hex(p)); return true; }
            sx += s + gap;
        }
        return hex.mouseDown(mx, my, b);
    }
    @Override public boolean key(int key, int mods) { return hex.key(key, mods); }
    @Override public boolean chr(int cp) { return hex.chr(cp); }
    @Override public boolean wantsKeyboard() { return true; }
    /** Keyboard goes to the inner hex field when it holds focus. */
    public TextField field() { return hex; }
}
