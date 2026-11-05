package dev.zer0.client.ui.screens;

import com.example.donutqueue.ModConfig;
import com.example.donutqueue.Zer0Bridge;
import dev.zer0.client.compat.Mc;
import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.spotify.SpotifyHud;
import dev.zer0.client.ui.core.*;
import dev.zer0.client.ui.widgets.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

/**
 * Drag-and-drop HUD editor for every HUD element (the five streamer boxes + Spotify).
 * In a world the real HUD is visible underneath while you drag. Snaps to edges, centre lines and other
 * elements; arrow keys nudge (Shift = 10px). Positions are saved straight into the real configs.
 */
public class HudEditorScreen extends Zer0Screen {
    private record El(String id, String name, BooleanSupplier on, Consumer<Boolean> setOn, IntSupplier gx, IntSupplier gy,
                      IntConsumer sx, IntConsumer sy, Supplier<int[]> size, Runnable save) {}

    private final List<El> els = new ArrayList<>();
    private El selected, dragging;
    private float offX, offY;
    private final List<float[]> guides = new ArrayList<>();
    private boolean snap = true;
    private Button done, reset, snapBtn;
    private final List<Toggle> toggles = new ArrayList<>();
    private final String focus;

    public HudEditorScreen(Screen parent, String focusId) {
        super("HUD Editor", parent);
        this.focus = focusId;
        ModConfig c = Zer0Bridge.cfg();
        Runnable cs = Zer0Bridge::save, zs = Zer0Settings::touch;
        els.add(new El("queue", "Queue", () -> c.hudEnabled, v -> c.hudEnabled = v, () -> c.hudX, () -> c.hudY, v -> c.hudX = v, v -> c.hudY = v, () -> Zer0Bridge.boxSize("queue"), cs));
        els.add(new El("startbal", "Starting Bal", () -> c.startBalBoxEnabled, v -> c.startBalBoxEnabled = v, () -> c.startBalBoxX, () -> c.startBalBoxY, v -> c.startBalBoxX = v, v -> c.startBalBoxY = v, () -> Zer0Bridge.boxSize("startbal"), cs));
        els.add(new El("bidding", "Bidding", () -> c.biddingBoxEnabled, v -> c.biddingBoxEnabled = v, () -> c.biddingBoxX, () -> c.biddingBoxY, v -> c.biddingBoxX = v, v -> c.biddingBoxY = v, () -> Zer0Bridge.boxSize("bidding"), cs));
        els.add(new El("info", "Info Display", () -> c.infoBoxEnabled, v -> c.infoBoxEnabled = v, () -> c.infoBoxX, () -> c.infoBoxY, v -> c.infoBoxX = v, v -> c.infoBoxY = v, () -> Zer0Bridge.boxSize("info"), cs));
        els.add(new El("earnings", "Earnings", () -> c.earningsBoxEnabled, v -> c.earningsBoxEnabled = v, () -> c.earningsBoxX, () -> c.earningsBoxY, v -> c.earningsBoxX = v, v -> c.earningsBoxY = v, () -> Zer0Bridge.boxSize("earnings"), cs));
        Zer0Settings.Spotify sp = Zer0Settings.get().spotify;
        els.add(new El("spotify", "Spotify", () -> sp.hudEnabled, v -> sp.hudEnabled = v, () -> sp.x, () -> sp.y, v -> sp.x = v, v -> sp.y = v,
                () -> { float[] s = SpotifyHud.size(); return new int[]{Math.round(s[0]), Math.round(s[1])}; }, zs));
    }

    @Override protected boolean showWorld() { return false; }
    @Override public void renderBackground(DrawContext g, int mx, int my, float d) {
        if (Mc.inWorld()) g.fill(0, 0, width, height, 0x38000000);       // keep the real HUD sharp underneath
        else super.renderBackground(g, mx, my, d);
    }

    @Override protected void build() {
        done = new Button("Done", this::close).kind(Button.Kind.ACCENT);
        snapBtn = new Button(() -> snap ? "Snapping on" : "Snapping off", () -> snap = !snap);
        reset = new Button("Reset positions", this::resetPositions).confirm();
        reset.kind(Button.Kind.DANGER);
        toggles.clear();
        for (El e : els) toggles.add(new Toggle(e.on::getAsBoolean, v -> { e.setOn.accept(v); e.save.run(); }));
        if (focus != null) for (El e : els) if (e.id.equals(focus)) selected = e;
    }

    private void resetPositions() {
        ModConfig d = Zer0Bridge.defaults(), c = Zer0Bridge.cfg();
        c.hudX = d.hudX; c.hudY = d.hudY; c.startBalBoxX = d.startBalBoxX; c.startBalBoxY = d.startBalBoxY;
        c.biddingBoxX = d.biddingBoxX; c.biddingBoxY = d.biddingBoxY; c.infoBoxX = d.infoBoxX; c.infoBoxY = d.infoBoxY;
        c.earningsBoxX = d.earningsBoxX; c.earningsBoxY = d.earningsBoxY;
        Zer0Bridge.save();
        Zer0Settings.Spotify sp = Zer0Settings.get().spotify; sp.x = 10; sp.y = 10; Zer0Settings.touch();
        Ui.toast("HUD positions reset");
    }

    @Override protected void draw(DrawContext g, float mx, float my, float delta) {
        boolean world = Mc.inWorld();
        // guides from the current drag
        for (float[] gd : guides) {
            if (gd[0] == 0) Render.rect(g, gd[1] - 0.25f, 0, 0.5f, height, 0, Col.withAlpha(Theme.accent, 200));
            else Render.rect(g, 0, gd[1] - 0.25f, width, 0.5f, 0, Col.withAlpha(Theme.accent, 200));
        }
        for (El e : els) {
            if (!e.on.getAsBoolean()) continue;
            int[] s = e.size.get();
            float x = e.gx.getAsInt(), y = e.gy.getAsInt();
            boolean hov = mx >= x && mx < x + s[0] && my >= y && my < y + s[1];
            boolean sel = e == selected;
            if (!world || e.id.equals("spotify") && !SpotifyHud.visibleNow()) {
                Render.rect(g, x, y, s[0], s[1], 8, Theme.dark ? 0x40FFFFFF : 0x60FFFFFF);
                Fonts.drawCentered(g, Fonts.Role.UI, e.name, x + s[0] / 2f, y + s[1] / 2f - 4, Theme.text, 8f);
            }
            int c = sel ? Theme.accent : hov ? 0xE0FFFFFF : 0x80FFFFFF;
            Render.stroke(g, x - 2, y - 2, s[0] + 4, s[1] + 4, 9, sel ? 1.2f : 0.7f, c);
            if (sel || hov) {   // name tag
                float tw = Fonts.width(Fonts.Role.UI, e.name, 7f) + 12;
                float ty = y - 16 < 0 ? y + s[1] + 4 : y - 16;
                Render.rect(g, x - 2, ty, tw, 12, 6, sel ? Theme.accent : 0xD0202028);
                Fonts.draw(g, Fonts.Role.UI, e.name, x + 4, ty + (12 - Fonts.px(Fonts.Role.UI, 7f)) / 2f, sel ? Theme.accentText : 0xFFFFFFFF, 7f);
            }
        }
        // side panel with element toggles
        float pw = 150, ph = 26 + els.size() * 22 + 8, px = width - pw - 10, py = 10;
        Render.glass(g, px, py, pw, ph, 12, Theme.dark ? 0xE0181820 : 0xEEFFFFFF, Theme.panelEdge, true);
        Fonts.draw(g, Fonts.Role.HEADING, "Elements", px + 12, py + 9, Theme.text, 9f);
        for (int i = 0; i < els.size(); i++) {
            El e = els.get(i);
            float ry = py + 26 + i * 22;
            Fonts.draw(g, Fonts.Role.UI, e.name, px + 12, ry + 6, e == selected ? Theme.accent : Theme.text, 7.8f);
            Toggle t = toggles.get(i);
            t.place(px + pw - 42, ry + 2, 30, 17); t.render(g, mx, my);
        }
        // bottom toolbar
        float bw = 340, bh = 34, bx = (width - bw) / 2f, by = height - bh - 12;
        Render.glass(g, bx, by, bw, bh, 17, Theme.dark ? 0xE0181820 : 0xEEFFFFFF, Theme.panelEdge, true);
        Fonts.draw(g, Fonts.Role.UI, "Drag to move · arrows nudge", bx + 14, by + (bh - Fonts.px(Fonts.Role.UI, 7f)) / 2f, Theme.textMuted, 7f);
        snapBtn.place(bx + 128, by + 7, 70, 20); snapBtn.render(g, mx, my);
        reset.place(bx + 202, by + 7, 76, 20); reset.text(7.2f); reset.render(g, mx, my);
        done.place(bx + bw - 58, by + 7, 48, 20); done.render(g, mx, my);
    }

    @Override protected boolean onMouseDown(float mx, float my, int b) {
        if (done.mouseDown(mx, my, b) || snapBtn.mouseDown(mx, my, b) || reset.mouseDown(mx, my, b)) return true;
        for (int i = 0; i < els.size(); i++) {
            if (toggles.get(i).mouseDown(mx, my, b)) return true;
            float px = width - 160, ry = 10 + 26 + i * 22;
            if (mx >= px && mx < width - 52 && my >= ry && my < ry + 22) { selected = els.get(i); return true; }
        }
        for (int i = els.size() - 1; i >= 0; i--) {
            El e = els.get(i);
            if (!e.on.getAsBoolean()) continue;
            int[] s = e.size.get();
            float x = e.gx.getAsInt(), y = e.gy.getAsInt();
            if (mx >= x - 2 && mx < x + s[0] + 2 && my >= y - 2 && my < y + s[1] + 2) {
                selected = dragging = e; offX = mx - x; offY = my - y;
                return true;
            }
        }
        selected = null;
        return false;
    }

    @Override public boolean mouseDragged(net.minecraft.client.gui.Click click, double dx, double dy) {
        if (dragging == null) return super.mouseDragged(click, dx, dy);
        float mx = (float) Mc.mx(click), my = (float) Mc.my(click);
        int[] s = dragging.size.get();
        float nx = mx - offX, ny = my - offY;
        guides.clear();
        if (snap) {
            float[] xs = {4, width / 2f - s[0] / 2f, width - s[0] - 4};
            float[] ys = {4, height / 2f - s[1] / 2f, height - s[1] - 4};
            for (float v : xs) if (Math.abs(nx - v) < 5) { nx = v; guides.add(new float[]{0, v == xs[1] ? width / 2f : v == 4 ? 4 : width - 4}); }
            for (float v : ys) if (Math.abs(ny - v) < 5) { ny = v; guides.add(new float[]{1, v == ys[1] ? height / 2f : v == 4 ? 4 : height - 4}); }
            for (El o : els) {   // align with other elements' edges
                if (o == dragging || !o.on.getAsBoolean()) continue;
                int[] os = o.size.get(); float ox = o.gx.getAsInt(), oy = o.gy.getAsInt();
                for (float v : new float[]{ox, ox + os[0] - s[0]}) if (Math.abs(nx - v) < 4) { nx = v; guides.add(new float[]{0, v == ox ? ox : ox + os[0]}); }
                for (float v : new float[]{oy + os[1] + 4, oy - s[1] - 4, oy}) if (Math.abs(ny - v) < 4) { ny = v; guides.add(new float[]{1, v}); }
            }
        }
        nx = Math.max(0, Math.min(width - s[0], nx)); ny = Math.max(0, Math.min(height - s[1], ny));
        dragging.sx.accept(Math.round(nx)); dragging.sy.accept(Math.round(ny));
        return true;
    }
    @Override protected void onMouseUp(float mx, float my, int b) {
        if (dragging != null) dragging.save.run();
        dragging = null; guides.clear();
    }
    @Override protected boolean onKey(int key, int mods) {
        if (selected == null) return false;
        int step = (mods & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1, dx = 0, dy = 0;
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> dx = -step; case GLFW.GLFW_KEY_RIGHT -> dx = step;
            case GLFW.GLFW_KEY_UP -> dy = -step; case GLFW.GLFW_KEY_DOWN -> dy = step;
            default -> { return false; }
        }
        int[] s = selected.size.get();
        selected.sx.accept(Math.max(0, Math.min(width - s[0], selected.gx.getAsInt() + dx)));
        selected.sy.accept(Math.max(0, Math.min(height - s[1], selected.gy.getAsInt() + dy)));
        selected.save.run();
        return true;
    }
    @Override public boolean shouldPause() { return false; }
}
