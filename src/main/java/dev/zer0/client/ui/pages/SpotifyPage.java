package dev.zer0.client.ui.pages;

import dev.zer0.client.compat.Mc;
import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.spotify.SpotifyClient;
import dev.zer0.client.spotify.SpotifyHud;
import dev.zer0.client.ui.core.*;
import dev.zer0.client.ui.page.*;
import dev.zer0.client.ui.screens.HudEditorScreen;
import dev.zer0.client.ui.widgets.*;

import java.util.List;

import static dev.zer0.client.ui.pages.Ctl.*;

/** Spotify connection, a full player, and every Spotify HUD option. */
final class SpotifyPage extends Page {
    SpotifyPage() { super("spotify", "CLIENT", "Spotify", "Connect your account and design the music HUD", "♪"); }

    @Override protected void build(List<Section> out) {
        Zer0Settings.Spotify s = z().spotify;
        SpotifyClient c = SpotifyClient.get();

        Section con = section(out, "Account");
        con.add(new Row.Custom(40, (g, x, y, w, h, mx, my) -> {
            SpotifyClient.State st = c.state();
            int dot = switch (st) { case CONNECTED -> Theme.good; case CONNECTING -> Theme.warn; case ERROR -> Theme.bad; default -> Theme.textFaint; };
            String label = switch (st) {
                case CONNECTED -> "Connected"; case CONNECTING -> "Connecting…"; case ERROR -> "Problem";
                case LOGGED_OUT -> "Not logged in"; default -> "Not set up";
            };
            Render.circle(g, x + 4, y + 14, 3.5f, dot);
            Fonts.draw(g, Fonts.Role.UI, label, x + 13, y + 10, Theme.text, 9f);
            String msg = c.message();
            if (msg != null && !msg.isEmpty()) Fonts.draw(g, Fonts.Role.UI, Fonts.fit(Fonts.Role.UI, msg, 7.2f, w - 10), x + 13, y + 24, Theme.textMuted, 7.2f);
        }));
        TextField id = new TextField("Client ID from developer.spotify.com").max(64).value(s.clientId);
        id.onChange = v -> { s.clientId = v.trim(); Zer0Settings.touch(); };
        con.wide("Client ID", "not a secret - PKCE needs no client secret", id);
        con.buttons(
                btn(() -> c.state() == SpotifyClient.State.CONNECTED ? "Reconnect" : c.state() == SpotifyClient.State.CONNECTING ? "Cancel" : "Connect",
                        () -> { if (c.state() == SpotifyClient.State.CONNECTING) c.cancelLogin(); else c.login(); }).selected(() -> c.state() != SpotifyClient.State.CONNECTED),
                btn("Disconnect", c::logout).when(() -> !s.refreshToken.isEmpty()).kind(Button.Kind.GHOST),
                btn("Open Spotify Dashboard", () -> Mc.openUrl("https://developer.spotify.com/dashboard")).kind(Button.Kind.GHOST));
        con.note(() -> "One-time setup: 1) In the Spotify Developer Dashboard create an app and tick “Web API”.  "
                + "2) Add this Redirect URI exactly: " + c.redirectUri() + "  3) Under User Management, add the Spotify account you will log in with.  "
                + "4) Copy the app's Client ID above and press Connect - "
                + "your browser opens Spotify's own login page.  Playback controls need Spotify Premium; reading what's playing works on any account.");
        Slider port = new Slider(1024, 65535, 1, () -> (double) s.port, v -> s.port = (int) Math.round(v), v -> String.valueOf((int) Math.round(v)));
        port.onRelease = Zer0Settings::touch;
        con.setting("Login port", "Change only if 8888 is taken - then update the Redirect URI too.", port,
                () -> { s.port = 8888; Zer0Settings.touch(); }, () -> s.port != 8888);

        Section np = section(out, "Now playing");
        np.add(new Row.Custom(w -> 90, (g, x, y, w, h, mx, my) -> {
            float[] size = SpotifyHud.base();
            float sc = Math.min(1.25f, Math.min((w - 4) / size[0], (h - 8) / size[1]));
            float px = x + (w - size[0] * sc) / 2f, py = y + (h - size[1] * sc) / 2f;
            if (c.state() != SpotifyClient.State.CONNECTED) {
                Fonts.drawCentered(g, Fonts.Role.UI, "Connect your account to see what's playing.", x + w / 2, y + h / 2 - 4, Theme.textFaint, 8f);
                return;
            }
            SpotifyHud.draw(g, px, py, sc, 1f, mx, my);
        }).click((x, y, w, h, mx, my, b) -> {
            float[] size = SpotifyHud.base();
            float sc = Math.min(1.25f, Math.min((w - 4) / size[0], (h - 8) / size[1]));
            return SpotifyHud.click(x + (w - size[0] * sc) / 2f, y + (h - size[1] * sc) / 2f, sc, mx, my);
        }));
        np.setting("Volume", "Only for devices that allow remote volume.", new Slider(0, 100, 1,
                () -> c.track() == null ? 0.0 : Math.max(0, c.track().volume()), v -> c.setVolume((int) Math.round(v)), v -> Math.round(v) + "%")
                .when(() -> c.track() != null && c.track().supportsVolume()));

        Section hud = section(out, "HUD");
        hud.setting("Show Spotify HUD", "", zToggle(() -> s.hudEnabled, v -> s.hudEnabled = v));
        hud.setting("Hide when nothing plays", "", zToggle(() -> s.hideWhenIdle, v -> s.hideWhenIdle = v));
        hud.setting("Layout", "Card, compact pill or tall cover.", new Segmented(List.of(new Dropdown.Opt("card", "Card"), new Dropdown.Opt("compact", "Compact"), new Dropdown.Opt("cover", "Cover")),
                () -> s.layout, v -> { s.layout = v; Zer0Settings.touch(); }).size(190, 20));
        hud.setting("Size", "", zSlider(0.6, 2, 0.05, () -> (double) s.scale, v -> s.scale = v.floatValue(), v -> String.format("%.2fx", v)),
                () -> { s.scale = 1f; Zer0Settings.touch(); }, () -> s.scale != 1f);
        hud.setting("Opacity", "", zSlider(0.2, 1, 0.05, () -> (double) s.alpha, v -> s.alpha = v.floatValue(), pct()),
                () -> { s.alpha = 0.9f; Zer0Settings.touch(); }, () -> s.alpha != 0.9f);
        hud.setting("Position", "", btn("Move in HUD editor", () -> Mc.mc().setScreen(new HudEditorScreen(Mc.mc().currentScreen, "spotify"))));
        Section el = section(out, "Elements");
        el.setting("Album art", "", zToggle(() -> s.showArt, v -> s.showArt = v));
        el.setting("Song title", "", zToggle(() -> s.showTitle, v -> s.showTitle = v));
        el.setting("Artist", "", zToggle(() -> s.showArtist, v -> s.showArtist = v));
        el.setting("Progress bar", "", zToggle(() -> s.showProgress, v -> s.showProgress = v));
        el.setting("Controls", "Clickable in chat, menus and the title screen; also bindable keys.", zToggle(() -> s.showControls, v -> s.showControls = v));
        el.setting("Volume bar", "Card layout, devices that report volume.", zToggle(() -> s.showVolume, v -> s.showVolume = v));
        el.setting("Spotify logo", "", zToggle(() -> s.showLogo, v -> s.showLogo = v));
        Section col = section(out, "Colour");
        col.setting("Use the ZER0 accent", "Otherwise use the colour below.", zToggle(() -> s.useThemeAccent, v -> s.useThemeAccent = v));
        col.setting("Progress colour", "", new ColorPicker(() -> s.accent, v -> { s.accent = v; Zer0Settings.touch(); }).when(() -> !s.useThemeAccent),
                () -> { s.accent = 0x1DB954; Zer0Settings.touch(); }, () -> s.accent != 0x1DB954);
    }
}
