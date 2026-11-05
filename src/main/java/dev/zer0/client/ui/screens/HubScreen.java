package dev.zer0.client.ui.screens;

import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.ui.core.*;
import dev.zer0.client.ui.page.*;
import dev.zer0.client.ui.pages.Pages;
import dev.zer0.client.ui.widgets.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The ZER0 Hub: every setting of the client and every feature of the original streamer mod, organised in
 * one sidebar (or rail / top bar, depending on layout) with search, animated page transitions and a
 * live background preview bar.
 */
public class HubScreen extends Zer0Screen {
    private final List<Page> pages = Pages.all();
    private Page current;
    private final PageView view = new PageView();
    private TextField search;
    private String lastQuery = "";
    private Button close, applyBg, cancelBg;
    private final Anim.Smooth navSel = new Anim.Smooth(-1, 16);
    private final Anim.Timeline pageAnim = new Anim.Timeline(0.35f);
    private String group = "STREAMER";
    // nav geometry cache (for clicks)
    private final List<float[]> navHits = new ArrayList<>();
    private final List<Page> navPages = new ArrayList<>();
    private float navScroll, navScrollTarget, navContentH;

    public HubScreen(Screen parent) { this(parent, null); }
    public HubScreen(Screen parent, String pageId) {
        super("ZER0 Hub", parent);
        Page p = null;
        if (pageId != null) p = byId(pageId);
        if (p == null) { int i = Zer0Settings.get().lastHubPage; if (i >= 0 && i < pages.size()) p = pages.get(i); }
        if (p == null || !p.visible()) p = pages.get(0);
        current = p;
        group = p.group;
    }

    private Page byId(String id) { for (Page p : pages) if (p.id.equals(id)) return p; return null; }
    public Page page() { return current; }

    @Override protected void build() {
        search = new TextField("Search settings").icon("search").max(40);
        search.onChange = q -> refreshSearch();
        search.value(lastQuery);
        close = new Button("Done", this::close).text(8f);
        close.kind(Button.Kind.FROST);
        applyBg = new Button("Apply", () -> { Zer0Settings.get().background = Backgrounds.previewId; Backgrounds.previewId = null; Zer0Settings.touch(); Ui.toast("Background applied"); }).kind(Button.Kind.ACCENT);
        cancelBg = new Button("Cancel", () -> Backgrounds.previewId = null);
        show(current, true);
        refreshSearch();
    }

    // ------------------------------------------------------------------ navigation
    public void open(String id) { Page p = byId(id); if (p != null) open(p); }
    public void open(Page p) {
        if (p == current && lastQuery.isEmpty()) return;
        if (current != null) current.onClose();
        current = p; group = p.group;
        Zer0Settings.get().lastHubPage = pages.indexOf(p); Zer0Settings.touch();
        if (!lastQuery.isEmpty()) { lastQuery = ""; search.value(""); }
        show(p, true);
    }
    private void show(Page p, boolean animate) {
        p.onOpen();
        List<Section> s = p.sections(this);
        for (Section sec : s) for (Row r : sec.rows) r.page = p;
        view.set(s, animate);
        if (animate && Zer0Settings.get().pageTransitions) pageAnim.restart();
        if (Ui.overlay != null) Ui.overlay.close();
    }
    /** A page rebuilt its rows; keep scroll position. */
    public void pageChanged(Page p) {
        if (p == current && lastQuery.isEmpty()) {
            List<Section> s = p.sections(this);
            for (Section sec : s) for (Row r : sec.rows) r.page = p;
            view.set(s, false);
        }
    }

    private void refreshSearch() {
        String q = search.text.trim();
        if (q.equals(lastQuery)) return;
        lastQuery = q;
        if (q.isEmpty()) { show(current, true); return; }
        List<Section> results = new ArrayList<>();
        for (Page p : pages) {
            if (!p.visible()) continue;
            Section hit = null;
            for (Section s : p.sections(this)) for (Row r : s.rows) {
                r.page = p;
                if (r.searchable() && r.matches(q)) {
                    if (hit == null) { hit = new Section(p.title); results.add(hit); }
                    hit.add(r);
                }
            }
            if (hit == null && (p.title + " " + p.subtitle).toLowerCase().contains(q.toLowerCase())) {
                hit = new Section(p.title); results.add(hit);
            }
            if (hit != null) {
                Page target = p;
                hit.add(new Row.Buttons(new Button("Open " + p.title + "  →", () -> open(target)).kind(Button.Kind.GHOST).text(7.5f)));
            }
        }
        if (results.isEmpty()) results.add(new Section("").add(new Row.Note(() -> "Nothing matches \"" + q + "\". Try another word, e.g. colour, font, queue, spotify.")));
        view.set(results, true);
    }

    // ------------------------------------------------------------------ geometry
    private float navW() {
        Layout L = Layout.current();
        return switch (L.nav) { case SIDEBAR -> Math.min(L.sidebarW, width * 0.3f); case RAIL -> L.sidebarW; case TOPBAR -> 0; };
    }
    private float margin() { return width < 480 ? 6 : 10; }

    @Override protected void draw(DrawContext g, float mx, float my, float delta) {
        Layout L = Layout.current();
        float m = margin();
        float appear = Anim.easeOutCubic(opened.t());
        switch (L.nav) {
            case SIDEBAR -> drawSidebar(g, mx, my, m, appear);
            case RAIL -> drawRail(g, mx, my, m, appear);
            case TOPBAR -> drawTopbar(g, mx, my, m, appear);
        }
        // content area
        float left = L.nav == Layout.Nav.TOPBAR ? 0 : m + navW() + (L.nav == Layout.Nav.RAIL ? 8 : 12);
        float top = L.nav == Layout.Nav.TOPBAR ? topbarH : m + 4;
        float avail = width - left - m - 6;
        float cw = Math.min(avail, L.maxContentW);
        float cx = left + (avail - cw) / 2f;

        // header
        float hp = Anim.easeOutCubic(pageAnim.t());
        float hs = (L.home == Layout.Home.HERO && current.id.equals("home") && lastQuery.isEmpty()) ? 26 : 17;
        String title = lastQuery.isEmpty() ? current.title : "Search";
        String sub = lastQuery.isEmpty() ? current.subtitle : "Results for “" + lastQuery + "”";
        float hy = top + 6 + (1 - hp) * 6;
        Fonts.draw(g, Fonts.Role.HEADING, title, cx, hy, Col.mulAlpha(Theme.text, hp), hs);
        float subY = hy + Fonts.px(Fonts.Role.HEADING, hs) + 4;
        Fonts.draw(g, Fonts.Role.UI, Fonts.fit(Fonts.Role.UI, sub, 8f, cw - 70), cx, subY, Col.mulAlpha(Theme.textMuted, hp), 8f);
        close.place(cx + cw - 52, hy, 52, 20);
        close.render(g, mx, my);
        if (L.nav == Layout.Nav.RAIL || L.nav == Layout.Nav.TOPBAR) {   // search lives in the header for these layouts
            float sw = Math.min(170, cw * 0.4f);
            search.place(cx + cw - 52 - 8 - sw, hy, sw, 20);
            search.render(g, mx, my);
        }
        float viewTop = subY + Fonts.px(Fonts.Role.UI, 8f) + 14;
        view.bounds(cx, viewTop, cw, height - viewTop - m);
        view.render(g, mx, my);

        if (Backgrounds.previewId != null) drawPreviewBar(g, mx, my);
    }

    private void drawPreviewBar(DrawContext g, float mx, float my) {
        String name = Backgrounds.def(Backgrounds.previewId).name();
        float bw = 290, bh = 34, bx = (width - bw) / 2f, by = height - bh - 14;
        Render.glass(g, bx, by, bw, bh, 17, Theme.dark ? 0xE61A1A22 : 0xF0FFFFFF, Theme.panelEdge, true);
        Fonts.draw(g, Fonts.Role.UI, "Previewing", bx + 16, by + 7, Theme.textMuted, 7f);
        Fonts.draw(g, Fonts.Role.UI, Fonts.fit(Fonts.Role.UI, name, 8.5f, 110), bx + 16, by + 17, Theme.text, 8.5f);
        cancelBg.place(bx + bw - 136, by + 7, 60, 20); cancelBg.render(g, mx, my);
        applyBg.place(bx + bw - 70, by + 7, 60, 20); applyBg.render(g, mx, my);
    }

    // ---------------------------------------------------------------- sidebar (Glass / Dashboard / Premium)
    private void drawSidebar(DrawContext g, float mx, float my, float m, float appear) {
        Layout L = Layout.current();
        float sw = navW(), sx = m - (1 - appear) * 20, sy = m, sh = height - 2 * m;
        Render.glass(g, sx, sy, sw, sh, L.radius + 2, Col.mulAlpha(Theme.panel, appear), Col.mulAlpha(Theme.panelEdge, appear), L.shadows);
        // brand
        float by = sy + 14;
        float bs = L == Layout.PREMIUM ? 15 : 13;
        float zw = Fonts.draw(g, Fonts.Role.HEADING, "ZER", sx + 14, by, Theme.text, bs);
        Fonts.draw(g, Fonts.Role.HEADING, "0", sx + 14 + zw, by, Theme.accent, bs);
        Fonts.draw(g, Fonts.Role.UI, "CLIENT", sx + 14 + zw + Fonts.width(Fonts.Role.HEADING, "0", bs) + 5, by + Fonts.px(Fonts.Role.HEADING, bs) - Fonts.px(Fonts.Role.UI, 6.5f) - 1, Theme.textMuted, 6.5f);
        float y = by + Fonts.px(Fonts.Role.HEADING, bs) + 12;
        search.place(sx + 9, y, sw - 18, 20);
        search.render(g, mx, my);
        y += 30;

        navHits.clear(); navPages.clear();
        float listTop = y, listBottom = sy + sh - 10;
        navScroll = Anim.approach(navScroll, navScrollTarget, 18);
        g.enableScissor((int) sx, (int) listTop, (int) (sx + sw), (int) listBottom);
        float iy = listTop - navScroll;
        String lastGroup = null;
        float itemH = L == Layout.PREMIUM ? 21 : 18;
        int selIndex = -1; float selY = 0;
        for (Page p : pages) {
            if (!p.visible()) continue;
            if (!p.group.equals(lastGroup)) {
                if (lastGroup != null) iy += 6;
                Fonts.draw(g, Fonts.Role.UI, p.group, sx + 14, iy + 2, Theme.textFaint, 6.3f);
                iy += 13; lastGroup = p.group;
            }
            if (p == current && lastQuery.isEmpty()) { selIndex = navPages.size(); selY = iy; }
            navPages.add(p);
            navHits.add(new float[]{sx + 6, iy, sw - 12, itemH});
            iy += itemH + 1;
        }
        navContentH = iy + navScroll - listTop;
        // sliding selection pill
        if (selIndex >= 0) {
            if (navSel.v < 0) navSel.snap(selY + navScroll);
            float py = navSel.to(selY + navScroll) - navScroll;
            Render.rect(g, sx + 6, py, sw - 12, itemH, Math.min(itemH / 2, 8), Theme.dark ? 0x2EFFFFFF : 0xCCFFFFFF);
            Render.rect(g, sx + 6, py + 4, 2.5f, itemH - 8, 1.25f, Theme.accent);
        }
        for (int i = 0; i < navPages.size(); i++) {
            Page p = navPages.get(i);
            float[] r = navHits.get(i);
            boolean hov = mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3] && my >= listTop && my < listBottom;
            boolean sel = i == selIndex;
            if (hov && !sel) Render.rect(g, r[0], r[1], r[2], r[3], Math.min(r[3] / 2, 8), Theme.dark ? 0x14FFFFFF : 0x80FFFFFF);
            glyphChip(g, p, r[0] + 7, r[1] + (r[3] - 12) / 2f, 12, sel);
            Fonts.draw(g, Fonts.Role.UI, Fonts.fit(Fonts.Role.UI, p.title, 8f, r[2] - 30), r[0] + 25, r[1] + (r[3] - Fonts.px(Fonts.Role.UI, 8f)) / 2f,
                    sel ? Theme.text : Theme.textMuted, 8f);
        }
        g.disableScissor();
    }

    /** Small rounded colour chip with the page's letter (consistent, crisp icons without bitmap art). */
    private void glyphChip(DrawContext g, Page p, float x, float y, float s, boolean sel) {
        int hue = Math.abs(p.id.hashCode());
        int base = p.group.equals("STREAMER") ? Col.hsv((hue % 360) / 360f * 0.25f + 0.55f, 0.45f, 0.95f) : Col.hsv((hue % 360) / 360f * 0.3f + 0.72f, 0.4f, 0.95f);
        int c = sel ? Theme.accent : Col.withAlpha(base, Theme.dark ? 70 : 110);
        Render.rect(g, x, y, s, s, s * 0.32f, c);
        int fg = sel ? Theme.accentText : (Theme.dark ? 0xEFFFFFFF : 0xFF22222B);
        Fonts.drawCentered(g, Fonts.Role.UI, p.glyph, x + s / 2f, y + (s - Fonts.px(Fonts.Role.UI, 6.8f)) / 2f + 0.2f, fg, 6.8f);
    }

    // ---------------------------------------------------------------- rail (Compact)
    private void drawRail(DrawContext g, float mx, float my, float m, float appear) {
        Layout L = Layout.current();
        float sw = navW(), sx = m - (1 - appear) * 14, sy = m, sh = height - 2 * m;
        Render.rect(g, sx, sy, sw, sh, L.radius + 2, Col.mulAlpha(Theme.panel, appear));
        Fonts.drawCentered(g, Fonts.Role.HEADING, "0", sx + sw / 2f, sy + 10, Theme.accent, 13);
        navHits.clear(); navPages.clear();
        float listTop = sy + 32, listBottom = sy + sh - 6;
        navScroll = Anim.approach(navScroll, navScrollTarget, 18);
        g.enableScissor((int) sx, (int) listTop, (int) (sx + sw), (int) listBottom);
        float iy = listTop - navScroll; String lastGroup = null;
        for (Page p : pages) {
            if (!p.visible()) continue;
            if (lastGroup != null && !p.group.equals(lastGroup)) { g.fill(Math.round(sx + 10), Math.round(iy + 3), Math.round(sx + sw - 10), Math.round(iy + 3) + 1, Theme.divider); iy += 8; }
            lastGroup = p.group;
            boolean sel = p == current && lastQuery.isEmpty();
            float s = 24, ix = sx + (sw - s) / 2f;
            boolean hov = mx >= ix && mx < ix + s && my >= iy && my < iy + s && my >= listTop && my < listBottom;
            if (sel || hov) Render.rect(g, ix - 2, iy - 2, s + 4, s + 4, 9, sel ? (Theme.dark ? 0x2EFFFFFF : 0xCCFFFFFF) : 0x14FFFFFF);
            glyphChip(g, p, ix + 4, iy + 4, s - 8, sel);
            if (hov) Ui.hoverTip(p, p.title, mx, my);
            navPages.add(p); navHits.add(new float[]{ix - 2, iy - 2, s + 4, s + 4});
            iy += s + 4;
        }
        navContentH = iy + navScroll - listTop;
        g.disableScissor();
    }

    // ---------------------------------------------------------------- top bar (Minimal)
    private float topbarH = 74;
    private void drawTopbar(DrawContext g, float mx, float my, float m, float appear) {
        float y = 12 - (1 - appear) * 10;
        float zw = Fonts.draw(g, Fonts.Role.HEADING, "ZER", 18, y + 2, Theme.text, 12);
        Fonts.draw(g, Fonts.Role.HEADING, "0", 18 + zw, y + 2, Theme.accent, 12);
        // group switch, centred
        String[] groups = {"STREAMER", "CLIENT"};
        float gw = 0; for (String gr : groups) gw += Fonts.width(Fonts.Role.BUTTON, gr, 7.5f) + 26;
        float gx = (width - gw) / 2f;
        navHits.clear(); navPages.clear();
        for (String gr : groups) {
            boolean sel = gr.equals(group);
            float tw = Fonts.width(Fonts.Role.BUTTON, gr, 7.5f) + 20;
            boolean hov = mx >= gx && mx < gx + tw && my >= y && my < y + 18;
            if (sel || hov) Render.rect(g, gx, y, tw, 18, 9, sel ? (Theme.dark ? 0x30FFFFFF : 0xDDFFFFFF) : 0x12FFFFFF);
            Fonts.drawCentered(g, Fonts.Role.BUTTON, gr, gx + tw / 2f, y + (18 - Fonts.px(Fonts.Role.BUTTON, 7.5f)) / 2f, sel ? Theme.text : Theme.textMuted, 7.5f);
            navPages.add(null); navHits.add(new float[]{gx, y, tw, 18});
            gx += tw + 6;
        }
        // page chips of the group: wrapped into centred lines
        List<Page> list = new ArrayList<>();
        for (Page p : pages) if (p.visible() && p.group.equals(group)) list.add(p);
        List<List<Page>> lines = new ArrayList<>();
        List<Float> lineW = new ArrayList<>();
        float maxW = width - 40, cur = 0; List<Page> line = new ArrayList<>();
        for (Page p : list) {
            float tw = Fonts.width(Fonts.Role.UI, p.title, 7.5f) + 18;
            if (!line.isEmpty() && cur + tw > maxW) { lines.add(line); lineW.add(cur - 4); line = new ArrayList<>(); cur = 0; }
            line.add(p); cur += tw + 4;
        }
        if (!line.isEmpty()) { lines.add(line); lineW.add(cur - 4); }
        float cy = y + 26;
        for (int li = 0; li < lines.size(); li++) {
            float cx = (width - lineW.get(li)) / 2f;
            for (Page p : lines.get(li)) {
                float tw = Fonts.width(Fonts.Role.UI, p.title, 7.5f) + 18;
                boolean sel = p == current && lastQuery.isEmpty();
                boolean hov = mx >= cx && mx < cx + tw && my >= cy && my < cy + 16;
                if (sel) Render.rect(g, cx, cy, tw, 16, 8, Theme.accent);
                else if (hov) Render.rect(g, cx, cy, tw, 16, 8, Theme.dark ? 0x1AFFFFFF : 0x99FFFFFF);
                Fonts.drawCentered(g, Fonts.Role.UI, p.title, cx + tw / 2f, cy + (16 - Fonts.px(Fonts.Role.UI, 7.5f)) / 2f, sel ? Theme.accentText : Theme.textMuted, 7.5f);
                navPages.add(p); navHits.add(new float[]{cx, cy, tw, 16});
                cx += tw + 4;
            }
            cy += 20;
        }
        topbarH = cy + 6;
    }

    // ------------------------------------------------------------------ input
    @Override protected boolean onMouseDown(float mx, float my, int b) {
        if (Backgrounds.previewId != null && (applyBg.mouseDown(mx, my, b) || cancelBg.mouseDown(mx, my, b))) return true;
        if (close.mouseDown(mx, my, b)) return true;
        if (search.mouseDown(mx, my, b)) return true;
        for (int i = 0; i < navHits.size(); i++) {
            float[] r = navHits.get(i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                Page p = navPages.get(i);
                Sounds.click();
                if (p == null) {   // top-bar group switch
                    group = i == 0 ? "STREAMER" : "CLIENT";
                    for (Page q : pages) if (q.visible() && q.group.equals(group)) { open(q); break; }
                } else open(p);
                return true;
            }
        }
        return view.mouseDown(mx, my, b);
    }
    @Override protected boolean onScroll(float mx, float my, double amount) {
        if (mx < navW() + margin() + 4 && Layout.current().nav != Layout.Nav.TOPBAR) {
            float max = Math.max(0, navContentH - (height - 100));
            navScrollTarget = Math.max(0, Math.min(max, navScrollTarget - (float) amount * 24));
            return true;
        }
        return view.scroll(mx, my, amount);
    }
    @Override protected boolean onKey(int key, int mods) {
        if (key == GLFW.GLFW_KEY_F && (mods & GLFW.GLFW_MOD_CONTROL) != 0) { Ui.focused = search; return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE && Backgrounds.previewId != null) { Backgrounds.previewId = null; return true; }
        if (key == GLFW.GLFW_KEY_PAGE_DOWN) { view.scrollBy(120); return true; }
        if (key == GLFW.GLFW_KEY_PAGE_UP) { view.scrollBy(-120); return true; }
        return false;
    }
    @Override public void removed() {
        if (current != null) current.onClose();
        Backgrounds.previewId = null;
        super.removed();
    }
}
