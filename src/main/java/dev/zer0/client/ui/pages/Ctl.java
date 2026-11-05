package dev.zer0.client.ui.pages;

import com.example.donutqueue.Zer0Bridge;
import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.ui.core.*;
import dev.zer0.client.ui.page.Row;
import dev.zer0.client.ui.widgets.*;
import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.function.*;

/** Small factories that bind widgets to real settings and persist them (original config via Zer0Bridge, ZER0 via Zer0Settings). */
final class Ctl {
    private Ctl() {}

    static Zer0Settings z() { return Zer0Settings.get(); }

    // ------------------------------------------------------------------ original-mod config (saved to config/donutqueue.json)
    static Toggle cfgToggle(Supplier<Boolean> get, Consumer<Boolean> set) {
        return new Toggle(get, v -> { set.accept(v); Zer0Bridge.save(); });
    }
    static Slider cfgScale(Supplier<Float> get, Consumer<Float> set) {
        Slider s = new Slider(0.5, 2.0, 0.05, () -> (double) get.get(), v -> set.accept(Zer0Bridge.clampScale(v.floatValue())), v -> String.format("%.2fx", v));
        s.onRelease = Zer0Bridge::save;
        return s;
    }
    static Slider cfgInt(int min, int max, Supplier<Integer> get, Consumer<Integer> set) {
        Slider s = new Slider(min, max, 1, () -> (double) get.get(), v -> set.accept((int) Math.round(v)), v -> String.valueOf((int) Math.round(v)));
        s.onRelease = Zer0Bridge::save;
        return s;
    }
    /** Colour stored as ARGB int in the original config (alpha forced to opaque). */
    static ColorPicker cfgColor(Supplier<Integer> get, Consumer<Integer> set) {
        return new ColorPicker(() -> get.get() & 0xFFFFFF, v -> { set.accept(0xFF000000 | v); Zer0Bridge.save(); });
    }
    /** Money field using the original parser (50k, 1.5m, 2b ...); "0" or empty clears. */
    static TextField cfgAmount(String placeholder, Supplier<Double> get, Consumer<Double> set) {
        TextField f = new TextField(placeholder).max(16).value(get.get() > 0 ? Zer0Bridge.fmtCompact(get.get()) : "");
        Runnable apply = () -> {
            String t = f.text.trim();
            double v = t.isEmpty() ? 0 : Zer0Bridge.parseAmount(t);
            if (v < 0 || Double.isNaN(v)) { Ui.toast("Use an amount like 50k, 1.5m or 2b", true); return; }
            set.accept(v); Zer0Bridge.save();
            Ui.toast(v > 0 ? "Set to " + Zer0Bridge.fmt(v) : "Cleared");
        };
        f.onSubmit = s -> apply.run();
        return f;
    }
    static TextField cfgText(String placeholder, int max, Supplier<String> get, Consumer<String> set) {
        TextField f = new TextField(placeholder).max(max).value(get.get() == null ? "" : get.get());
        f.onChange = s -> { set.accept(s); Zer0Bridge.save(); };
        return f;
    }

    // ------------------------------------------------------------------ ZER0 settings
    static Toggle zToggle(Supplier<Boolean> get, Consumer<Boolean> set) {
        return new Toggle(get, v -> { set.accept(v); Zer0Settings.touch(); });
    }
    static Slider zSlider(double min, double max, double step, Supplier<Double> get, Consumer<Double> set, DoubleFunction<String> fmt) {
        return new Slider(min, max, step, get, v -> { set.accept(v); Zer0Settings.touch(); }, fmt);
    }
    static DoubleFunction<String> pct() { return v -> Math.round(v * 100) + "%"; }

    static Button btn(String label, Runnable r) { return new Button(label, r); }
    static Button btn(Supplier<String> label, Runnable r) { return new Button(label, r); }

    /** Row whose control is a text field plus an action button (e.g. "Add"). */
    static Row.Custom fieldWithButton(String title, TextField field, String btnLabel, Consumer<String> action) {
        Button b = new Button(btnLabel, () -> { action.accept(field.text.trim()); field.value(""); });
        b.kind(Button.Kind.FROST);
        field.onSubmit = s -> { action.accept(s.trim()); field.value(""); };
        Row.Custom row = new Row.Custom(w -> 34, (g, x, y, w, h, mx, my) -> {
            float bw = Math.max(54, b.naturalWidth());
            float lw = title.isEmpty() ? 0 : Math.min(150, w * 0.35f);
            if (lw > 0) Fonts.draw(g, Fonts.Role.UI, title, x, y + (h - Fonts.px(Fonts.Role.UI, 8.5f)) / 2f, Theme.text, 8.5f);
            field.place(x + lw, y + 7, w - lw - bw - 6, 20); field.render(g, mx, my);
            b.place(x + w - bw, y + 7, bw, 20); b.render(g, mx, my);
        }).with(field, b);
        row.titled(title, "");
        return row;
    }

    /** Wrapping chips with a remove "x" (blacklists, non-gamble lists). */
    static Row.Custom chips(Supplier<List<String>> list, Consumer<String> remove, String empty) {
        final float CH = 18, GAP = 5;
        Function<Float, float[][]> flow = w -> {
            List<String> l = list.get();
            float[][] out = new float[l.size()][];
            float cx = 0, cy = 6;
            for (int i = 0; i < l.size(); i++) {
                float tw = Fonts.width(Fonts.Role.UI, l.get(i), 7.5f) + 26;
                if (cx > 0 && cx + tw > w) { cx = 0; cy += CH + GAP; }
                out[i] = new float[]{cx, cy, tw}; cx += tw + GAP;
            }
            return out;
        };
        return new Row.Custom(w -> {
            float[][] f = flow.apply(w);
            return f.length == 0 ? 28 : f[f.length - 1][1] + CH + 7;
        }, (g, x, y, w, h, mx, my) -> {
            List<String> l = list.get();
            if (l.isEmpty()) { Fonts.draw(g, Fonts.Role.UI, empty, x, y + (h - Fonts.px(Fonts.Role.UI, 7.5f)) / 2f, Theme.textFaint, 7.5f); return; }
            float[][] f = flow.apply(w);
            for (int i = 0; i < l.size() && i < f.length; i++) {
                float cx = x + f[i][0], cy = y + f[i][1], cw = f[i][2];
                boolean xh = mx >= cx + cw - 16 && mx < cx + cw && my >= cy && my < cy + CH;
                Render.rect(g, cx, cy, cw, CH, CH / 2, Theme.dark ? 0x1FFFFFFF : 0xB0FFFFFF);
                Fonts.draw(g, Fonts.Role.UI, l.get(i), cx + 9, cy + (CH - Fonts.px(Fonts.Role.UI, 7.5f)) / 2f, Theme.text, 7.5f);
                Render.circle(g, cx + cw - 9, cy + CH / 2, 5.5f, xh ? Col.withAlpha(Theme.bad, 200) : (Theme.dark ? 0x22FFFFFF : 0x2214141B));
                cross(g, cx + cw - 9, cy + CH / 2, 2.2f, xh ? 0xFFFFFFFF : Theme.textMuted);
            }
        }).click((x, y, w, h, mx, my, b) -> {
            List<String> l = list.get();
            float[][] f = flow.apply(w);
            for (int i = 0; i < l.size() && i < f.length; i++) {
                float cx = x + f[i][0], cy = y + f[i][1], cw = f[i][2];
                if (mx >= cx + cw - 16 && mx < cx + cw && my >= cy && my < cy + CH) { Sounds.click(); remove.accept(l.get(i)); return true; }
            }
            return false;
        });
    }

    /** Anti-aliased "x" made of small dots. */
    static void cross(DrawContext g, float cx, float cy, float s, int c) {
        for (int i = -3; i <= 3; i++) {
            float t = i / 3f * s;
            Render.rect(g, cx + t - 0.6f, cy + t - 0.6f, 1.2f, 1.2f, 0.6f, c);
            Render.rect(g, cx + t - 0.6f, cy - t - 0.6f, 1.2f, 1.2f, 0.6f, c);
        }
    }

    /** "x ago" for timestamps. */
    static String ago(long millis) {
        long s = Math.max(0, (System.currentTimeMillis() - millis) / 1000);
        if (s < 60) return s + "s ago";
        if (s < 3600) return (s / 60) + "m ago";
        if (s < 86400) return (s / 3600) + "h ago";
        return (s / 86400) + "d ago";
    }
    static String dur(long sec) {
        long h = sec / 3600, m = (sec % 3600) / 60, s = sec % 60;
        return h > 0 ? h + "h " + m + "m" : m > 0 ? m + "m " + s + "s" : s + "s";
    }
    static int moneyColor(double v) { return v >= 0 ? Theme.good : Theme.bad; }
    static String signed(double v) { return (v >= 0 ? "+$" : "-$") + Zer0Bridge.fmtCompact(Math.abs(v)); }

    // ------------------------------------------------------------------ shared pieces
    /** The standard "HUD box" section used by every streamer box. */
    static void boxSection(java.util.List<dev.zer0.client.ui.page.Section> out, dev.zer0.client.ui.screens.HubScreen hub, String boxId,
                           Supplier<Boolean> on, Consumer<Boolean> setOn, Supplier<Float> scale, Consumer<Float> setScale,
                           Supplier<Integer> color, Consumer<Integer> setColor, float defScale, int defColor) {
        dev.zer0.client.ui.page.Section s = new dev.zer0.client.ui.page.Section("HUD box");
        out.add(s);
        s.setting("Show on HUD", "Draw this box while you play.", cfgToggle(on, setOn));
        s.setting("Size", "Scale of the box.", cfgScale(scale, setScale),
                () -> { setScale.accept(defScale); Zer0Bridge.save(); }, () -> Math.abs(scale.get() - defScale) > 0.001f);
        s.setting("Colour", "Accent colour of the box.", cfgColor(color, setColor),
                () -> { setColor.accept(defColor); Zer0Bridge.save(); }, () -> (color.get() & 0xFFFFFF) != (defColor & 0xFFFFFF));
        Button move = new Button("Move box", () -> dev.zer0.client.compat.Mc.mc().setScreen(new dev.zer0.client.ui.screens.HudEditorScreen(hub, boxId)));
        s.setting("Position", "Opens the HUD editor with this box selected.", move);
    }

    /** Frosted statistic tile. */
    static void stat(DrawContext g, float x, float y, float w, float h, String label, String value, int valueColor, String sub) {
        Layout L = Layout.current();
        if (L.panels == Layout.Panels.GLASS) Render.glass(g, x, y, w, h, L.radius, Theme.card, Theme.panelEdge, L.shadows);
        else Render.rect(g, x, y, w, h, L.radius, Theme.card);
        float vs = h > 50 ? 15 : 12;
        Fonts.draw(g, Fonts.Role.UI, label.toUpperCase(), x + 11, y + 9, Theme.textMuted, 6.3f);
        Fonts.draw(g, Fonts.Role.HEADING, Fonts.fit(Fonts.Role.HEADING, value, vs, w - 20), x + 11, y + 9 + Fonts.px(Fonts.Role.UI, 6.3f) + 5, valueColor, vs);
        if (sub != null && !sub.isEmpty())
            Fonts.draw(g, Fonts.Role.UI, Fonts.fit(Fonts.Role.UI, sub, 6.8f, w - 20), x + 11, y + h - 8 - Fonts.px(Fonts.Role.UI, 6.8f), Theme.textFaint, 6.8f);
    }

    /** Grid of equally sized tiles laid out to the available width. */
    static Row.Custom tiles(int count, float minW, float tileH, java.util.function.IntFunction<TilePainter> tile) {
        return new Row.Custom(w -> {
            int cols = Math.max(1, Math.min(count, (int) ((w + 8) / (minW + 8))));
            int rows = (count + cols - 1) / cols;
            return rows * tileH + (rows - 1) * 8 + 4;
        }, (g, x, y, w, h, mx, my) -> {
            int cols = Math.max(1, Math.min(count, (int) ((w + 8) / (minW + 8))));
            float tw = (w - (cols - 1) * 8) / cols;
            for (int i = 0; i < count; i++) {
                float tx = x + (i % cols) * (tw + 8), ty = y + 2 + (i / cols) * (tileH + 8);
                tile.apply(i).paint(g, tx, ty, tw, tileH, mx, my);
            }
        });
    }
    interface TilePainter { void paint(DrawContext g, float x, float y, float w, float h, float mx, float my); }
}
