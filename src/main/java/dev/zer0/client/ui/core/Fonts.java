package dev.zer0.client.ui.core;

import dev.zer0.client.compat.Mc;
import dev.zer0.client.config.Zer0Settings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Typography. Fonts are real TrueType files rendered by Minecraft's own "ttf" font provider (crisp at any
 * size thanks to oversampling). Four independent roles: UI, heading, button and HUD.
 * Only fonts whose .ttf was actually bundled at build time are offered.
 */
public final class Fonts {
    private Fonts() {}

    public enum Role { UI, HEADING, BUTTON, HUD }
    public record Def(String id, String name, String vibe) {}

    public static final List<Def> CATALOG = List.of(
            new Def("minecraft", "Minecraft", "the game's own font"),
            new Def("inter", "Inter", "neutral, modern UI"),
            new Def("poppins", "Poppins", "geometric, friendly"),
            new Def("montserrat", "Montserrat", "wide, confident"),
            new Def("manrope", "Manrope", "clean, technical"),
            new Def("outfit", "Outfit", "rounded geometric"),
            new Def("roboto", "Roboto", "familiar, compact"),
            new Def("plusjakartasans", "Plus Jakarta Sans", "premium, editorial"),
            new Def("dmsans", "DM Sans", "soft, low contrast"),
            new Def("spacegrotesk", "Space Grotesk", "quirky, techy"),
            new Def("nunito", "Nunito", "very rounded"),
            new Def("sora", "Sora", "futuristic"),
            new Def("lexend", "Lexend", "highly readable"),
            new Def("urbanist", "Urbanist", "slim, elegant"),
            new Def("figtree", "Figtree", "warm, simple"),
            new Def("rubik", "Rubik", "chunky, playful"));

    private static List<Def> available;
    private static final Map<String, Style> styles = new LinkedHashMap<>();

    /** Fonts whose TTF is present (re-scanned after a resource reload). */
    public static List<Def> available() {
        if (available == null) {
            List<Def> out = new ArrayList<>();
            for (Def d : CATALOG)
                if (d.id.equals("minecraft") || Mc.resourceExists(Identifier.of("zer0client", "font/" + d.id + ".ttf"))) out.add(d);
            available = out;
        }
        return available;
    }
    public static void invalidate() { available = null; styles.clear(); cache.clear(); }
    public static boolean isAvailable(String id) { return available().stream().anyMatch(d -> d.id.equals(id)); }
    public static String name(String id) { return CATALOG.stream().filter(d -> d.id.equals(id)).map(Def::name).findFirst().orElse(id); }

    public static String fontOf(Role r) {
        Zer0Settings s = Zer0Settings.get();
        String id = switch (r) { case UI -> s.fontUi; case HEADING -> s.fontHeading; case BUTTON -> s.fontButton; case HUD -> s.fontHud; };
        return isAvailable(id) ? id : "minecraft";
    }
    public static boolean boldOf(Role r) {
        Zer0Settings s = Zer0Settings.get();
        return switch (r) { case UI -> s.boldUi; case HEADING -> s.boldHeading; case BUTTON -> s.boldButton; case HUD -> s.boldHud; };
    }

    // ------------------------------------------------------------------ text objects (small LRU cache: no per-frame garbage, no leak)
    private static final Map<String, Text> cache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Text> e) { return size() > 600; }
    };
    private static Style style(String fontId, boolean bold) {
        String k = fontId + (bold ? "#b" : "");
        Style st = styles.get(k);
        if (st == null) {
            st = fontId.equals("minecraft") ? Style.EMPTY : Mc.fontStyle(Identifier.of("zer0client", fontId));
            st = st.withBold(bold);
            styles.put(k, st);
        }
        return st;
    }
    public static Text text(String fontId, boolean bold, String s) {
        String k = fontId + (bold ? 1 : 0) + '\u0000' + s;
        Text t = cache.get(k);
        if (t == null) { t = Text.literal(s).setStyle(style(fontId, bold)); cache.put(k, t); }
        return t;
    }

    // ------------------------------------------------------------------ measuring & drawing
    /** Base size multiplier: user font size x layout type scale. HUD ignores the layout. */
    private static float mult(Role r) {
        float m = Zer0Settings.get().fontScale;
        return r == Role.HUD ? m : m * Layout.current().typeScale;
    }
    private static float scaleFor(String fontId, float size) { return size / (fontId.equals("minecraft") ? 9f : 10f); }

    public static float width(Role r, String s, float size) { return widthWith(fontOf(r), boldOf(r), s, size * mult(r)); }
    public static float widthWith(String fontId, boolean bold, String s, float px) {
        return Mc.tr().getWidth(text(fontId, bold, s)) * scaleFor(fontId, px);
    }
    /** Final pixel size used for a role at a nominal size (useful for layout). */
    public static float px(Role r, float size) { return size * mult(r); }

    public static float draw(DrawContext g, Role r, String s, float x, float y, int color, float size) {
        return drawWith(g, fontOf(r), boldOf(r), s, x, y, color, size * mult(r));
    }
    public static float drawCentered(DrawContext g, Role r, String s, float cx, float y, int color, float size) {
        return draw(g, r, s, cx - width(r, s, size) / 2f, y, color, size);
    }
    public static float drawRight(DrawContext g, Role r, String s, float rx, float y, int color, float size) {
        return draw(g, r, s, rx - width(r, s, size), y, color, size);
    }
    /** Draws text whose glyph box is {@code px} tall at (x, y = top). Returns the advance width. */
    public static float drawWith(DrawContext g, String fontId, boolean bold, String s, float x, float y, int color, float px) {
        if (s == null || s.isEmpty() || Col.a(color) < 4) return 0;
        float sc = scaleFor(fontId, px);
        float d = Render.density(g);
        float sx = Math.round(x * d) / d, sy = Math.round(y * d) / d;   // snap to physical pixels = crisp
        Matrix3x2fStack m = g.getMatrices();
        m.pushMatrix();
        m.translate(sx, sy);
        m.scale(sc, sc);
        Text t = text(fontId, bold, s);
        g.drawText(Mc.tr(), t, 0, 0, color, false);
        m.popMatrix();
        return Mc.tr().getWidth(t) * sc;
    }

    /** Cuts {@code s} with an ellipsis so it fits {@code maxW}. */
    public static String fit(Role r, String s, float size, float maxW) {
        if (s == null) return "";
        if (width(r, s, size) <= maxW) return s;
        String e = "…";
        int lo = 0, hi = s.length();
        while (lo < hi) { int mid = (lo + hi + 1) / 2; if (width(r, s.substring(0, mid) + e, size) <= maxW) lo = mid; else hi = mid - 1; }
        return s.substring(0, lo) + e;
    }
    /** Word-wraps into lines no wider than maxW. */
    public static List<String> wrap(Role r, String s, float size, float maxW) {
        List<String> out = new ArrayList<>();
        for (String para : s.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String w : para.split(" ")) {
                String cand = line.length() == 0 ? w : line + " " + w;
                if (width(r, cand, size) > maxW && line.length() > 0) { out.add(line.toString()); line = new StringBuilder(w); }
                else line = new StringBuilder(cand);
            }
            out.add(line.toString());
        }
        return out;
    }
}
