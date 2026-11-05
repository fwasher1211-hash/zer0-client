package dev.zer0.client.ui.pages;

import com.example.donutqueue.ModConfig;
import com.example.donutqueue.Zer0Bridge;
import dev.zer0.client.compat.Mc;
import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.ui.core.*;
import dev.zer0.client.ui.page.*;
import dev.zer0.client.ui.screens.HudEditorScreen;
import dev.zer0.client.ui.widgets.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

import static dev.zer0.client.ui.pages.Ctl.*;

/** Client settings: General, Appearance, Layouts, Backgrounds, Fonts, HUD, Animations, Performance, Configurations, About. */
final class ClientPages {
    private ClientPages() {}
    static final Zer0Settings DEF = new Zer0Settings();

    // ================================================================== GENERAL
    static final class General extends Page {
        General() { super("general", "CLIENT", "General", "Menus, sounds and behaviour", "G"); }
        @Override protected void build(List<Section> out) {
            Zer0Settings z = z();
            Section s = section(out, "Main menu");
            s.setting("ZER0 main menu", "Replace Minecraft's title screen. The classic one stays one click away.",
                    zToggle(() -> z.customTitle, v -> z.customTitle = v));
            TextField addr = new TextField("server address").max(60).value(z.playAddress);
            addr.onChange = v -> { z.playAddress = v.trim(); Zer0Settings.touch(); };
            s.setting("Play button joins", "Server the big Play button connects to. Empty = open the server list.", addr.size(150, 20));
            Section b = section(out, "Behaviour");
            b.setting("Show the game behind menus", "In a world, menus are frosted glass over the (blurred) game.", zToggle(() -> z.worldBehindMenus, v -> z.worldBehindMenus = v));
            b.setting("Interface sounds", "Soft click on buttons and toggles.", zToggle(() -> z.uiSounds, v -> z.uiSounds = v));
            b.setting("Tooltips", "Explanations when you hover settings.", zToggle(() -> z.tooltips, v -> z.tooltips = v));
            b.setting("Confirm destructive actions", "Clear, delete and reset need a second click.", zToggle(() -> z.confirmDangerous, v -> z.confirmDangerous = v));
            Section k = section(out, "Keys");
            k.note(() -> "Hub: " + Zer0Bridge.keyLabel("MENU") + "   \u00B7   Bidding: " + Zer0Bridge.keyLabel("BID") + "   \u00B7   Skip: " + Zer0Bridge.keyLabel("SKIP")
                    + "   \u00B7   Lever pause: " + Zer0Bridge.keyLabel("LEVER") + ".  Spotify play/pause, next and previous can be bound too.");
            k.buttons(btn("Open key bindings", () -> Mc.mc().setScreen(Mc.keybindsScreen(Mc.mc().currentScreen))));
            Section sb = section(out, "Sidebar");
            sb.note("Hide streamer pages you don't use. Everything keeps working; the page just leaves the sidebar.");
            for (String id : List.of("queue", "startbal", "bidding", "auctions", "info", "earnings", "sessions", "payments", "chat", "ranks")) {
                sb.setting(Pages.titleOf(id), "", zToggle(() -> !z.hiddenPages.contains(id), v -> { if (v) z.hiddenPages.remove(id); else if (!z.hiddenPages.contains(id)) z.hiddenPages.add(id); }));
            }
        }
    }

    // ================================================================== APPEARANCE
    static final class Appearance extends Page {
        Appearance() { super("appearance", "CLIENT", "Appearance", "Theme, accent colour and transparency", "A"); }
        @Override protected void build(List<Section> out) {
            Zer0Settings z = z();
            Section s = section(out, "Theme");
            s.setting("Mode", "", new Segmented(List.of(new Dropdown.Opt("dark", "Dark"), new Dropdown.Opt("light", "Light")),
                    () -> z.dark ? "dark" : "light", v -> { z.dark = v.equals("dark"); Zer0Settings.touch(); }).size(130, 20));
            s.setting("Accent colour", "Highlights, toggles, sliders and selection.", new ColorPicker(() -> z.accent, v -> { z.accent = v; Zer0Settings.touch(); }),
                    () -> { z.accent = DEF.accent; Zer0Settings.touch(); }, () -> z.accent != DEF.accent);
            s.setting("Interface transparency", "How see-through panels and buttons are.", zSlider(0.3, 1, 0.05, () -> (double) z.uiAlpha, v -> z.uiAlpha = v.floatValue(), pct()),
                    () -> { z.uiAlpha = DEF.uiAlpha; Zer0Settings.touch(); }, () -> z.uiAlpha != DEF.uiAlpha);
            Section bl = section(out, "Glass");
            Slider blur = new Slider(0, 10, 1, () -> (double) Mc.menuBlur(), v -> Mc.setMenuBlur((int) Math.round(v)), v -> String.valueOf((int) Math.round(v)));
            bl.setting("Background blur", "Blur of the world behind in-game menus (Minecraft's Menu Background Blur).", blur);
            bl.setting("Glass HUD boxes", "Draw the streamer HUD boxes as rounded frosted glass.", zToggle(() -> z.glassHud, v -> z.glassHud = v));
            Section bg = section(out, "Background");
            bg.add(new Row.Custom(70, (g, x, y, w, h, mx, my) -> {
                Backgrounds.drawThumb(g, z.background, Math.round(x), Math.round(y + 6), 104, 58);
                Render.stroke(g, x, y + 6, 104, 58, 8, 0.5f, Theme.panelEdge);
                Fonts.draw(g, Fonts.Role.UI, Backgrounds.def(z.background).name(), x + 116, y + 18, Theme.text, 9f);
                Fonts.draw(g, Fonts.Role.UI, "Blur, brightness, opacity and custom images on the Backgrounds page.", x + 116, y + 32, Theme.textMuted, 7f);
            }).titled("Background", "current background"));
            bg.buttons(btn("Choose background \u2192", () -> hub.open("backgrounds")), btn("Layouts \u2192", () -> hub.open("layouts")), btn("Fonts \u2192", () -> hub.open("fonts")));
        }
    }

    // ================================================================== LAYOUTS
    static final class Layouts extends Page {
        Layouts() { super("layouts", "CLIENT", "Layouts", "Five complete interface layouts - switch instantly", "L"); }
        @Override protected void build(List<Section> out) {
            Section s = section(out, "").bare();
            float tileH = 132;
            s.add(tiles(Layout.ALL.size(), 170, tileH, i -> (g, x, y, w, h, mx, my) -> {
                Layout l = Layout.ALL.get(i);
                boolean sel = Layout.current() == l, hov = mx >= x && mx < x + w && my >= y && my < y + h;
                Render.glass(g, x, y, w, h, 14, Col.mix(Theme.card, Theme.cardHover, hov ? 1 : 0), sel ? Theme.accent : Theme.panelEdge, true);
                if (sel) Render.stroke(g, x - 1.5f, y - 1.5f, w + 3, h + 3, 15.5f, 1.2f, Theme.accent);
                miniature(g, l, x + 10, y + 10, w - 20, 66);
                Fonts.draw(g, Fonts.Role.HEADING, l.name, x + 12, y + 84, Theme.text, 10);
                if (sel) {
                    float bw = Fonts.width(Fonts.Role.UI, "Active", 6.5f) + 12;
                    Render.rect(g, x + w - bw - 10, y + 84, bw, 12, 6, Theme.accent);
                    Fonts.drawCentered(g, Fonts.Role.UI, "Active", x + w - bw / 2 - 10, y + 84 + (12 - Fonts.px(Fonts.Role.UI, 6.5f)) / 2f, Theme.accentText, 6.5f);
                }
                List<String> lines = Fonts.wrap(Fonts.Role.UI, l.blurb, 7f, w - 24);
                float ly = y + 100;
                for (String line : lines) { Fonts.draw(g, Fonts.Role.UI, line, x + 12, ly, Theme.textMuted, 7f); ly += 10; }
            }).click((x, y, w, h, mx, my, b) -> {
                int n = Layout.ALL.size(), cols = Math.max(1, Math.min(n, (int) ((w + 8) / (170 + 8))));
                float tw = (w - (cols - 1) * 8) / cols;
                int i = (int) ((my - y - 2) / (tileH + 8)) * cols + (int) ((mx - x) / (tw + 8));
                if (i < 0 || i >= n) return false;
                z().layout = Layout.ALL.get(i).id; Zer0Settings.touch(); Sounds.click();
                Ui.toast(Layout.ALL.get(i).name + " layout applied");
                Pages.rebuildAll();
                return true;
            }));
        }
        /** Schematic thumbnail of each layout. */
        private static void miniature(DrawContext g, Layout l, float x, float y, float w, float h) {
            Render.rect(g, x, y, w, h, 9, Theme.dark ? 0x40000000 : 0x14000020);
            int line = Theme.dark ? 0x40FFFFFF : 0x4014141B, card = Theme.dark ? 0x1EFFFFFF : 0x90FFFFFF;
            switch (l.nav) {
                case SIDEBAR -> {
                    float sw = l == Layout.PREMIUM ? w * 0.3f : w * 0.26f;
                    Render.rect(g, x + 4, y + 4, sw, h - 8, 6, card);
                    for (int i = 0; i < 5; i++) Render.rect(g, x + 9, y + 14 + i * 8, sw - 14, 3, 1.5f, i == 1 ? Theme.accent : line);
                    float cx = x + sw + 9, cw = w - sw - 13;
                    if (l.home == Layout.Home.DASHBOARD) {
                        for (int i = 0; i < 3; i++) Render.rect(g, cx + i * (cw / 3), y + 6, cw / 3 - 3, 16, 4, card);
                        Render.rect(g, cx, y + 26, cw - 3, h - 32, 5, card);
                        Render.areaChart(g, cx + 4, y + 32, cw - 12, h - 42, new double[]{1, 3, 2, 5, 4, 7, 6, 9}, Theme.accent, true);
                    } else {
                        Render.rect(g, cx, y + 6, cw * (l == Layout.PREMIUM ? 0.7f : 0.5f), l == Layout.PREMIUM ? 7 : 5, 2.5f, Theme.text);
                        Render.rect(g, cx, y + 20, cw - 3, h - 26, 5, card);
                        for (int i = 0; i < 3; i++) Render.rect(g, cx + 5, y + 26 + i * 10, cw * 0.5f, 3, 1.5f, line);
                    }
                }
                case RAIL -> {
                    Render.rect(g, x + 4, y + 4, 10, h - 8, 4, card);
                    for (int i = 0; i < 6; i++) Render.rect(g, x + 6, y + 8 + i * 8, 6, 5, 2, i == 2 ? Theme.accent : line);
                    float cx = x + 18, cw = (w - 24) / 2;
                    for (int c = 0; c < 2; c++) for (int r = 0; r < 4; r++) Render.rect(g, cx + c * (cw + 3), y + 6 + r * 14, cw, 11, 3, card);
                }
                case TOPBAR -> {
                    for (int i = 0; i < 5; i++) Render.rect(g, x + w / 2 - 34 + i * 14, y + 6, 11, 4, 2, i == 0 ? Theme.accent : line);
                    Render.rect(g, x + w * 0.2f, y + 18, w * 0.25f, 4, 2, Theme.text);
                    for (int i = 0; i < 3; i++) Render.rect(g, x + w * 0.2f, y + 28 + i * 11, w * 0.6f, 7, 3, line);
                }
            }
        }
    }

    // ================================================================== BACKGROUNDS
    static final class BackgroundsPage extends Page {
        BackgroundsPage() { super("backgrounds", "CLIENT", "Backgrounds", "Premade backgrounds, custom images and fine tuning", "W"); }
        @Override protected void build(List<Section> out) {
            Zer0Settings z = z();
            Section s = section(out, "Choose").bare();
            float tileH = 96;
            List<Backgrounds.Def> defs = Backgrounds.ALL;
            s.add(tiles(defs.size(), 132, tileH, i -> (g, x, y, w, h, mx, my) -> {
                Backgrounds.Def d = defs.get(i);
                boolean sel = d.id().equals(z.background), prev = d.id().equals(Backgrounds.previewId);
                boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
                float th = h - 22;
                Render.shadow(g, x, y, w, th, 10, hov ? 9 : 5, Theme.shadow);
                Backgrounds.drawThumb(g, d.id(), Math.round(x), Math.round(y), Math.round(w), Math.round(th));
                if (sel || prev) Render.stroke(g, x - 1.5f, y - 1.5f, w + 3, th + 3, 11.5f, 1.3f, prev ? 0xFFFFFFFF : Theme.accent);
                if (d.animated()) {
                    Render.rect(g, x + 6, y + 6, 40, 12, 6, 0x90000000);
                    Fonts.draw(g, Fonts.Role.UI, "Animated", x + 11, y + 6 + (12 - Fonts.px(Fonts.Role.UI, 6.2f)) / 2f, 0xFFFFFFFF, 6.2f);
                }
                Fonts.draw(g, Fonts.Role.UI, d.name(), x + 2, y + th + 7, sel ? Theme.text : Theme.textMuted, 8f);
                if (sel) Render.circle(g, x + w - 5, y + th + 11, 2.5f, Theme.accent);
            }).click((x, y, w, h, mx, my, b) -> {
                int n = defs.size(), cols = Math.max(1, Math.min(n, (int) ((w + 8) / (132 + 8))));
                float tw = (w - (cols - 1) * 8) / cols;
                int i = (int) ((my - y - 2) / (tileH + 8)) * cols + (int) ((mx - x) / (tw + 8));
                if (i < 0 || i >= n) return false;
                String id = defs.get(i).id();
                Backgrounds.previewId = id.equals(z.background) ? null : id;
                Sounds.click();
                return true;
            }));
            s.footer("Click a background to preview it behind this menu, then Apply or Cancel at the bottom.");

            Section t = section(out, "Adjust");
            t.setting("Softness", "How blurred and diffuse the generated backgrounds are.", zSlider(0, 1, 0.05, () -> (double) z.bgSoftness, v -> z.bgSoftness = v.floatValue(), pct()),
                    () -> { z.bgSoftness = DEF.bgSoftness; Zer0Settings.touch(); }, () -> z.bgSoftness != DEF.bgSoftness);
            t.setting("Brightness", "", zSlider(0.3, 1.5, 0.05, () -> (double) z.bgBrightness, v -> z.bgBrightness = v.floatValue(), pct()),
                    () -> { z.bgBrightness = DEF.bgBrightness; Zer0Settings.touch(); }, () -> z.bgBrightness != DEF.bgBrightness);
            t.setting("Opacity", "How much of the background covers the game in in-game menus.", zSlider(0, 1, 0.05, () -> (double) z.bgOpacity, v -> z.bgOpacity = v.floatValue(), pct()),
                    () -> { z.bgOpacity = DEF.bgOpacity; Zer0Settings.touch(); }, () -> z.bgOpacity != DEF.bgOpacity);
            Slider blur = new Slider(0, 10, 1, () -> (double) Mc.menuBlur(), v -> Mc.setMenuBlur((int) Math.round(v)), v -> String.valueOf((int) Math.round(v)));
            t.setting("World blur", "Minecraft's menu blur behind in-game menus.", blur);

            Section c = section(out, "Custom image");
            TextField path = new TextField("C:\\Users\\you\\Pictures\\wallpaper.png").max(400).value(z.customBgPath);
            path.onChange = v -> { z.customBgPath = v.trim(); Zer0Settings.touch(); Backgrounds.invalidateThumb("custom"); Backgrounds.refresh(); };
            c.wide("Image file", "PNG or JPG", path);
            c.buttons(btn("Browse\u2026", () -> Mc.pickImage(p -> { if (p != null) { path.value(p); path.onChange.accept(p); } })),
                    btn("Use custom image", () -> { z.background = "custom"; Backgrounds.previewId = null; Zer0Settings.touch(); }).selected(() -> "custom".equals(z.background)));
            c.note(Backgrounds::customError);
        }
    }

    // ================================================================== FONTS
    static final class FontsPage extends Page {
        FontsPage() { super("fonts", "CLIENT", "Fonts", "Typefaces, size and weight for every part of the UI", "F"); }
        @Override protected void build(List<Section> out) {
            Zer0Settings z = z();
            Section r = section(out, "Typefaces");
            r.setting("Interface", "Labels, descriptions, lists.", fontPicker(() -> z.fontUi, v -> z.fontUi = v));
            r.setting("Headings", "Page titles and section headers.", fontPicker(() -> z.fontHeading, v -> z.fontHeading = v));
            r.setting("Buttons", "", fontPicker(() -> z.fontButton, v -> z.fontButton = v));
            r.setting("HUD", "Spotify player and HUD text.", fontPicker(() -> z.fontHud, v -> z.fontHud = v));
            Section w = section(out, "Size & weight");
            w.setting("Text size", "Scales all ZER0 text.", zSlider(0.8, 1.3, 0.05, () -> (double) z.fontScale, v -> z.fontScale = v.floatValue(), pct()),
                    () -> { z.fontScale = 1f; Zer0Settings.touch(); }, () -> z.fontScale != 1f);
            w.setting("Bold interface text", "", zToggle(() -> z.boldUi, v -> z.boldUi = v));
            w.setting("Bold headings", "", zToggle(() -> z.boldHeading, v -> z.boldHeading = v));
            w.setting("Bold buttons", "", zToggle(() -> z.boldButton, v -> z.boldButton = v));
            w.setting("Bold HUD", "", zToggle(() -> z.boldHud, v -> z.boldHud = v));
            w.footer("Variable fonts render at their Regular weight; Bold uses Minecraft's bold rendering.");
            Section p = section(out, "Live preview");
            p.add(new Row.Custom(118, (g, x, y, ww, h, mx, my) -> {
                Fonts.draw(g, Fonts.Role.HEADING, "The quick brown fox", x, y + 8, Theme.text, 17);
                Fonts.draw(g, Fonts.Role.UI, "Interface text reads like this - clear at every size. 0123456789 $50M", x, y + 34, Theme.textMuted, 8.5f);
                Button demo = new Button("Frosted button", () -> {});
                demo.place(x, y + 52, Math.min(130, ww), 22); demo.render(g, mx, my);
                Render.rect(g, x, y + 82, Math.min(200, ww), 30, 10, 0xA0141418);
                Fonts.draw(g, Fonts.Role.HUD, "HUD \u00B7 Queue: Forsurewater $50M", x + 10, y + 82 + (30 - Fonts.px(Fonts.Role.HUD, 8f)) / 2f, 0xFFFFFFFF, 8f);
            }));
            Section a = section(out, "Installed fonts");
            a.note(() -> {
                StringBuilder sb = new StringBuilder();
                for (var d : Fonts.available()) { if (sb.length() > 0) sb.append(", "); sb.append(d.name()); }
                int missing = Fonts.CATALOG.size() - Fonts.available().size();
                return sb + (missing > 0 ? ".  " + missing + " font(s) were not downloaded at build time and are hidden." : ".  All bundled under the SIL Open Font License / Apache 2.0.");
            });
            a.buttons(btn("Reset fonts", () -> { z.fontUi = z.fontHeading = z.fontButton = z.fontHud = DEF.fontUi; z.boldUi = DEF.boldUi; z.boldHeading = DEF.boldHeading; z.boldButton = DEF.boldButton; z.boldHud = DEF.boldHud; z.fontScale = 1f; Zer0Settings.touch(); }).kind(Button.Kind.GHOST));
        }
        private static Dropdown fontPicker(java.util.function.Supplier<String> get, java.util.function.Consumer<String> set) {
            Dropdown d = new Dropdown(() -> {
                List<Dropdown.Opt> o = new ArrayList<>();
                for (var f : Fonts.available()) o.add(new Dropdown.Opt(f.id(), f.name()));
                return o;
            }, get, v -> { set.accept(v); Zer0Settings.touch(); });
            d.render((g, o, x, y, w, h, color) -> Fonts.drawWith(g, o.id(), false, o.label(), x, y + (h - 8.5f) / 2f, color, 8.5f));
            d.prefW = 150;
            return d;
        }
    }

    // ================================================================== HUD
    static final class Hud extends Page {
        Hud() { super("hud", "CLIENT", "HUD", "Position and style every on-screen element", "U"); }
        @Override protected void build(List<Section> out) {
            Zer0Settings z = z();
            ModConfig c = Zer0Bridge.cfg();
            Section e = section(out, "Editor");
            e.add(new Row.Custom(60, (g, x, y, w, h, mx, my) -> {
                Fonts.draw(g, Fonts.Role.HEADING, "HUD Editor", x, y + 10, Theme.text, 12);
                Fonts.draw(g, Fonts.Role.UI, "Drag boxes on top of the real game, with snapping and guides.", x, y + 28, Theme.textMuted, 7.5f);
            }));
            e.buttons(btn("Open HUD editor", () -> Mc.mc().setScreen(new HudEditorScreen(Mc.mc().currentScreen, null))).selected(() -> true));
            Section st = section(out, "Style");
            st.setting("Glass boxes", "Rounded frosted backgrounds for the streamer boxes (the content is unchanged).", zToggle(() -> z.glassHud, v -> z.glassHud = v));
            st.setting("HUD font", "", FontsPage.fontPicker(() -> z.fontHud, v -> z.fontHud = v));
            Section el = section(out, "Elements");
            el.setting("Queue", "", cfgToggle(() -> c.hudEnabled, v -> c.hudEnabled = v));
            el.setting("Starting Bal", "", cfgToggle(() -> c.startBalBoxEnabled, v -> c.startBalBoxEnabled = v));
            el.setting("Bidding", "", cfgToggle(() -> c.biddingBoxEnabled, v -> c.biddingBoxEnabled = v));
            el.setting("Info Display", "", cfgToggle(() -> c.infoBoxEnabled, v -> c.infoBoxEnabled = v));
            el.setting("Earnings", "", cfgToggle(() -> c.earningsBoxEnabled, v -> c.earningsBoxEnabled = v));
            el.setting("Spotify", "", zToggle(() -> z.spotify.hudEnabled, v -> z.spotify.hudEnabled = v));
            el.footer("Sizes and colours live on each feature's page.");
        }
    }

    // ================================================================== ANIMATIONS
    static final class Animations extends Page {
        Animations() { super("animations", "CLIENT", "Animations", "Motion, transitions and speed", "N"); }
        private final Anim.Timeline demo = new Anim.Timeline(0.9f);
        @Override protected void build(List<Section> out) {
            Zer0Settings z = z();
            Section s = section(out, "Motion");
            s.setting("Animations", "Turn every ZER0 animation on or off.", zToggle(() -> z.animations, v -> z.animations = v));
            s.setting("Speed", "", zSlider(0.4, 2.5, 0.05, () -> (double) z.animSpeed, v -> z.animSpeed = v.floatValue(), v -> String.format("%.2fx", v)),
                    () -> { z.animSpeed = 1f; Zer0Settings.touch(); }, () -> z.animSpeed != 1f);
            s.setting("Page transitions", "Fade and slide when switching pages.", zToggle(() -> z.pageTransitions, v -> z.pageTransitions = v));
            s.setting("Animated backgrounds", "Aurora, abstract and gradient backgrounds move.", zToggle(() -> z.animatedBackground, v -> z.animatedBackground = v));
            Section p = section(out, "Preview");
            p.add(new Row.Custom(64, (g, x, y, w, h, mx, my) -> {
                float t = demo.t();
                float e = Anim.easeOutBack(t);
                Render.rect(g, x, y + 30, w, 4, 2, Theme.dark ? 0x1AFFFFFF : 0x1414141B);
                float px = x + (w - 60) * e;
                Render.shadow(g, px, y + 18, 60, 28, 14, 6, Theme.shadow);
                Render.rect(g, px, y + 18, 60, 28, 14, Theme.accent);
                Fonts.drawCentered(g, Fonts.Role.BUTTON, "ZER0", px + 30, y + 18 + (28 - Fonts.px(Fonts.Role.BUTTON, 9f)) / 2f, Theme.accentText, 9f);
            }));
            p.buttons(btn("Replay", demo::restart));
        }
        @Override public void onOpen() { demo.restart(); }
    }

    // ================================================================== PERFORMANCE
    static final class Performance extends Page {
        Performance() { super("performance", "CLIENT", "Performance", "Keep the client light on slower PCs", "P"); }
        @Override protected void build(List<Section> out) {
            Zer0Settings z = z();
            Section s = section(out, "Backgrounds");
            s.setting("Quality", "Resolution backgrounds are rendered at.", new Segmented(List.of(new Dropdown.Opt("0", "Low"), new Dropdown.Opt("1", "Balanced"), new Dropdown.Opt("2", "High")),
                    () -> String.valueOf(z.bgQuality), v -> { z.bgQuality = Integer.parseInt(v); Zer0Settings.touch(); }).size(190, 20));
            s.setting("Animation frame rate", "Updates per second for animated backgrounds (rendered off the main thread).",
                    zSlider(5, 60, 1, () -> (double) z.bgFps, v -> z.bgFps = (int) Math.round(v), v -> (int) Math.round(v) + " fps"),
                    () -> { z.bgFps = DEF.bgFps; Zer0Settings.touch(); }, () -> z.bgFps != DEF.bgFps);
            s.setting("Animated backgrounds", "", zToggle(() -> z.animatedBackground, v -> z.animatedBackground = v));
            Section u = section(out, "Interface");
            u.setting("Animations", "", zToggle(() -> z.animations, v -> z.animations = v));
            u.setting("Glass HUD boxes", "", zToggle(() -> z.glassHud, v -> z.glassHud = v));
            u.note("ZER0 draws its UI with plain quads (no custom shaders), so it works with any graphics card and with Sodium/Iris. "
                    + "Backgrounds and album art are processed on background threads; the Spotify connection polls only while something shows it.");
        }
    }

    // ================================================================== CONFIGURATIONS
    static final class Configs extends Page {
        Configs() { super("configs", "CLIENT", "Configurations", "Profiles, resets and config files", "K"); }
        @Override protected void build(List<Section> out) {
            Section p = section(out, "Profiles");
            p.note("A profile stores the ZER0 look (theme, layout, background, fonts, HUD style). Your Spotify login is never saved into profiles.");
            TextField name = new TextField("Profile name").max(32);
            p.add(fieldWithButton("Save current", name, "Save", n -> {
                if (Zer0Settings.saveProfile(n)) { Ui.toast("Saved \u201C" + Zer0Settings.safe(n) + "\u201D"); rebuild(); } else Ui.toast("Type a name first", true);
            }));
            List<String> profiles = Zer0Settings.profiles();
            if (profiles.isEmpty()) p.note("No profiles saved yet.");
            for (String n : profiles)
                p.setting(n, "", new Button("Load", () -> { if (Zer0Settings.loadProfile(n)) { Ui.toast("Loaded " + n); Pages.rebuildAll(); } }));
            if (!profiles.isEmpty()) p.add(new Row.Custom(30, (g, x, y, w, h, mx, my) -> Fonts.draw(g, Fonts.Role.UI, "Delete a profile:", x, y + 11, Theme.textMuted, 7.5f)));
            for (String n : profiles) p.buttons(new Button("Delete " + n, () -> { Zer0Settings.deleteProfile(n); rebuild(); }).confirm().kind(Button.Kind.DANGER).text(7.5f));
            Section r = section(out, "Reset");
            r.setting("Reset ZER0 settings", "Look, layout, fonts and Spotify HUD back to defaults (keeps your Spotify login).",
                    new Button("Reset", () -> { Zer0Settings.resetAll(); Pages.rebuildAll(); Ui.toast("ZER0 settings reset"); }).confirm().kind(Button.Kind.DANGER));
            r.setting("Reset HUD positions", "Puts every HUD box back in its default place.", new Button("Reset", () -> {
                ModConfig d = Zer0Bridge.defaults(), c = Zer0Bridge.cfg();
                c.hudX = d.hudX; c.hudY = d.hudY; c.startBalBoxX = d.startBalBoxX; c.startBalBoxY = d.startBalBoxY; c.biddingBoxX = d.biddingBoxX; c.biddingBoxY = d.biddingBoxY;
                c.infoBoxX = d.infoBoxX; c.infoBoxY = d.infoBoxY; c.earningsBoxX = d.earningsBoxX; c.earningsBoxY = d.earningsBoxY; Zer0Bridge.save();
                Ui.toast("HUD positions reset");
            }).confirm().kind(Button.Kind.DANGER));
            Section f = section(out, "Files");
            f.note("ZER0: config/zer0client/settings.json  \u00B7  Streamer tools: config/donutqueue*.json (unchanged from the original mod).");
            f.buttons(btn("Open config folder", () -> Mc.openFolder(FabricLoader.getInstance().getConfigDir())));
        }
    }

    // ================================================================== ABOUT
    static final class About extends Page {
        About() { super("about", "CLIENT", "About ZER0", "Version, credits and licences", "Z"); }
        @Override protected void build(List<Section> out) {
            Section h = section(out, "").bare();
            h.add(new Row.Custom(96, (g, x, y, w, hh, mx, my) -> {
                Render.glass(g, x, y, w, hh, 18, Theme.card, Theme.panelEdge, true);
                float zw = Fonts.draw(g, Fonts.Role.HEADING, "ZER", x + 20, y + 20, Theme.text, 30);
                Fonts.draw(g, Fonts.Role.HEADING, "0", x + 20 + zw, y + 20, Theme.accent, 30);
                Fonts.draw(g, Fonts.Role.UI, "C L I E N T", x + 22, y + 58, Theme.textMuted, 8f);
                String v = FabricLoader.getInstance().getModContainer("zer0client").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("dev");
                Fonts.drawRight(g, Fonts.Role.UI, "Version " + v, x + w - 20, y + 24, Theme.textMuted, 8f);
                Fonts.drawRight(g, Fonts.Role.UI, "Minecraft 1.21.11 \u00B7 Fabric", x + w - 20, y + 38, Theme.textFaint, 7.5f);
            }));
            Section c = section(out, "Credits");
            c.note("Streamer features (queue, starting balance, bidding & auctions, info display, earnings, sessions, payment log, chat styling, rank colours, commands and HUD boxes) "
                    + "come from DonutSMP Streamer Utility's by Forsurewater, used under the MIT License. ZER0 CLIENT keeps that code unchanged and builds a new interface around it.");
            c.note("Fonts: Inter, Poppins, Montserrat, Manrope, Outfit, Roboto, Plus Jakarta Sans, DM Sans, Space Grotesk, Nunito, Sora, Lexend, Urbanist, Figtree, Rubik - SIL Open Font License / Apache 2.0.");
            c.note("Spotify is a trademark of Spotify AB. ZER0 uses the official Spotify Web API with your own app credentials; it is not affiliated with Spotify. "
                    + "Not affiliated with Mojang or Microsoft.");
            Section t = section(out, "Shortcuts");
            t.note("Ctrl+F search  \u00B7  Esc close / cancel preview  \u00B7  Page Up/Down scroll  \u00B7  Right-click a text box to clear it");
        }
    }
}
