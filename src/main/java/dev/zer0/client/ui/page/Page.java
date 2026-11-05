package dev.zer0.client.ui.page;

import dev.zer0.client.ui.screens.HubScreen;

import java.util.ArrayList;
import java.util.List;

/** One hub category. Pages build their content as sections of rows; rows hold live widgets bound to real settings. */
public abstract class Page {
    public final String id, title, subtitle, group, glyph;
    protected HubScreen hub;
    private List<Section> built;

    protected Page(String id, String group, String title, String subtitle, String glyph) {
        this.id = id; this.group = group; this.title = title; this.subtitle = subtitle; this.glyph = glyph;
    }

    /** Fill in sections. Called on open and whenever {@link #rebuild()} is requested. */
    protected abstract void build(List<Section> out);
    /** Pages can be hidden from the sidebar in General > Sidebar (they still work and stay searchable by key). */
    public boolean visible() { return !dev.zer0.client.ui.pages.Pages.hidden(id); }
    public void onOpen() {}
    public void onClose() {}

    public final List<Section> sections(HubScreen hub) {
        this.hub = hub;
        if (built == null) { built = new ArrayList<>(); build(built); }
        return built;
    }
    /** Re-create the page's rows (e.g. a list changed size). */
    public void rebuild() { built = null; if (hub != null) hub.pageChanged(this); }

    protected static Section section(List<Section> out, String title) { Section s = new Section(title); out.add(s); return s; }
}
