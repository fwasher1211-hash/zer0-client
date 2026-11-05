package dev.zer0.client.ui.page;

import dev.zer0.client.ui.core.*;
import dev.zer0.client.ui.widgets.Button;
import dev.zer0.client.ui.widgets.Ui;
import dev.zer0.client.ui.widgets.Widget;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** A line inside a section. Subclasses cover the standard shapes; {@link Custom} handles charts, lists and grids. */
public abstract class Row {
    public String title = "", desc = "", keywords = "";
    public BooleanSupplier visible = () -> true;
    public float x, y, w, h;
    /** Set by the hub so search results can show where a row lives. */
    public Page page;

    public Row keywords(String k) { this.keywords = k; return this; }
    public Row when(BooleanSupplier v) { this.visible = v; return this; }

    public abstract float height(float width);
    public void place(float x, float y, float w) { this.x = x; this.y = y; this.w = w; this.h = height(w); }
    public abstract void render(DrawContext g, float mx, float my);
    public List<Widget> widgets() { return List.of(); }

    public boolean mouseDown(float mx, float my, int b) {
        for (Widget wd : widgets()) if (wd.mouseDown(mx, my, b)) return true;
        return false;
    }
    public boolean scroll(float mx, float my, double a) {
        for (Widget wd : widgets()) if (wd.scroll(mx, my, a)) return true;
        return false;
    }
    public boolean searchable() { return !title.isEmpty(); }
    public boolean matches(String q) {
        String hay = (title + " " + desc + " " + keywords).toLowerCase();
        for (String part : q.toLowerCase().split("\\s+")) if (!part.isEmpty() && !hay.contains(part)) return false;
        return true;
    }

    protected static float size(float base) { return base; }
    protected static float rowH() { return Layout.current().rowH; }
    protected static boolean showDesc() { return Layout.current() != Layout.COMPACT; }

    // ================================================================== title + description on the left, control on the right
    public static class Setting extends Row {
        public final Widget control;
        private Runnable reset; private BooleanSupplier changed = () -> false;
        private final Button resetBtn;
        private final Anim.Smooth hover = new Anim.Smooth(0, 14);

        public Setting(String title, String desc, Widget control) {
            this.title = title; this.desc = desc == null ? "" : desc; this.control = control;
            resetBtn = new Button("↺", () -> { if (reset != null) reset.run(); });
            resetBtn.kind(Button.Kind.GHOST).text(8.5f);
            resetBtn.tooltip = "Reset to default";
        }
        public Setting reset(Runnable r, BooleanSupplier changed) { this.reset = r; this.changed = changed; return this; }

        @Override public float height(float width) {
            float base = rowH();
            if (showDesc() && !desc.isEmpty()) base = Math.max(base, Fonts.px(Fonts.Role.UI, 8.5f) + Fonts.px(Fonts.Role.UI, 7f) + 15);
            return Math.max(base, control.prefH + 10);
        }
        @Override public void render(DrawContext g, float mx, float my) {
            boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
            float hv = hover.to(hov ? 1 : 0);
            float cw = control.prefW <= 0 && control instanceof Button b ? b.naturalWidth() : control.prefW;
            cw = Math.min(cw, w * 0.58f);
            float cx = x + w - cw;
            control.place(cx, y + (h - control.prefH) / 2f, cw, control.prefH);
            boolean canReset = reset != null && changed.getAsBoolean();
            float textW = cx - x - (canReset ? 24 : 10);
            float ts = 8.5f, ds = 7f;
            boolean withDesc = showDesc() && !desc.isEmpty();
            float th = Fonts.px(Fonts.Role.UI, ts) + (withDesc ? Fonts.px(Fonts.Role.UI, ds) + 3 : 0);
            float ty = y + (h - th) / 2f;
            Fonts.draw(g, Fonts.Role.UI, Fonts.fit(Fonts.Role.UI, title, ts, textW), x, ty, Theme.text, ts);
            if (withDesc) Fonts.draw(g, Fonts.Role.UI, Fonts.fit(Fonts.Role.UI, desc, ds, textW), x, ty + Fonts.px(Fonts.Role.UI, ts) + 3, Theme.textMuted, ds);
            if (hov && !desc.isEmpty() && mx < cx - 4 && (!withDesc || Fonts.width(Fonts.Role.UI, desc, ds) > textW)) Ui.hoverTip(this, desc, mx, my);
            if (canReset) {
                resetBtn.place(cx - 20, y + (h - 16) / 2f, 16, 16);
                if (hv > 0.02f || resetBtn.over(mx, my)) resetBtn.render(g, mx, my);
            }
            control.render(g, mx, my);
        }
        @Override public List<Widget> widgets() {
            return reset != null && changed.getAsBoolean() ? List.of(control, resetBtn) : List.of(control);
        }
    }

    // ================================================================== full-width control under an optional label
    public static class Wide extends Row {
        public final Widget control;
        public Wide(String title, String desc, Widget control) { this.title = title == null ? "" : title; this.desc = desc == null ? "" : desc; this.control = control; }
        @Override public float height(float width) {
            float lab = title.isEmpty() ? 0 : Fonts.px(Fonts.Role.UI, 8f) + 5;
            return 7 + lab + control.prefH + 7;
        }
        @Override public void render(DrawContext g, float mx, float my) {
            float cy = y + 7;
            if (!title.isEmpty()) {
                Fonts.draw(g, Fonts.Role.UI, title, x, cy, Theme.text, 8f);
                if (!desc.isEmpty() && showDesc()) {
                    float tw = Fonts.width(Fonts.Role.UI, title, 8f);
                    Fonts.draw(g, Fonts.Role.UI, Fonts.fit(Fonts.Role.UI, desc, 7f, w - tw - 10), x + tw + 8, cy + 0.6f, Theme.textMuted, 7f);
                }
                cy += Fonts.px(Fonts.Role.UI, 8f) + 5;
            }
            control.place(x, cy, w, control.prefH);
            control.render(g, mx, my);
        }
        @Override public List<Widget> widgets() { return List.of(control); }
    }

    // ================================================================== a wrapping line of buttons
    public static class Buttons extends Row {
        private final List<Button> buttons = new ArrayList<>();
        public Buttons(Button... b) { buttons.addAll(List.of(b)); }
        private List<float[]> flow(float width) {
            List<float[]> pos = new ArrayList<>();
            float cx = 0, cy = 6, gap = 6;
            for (Button b : buttons) {
                float bw = b.prefW > 0 ? b.prefW : b.naturalWidth();
                if (cx > 0 && cx + bw > width) { cx = 0; cy += 20 + gap; }
                pos.add(new float[]{cx, cy, bw}); cx += bw + gap;
            }
            return pos;
        }
        @Override public float height(float width) { List<float[]> f = flow(width); return f.isEmpty() ? 0 : f.get(f.size() - 1)[1] + 20 + 6; }
        @Override public void render(DrawContext g, float mx, float my) {
            List<float[]> f = flow(w);
            for (int i = 0; i < buttons.size(); i++) {
                float[] p = f.get(i);
                buttons.get(i).place(x + p[0], y + p[1], p[2], 20);
                buttons.get(i).render(g, mx, my);
            }
        }
        @Override public List<Widget> widgets() { return new ArrayList<>(buttons); }
        @Override public boolean searchable() { return false; }
    }

    // ================================================================== paragraph
    public static class Note extends Row {
        private final Supplier<String> text;
        public Note(Supplier<String> t) { this.text = t; }
        @Override public float height(float width) {
            String s = text.get();
            if (s == null || s.isEmpty()) return 0;
            return Fonts.wrap(Fonts.Role.UI, s, 7.5f, width).size() * (Fonts.px(Fonts.Role.UI, 7.5f) + 3) + 10;
        }
        @Override public void render(DrawContext g, float mx, float my) {
            String s = text.get();
            if (s == null || s.isEmpty()) return;
            float ty = y + 5;
            for (String l : Fonts.wrap(Fonts.Role.UI, s, 7.5f, w)) { Fonts.draw(g, Fonts.Role.UI, l, x, ty, Theme.textMuted, 7.5f); ty += Fonts.px(Fonts.Role.UI, 7.5f) + 3; }
        }
        @Override public boolean searchable() { return false; }
    }

    // ================================================================== anything else
    public interface Painter { void paint(DrawContext g, float x, float y, float w, float h, float mx, float my); }
    public interface Clicker { boolean click(float x, float y, float w, float h, float mx, float my, int button); }
    public interface Heighter { float height(float width); }

    public static class Custom extends Row {
        private final Heighter heighter; private final Painter painter; private Clicker clicker;
        private Scroller scroller; private final List<Widget> widgets = new ArrayList<>();
        public interface Scroller { boolean scroll(float mx, float my, double amount); }
        public Custom(Heighter h, Painter p) { this.heighter = h; this.painter = p; }
        public Custom(float h, Painter p) { this(w -> h, p); }
        public Custom click(Clicker c) { this.clicker = c; return this; }
        public Custom onScroll(Scroller s) { this.scroller = s; return this; }
        public Custom with(Widget... w) { widgets.addAll(List.of(w)); return this; }
        public Custom titled(String t, String d) { title = t; desc = d; return this; }
        @Override public float height(float width) { return heighter.height(width); }
        @Override public void render(DrawContext g, float mx, float my) { painter.paint(g, x, y, w, h, mx, my); }
        @Override public boolean mouseDown(float mx, float my, int b) {
            if (super.mouseDown(mx, my, b)) return true;
            return clicker != null && mx >= x && mx < x + w && my >= y && my < y + h && clicker.click(x, y, w, h, mx, my, b);
        }
        @Override public boolean scroll(float mx, float my, double a) {
            if (super.scroll(mx, my, a)) return true;
            return scroller != null && mx >= x && mx < x + w && my >= y && my < y + h && scroller.scroll(mx, my, a);
        }
        @Override public List<Widget> widgets() { return widgets; }
    }
}
