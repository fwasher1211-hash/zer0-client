package dev.zer0.client.ui.page;

import dev.zer0.client.ui.core.*;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Scrollable column (or two columns in the Compact layout) of section cards, with smooth momentum
 * scrolling, staggered fade/slide entrance and a slim auto-hiding scrollbar.
 */
public final class PageView {
    private List<Section> sections = List.of();
    private float scroll, target, contentH;
    private final Anim.Timeline enter = new Anim.Timeline(0.42f);
    private float x, y, w, h;
    private double lastScrollAt = -10;
    private final List<float[]> cardRects = new ArrayList<>();

    public void set(List<Section> s, boolean resetScroll) {
        sections = s;
        if (resetScroll) { scroll = target = 0; enter.restart(); }
    }
    public List<Section> sections() { return sections; }
    public void bounds(float x, float y, float w, float h) { this.x = x; this.y = y; this.w = w; this.h = h; }
    public boolean over(float mx, float my) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    private boolean twoCol() { return Layout.current().twoColumn && w > 520; }

    public void render(DrawContext g, float mx, float my) {
        Layout L = Layout.current();
        float maxScroll = Math.max(0, contentH - h + 16);
        target = Math.max(0, Math.min(target, maxScroll));
        scroll = Anim.approach(scroll, target, 18);
        boolean mouseIn = over(mx, my);
        float pmx = mouseIn ? mx : -9999, pmy = mouseIn ? my : -9999;

        g.enableScissor((int) Math.floor(x - 14), (int) Math.floor(y), (int) Math.ceil(x + w + 14), (int) Math.ceil(y + h));
        int cols = twoCol() ? 2 : 1;
        float colGap = 12, colW = (w - (cols - 1) * colGap) / cols;
        float[] colY = new float[cols];
        cardRects.clear();
        int idx = 0;
        for (Section s : sections) {
            if (!s.visible.getAsBoolean()) continue;
            List<Row> rows = new ArrayList<>();
            for (Row r : s.rows) if (r.visible.getAsBoolean()) rows.add(r);
            if (rows.isEmpty() && s.title.isEmpty()) continue;
            int c = 0; for (int i = 1; i < cols; i++) if (colY[i] < colY[c]) c = i;
            float cx = x + c * (colW + colGap), cy = y + colY[c] - scroll;

            float t = enter.delayed(idx * 0.045f);
            float ease = Anim.easeOutCubic(t), alphaMul = ease;
            float slide = (1 - ease) * 14;
            cy += slide;

            float start = cy;
            if (!s.title.isEmpty()) {
                Fonts.draw(g, Fonts.Role.HEADING, s.title.toUpperCase(), cx + 4, cy, Col.mulAlpha(Theme.textMuted, alphaMul), 6.8f);
                cy += Fonts.px(Fonts.Role.HEADING, 6.8f) + 6;
            }
            float innerPad = s.bare ? 0 : (L.panels == Layout.Panels.NONE ? 2 : 12);
            float rowsH = 0;
            for (Row r : rows) rowsH += r.height(colW - innerPad * 2);
            float cardH = rowsH + (s.bare ? 0 : (L.panels == Layout.Panels.NONE ? 0 : 6));
            if (!s.bare && cy < y + h && cy + cardH > y) {
                switch (L.panels) {
                    case GLASS -> Render.glass(g, cx, cy, colW, cardH, L.radius, Col.mulAlpha(Theme.card, alphaMul), Col.mulAlpha(Theme.panelEdge, alphaMul), false);
                    case SOLID -> Render.rect(g, cx, cy, colW, cardH, L.radius, Col.mulAlpha(Theme.card, alphaMul));
                    default -> {}
                }
            }
            cardRects.add(new float[]{cx, cy, colW, cardH});
            float ry = cy + (s.bare || L.panels == Layout.Panels.NONE ? 0 : 3);
            for (int i = 0; i < rows.size(); i++) {
                Row r = rows.get(i);
                r.place(cx + innerPad, ry, colW - innerPad * 2);
                if (ry + r.h > y - 40 && ry < y + h + 40 && alphaMul > 0.02f) {
                    r.render(g, alphaMul > 0.9f ? pmx : -9999, alphaMul > 0.9f ? pmy : -9999);
                    if (i < rows.size() - 1 && !s.bare && r.h > 0)
                        g.fill(Math.round(cx + innerPad), Math.round(ry + r.h), Math.round(cx + colW - innerPad), Math.round(ry + r.h) + 1,
                                Col.mulAlpha(Theme.divider, alphaMul));
                }
                ry += r.h;
            }
            float end = ry + (s.bare || L.panels == Layout.Panels.NONE ? 0 : 3);
            if (s.footer != null) {
                Fonts.draw(g, Fonts.Role.UI, s.footer, cx + 4, end + 4, Col.mulAlpha(Theme.textFaint, alphaMul), 6.8f);
                end += Fonts.px(Fonts.Role.UI, 6.8f) + 6;
            }
            colY[c] += (end - start) + L.gap + 10;
            idx++;
        }
        g.disableScissor();
        float tall = 0; for (float v : colY) tall = Math.max(tall, v);
        contentH = tall;

        // scrollbar: appears while scrolling or hovering the right edge
        if (maxScroll > 1) {
            boolean near = mouseIn && mx > x + w - 10;
            float vis = (float) Math.max(near ? 1 : 0, 1 - (Anim.seconds() - lastScrollAt - 0.8) / 0.4);
            vis = Anim.clamp01(vis);
            if (vis > 0) {
                float th = Math.max(24, h * h / (contentH + 16)), ty = y + (h - th) * (scroll / maxScroll);
                Render.rect(g, x + w + 5, ty, 3, th, 1.5f, Col.mulAlpha(Theme.textFaint, vis));
            }
        }
    }

    public boolean mouseDown(float mx, float my, int b) {
        if (!over(mx, my)) return false;
        for (Section s : sections) {
            if (!s.visible.getAsBoolean()) continue;
            for (Row r : s.rows) if (r.visible.getAsBoolean() && r.h > 0 && my >= r.y && my < r.y + r.h && r.mouseDown(mx, my, b)) return true;
        }
        return false;
    }
    public boolean scroll(float mx, float my, double amount) {
        if (!over(mx, my)) return false;
        for (Section s : sections) {
            if (!s.visible.getAsBoolean()) continue;
            for (Row r : s.rows) if (r.visible.getAsBoolean() && my >= r.y && my < r.y + r.h && r.scroll(mx, my, amount)) return true;
        }
        target -= (float) amount * 28;
        lastScrollAt = Anim.seconds();
        return true;
    }
    public void scrollBy(float d) { target += d; lastScrollAt = Anim.seconds(); }
}
