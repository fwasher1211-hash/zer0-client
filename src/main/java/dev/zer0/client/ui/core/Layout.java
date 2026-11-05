package dev.zer0.client.ui.core;

import dev.zer0.client.config.Zer0Settings;

import java.util.List;

/**
 * The five layout presets. Each one changes structure, not just colours: navigation style, panel
 * treatment, density, type scale, home page and animation feel. Switching rebuilds the hub instantly.
 */
public final class Layout {
    public enum Nav { SIDEBAR, RAIL, TOPBAR }
    public enum Panels { GLASS, NONE, SOLID }
    public enum Home { SIMPLE, DASHBOARD, HERO }

    public final String id, name, blurb;
    public final Nav nav;
    public final Panels panels;
    public final Home home;
    public final float sidebarW, pad, gap, rowH, radius, typeScale, animScale, maxContentW;
    public final boolean shadows, twoColumn;

    private Layout(String id, String name, String blurb, Nav nav, Panels panels, Home home, float sidebarW, float pad,
                   float gap, float rowH, float radius, float typeScale, float animScale, float maxContentW,
                   boolean shadows, boolean twoColumn) {
        this.id = id; this.name = name; this.blurb = blurb; this.nav = nav; this.panels = panels; this.home = home;
        this.sidebarW = sidebarW; this.pad = pad; this.gap = gap; this.rowH = rowH; this.radius = radius;
        this.typeScale = typeScale; this.animScale = animScale; this.maxContentW = maxContentW;
        this.shadows = shadows; this.twoColumn = twoColumn;
    }

    public static final Layout MINIMAL = new Layout("minimal", "Minimal",
            "Top navigation, no panels, generous spacing, simple cards.",
            Nav.TOPBAR, Panels.NONE, Home.SIMPLE, 0, 22, 12, 32, 10, 1.0f, 0.9f, 560, false, false);
    public static final Layout GLASS = new Layout("glass", "Glass",
            "Frosted sidebar and translucent panels with soft shadows.",
            Nav.SIDEBAR, Panels.GLASS, Home.SIMPLE, 132, 16, 8, 30, 12, 1.0f, 1.0f, 640, true, false);
    public static final Layout COMPACT = new Layout("compact", "Compact",
            "Icon rail, smaller rows and two columns when there is room.",
            Nav.RAIL, Panels.SOLID, Home.SIMPLE, 44, 10, 5, 22, 8, 0.85f, 1.2f, 900, false, true);
    public static final Layout DASHBOARD = new Layout("dashboard", "Dashboard",
            "Sidebar plus a live statistics dashboard as the home page.",
            Nav.SIDEBAR, Panels.GLASS, Home.DASHBOARD, 132, 14, 8, 28, 12, 1.0f, 1.0f, 820, true, false);
    public static final Layout PREMIUM = new Layout("premium", "Premium",
            "Large type, wide spacing, a hero header and slower, elegant motion.",
            Nav.SIDEBAR, Panels.GLASS, Home.HERO, 156, 26, 12, 38, 16, 1.22f, 0.75f, 620, true, false);

    public static final List<Layout> ALL = List.of(MINIMAL, GLASS, COMPACT, DASHBOARD, PREMIUM);

    public static Layout current() {
        String id = Zer0Settings.get().layout;
        for (Layout l : ALL) if (l.id.equals(id)) return l;
        return GLASS;
    }
}
