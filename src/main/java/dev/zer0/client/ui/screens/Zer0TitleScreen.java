package dev.zer0.client.ui.screens;

import dev.zer0.client.ScreenRouter;
import dev.zer0.client.compat.Mc;
import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.spotify.SpotifyClient;
import dev.zer0.client.spotify.SpotifyHud;
import dev.zer0.client.ui.core.*;
import dev.zer0.client.ui.widgets.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** The ZER0 main menu: replaces Minecraft's title screen, keeps every vanilla destination one click away. */
public class Zer0TitleScreen extends Zer0Screen {
    private final List<Button> stack = new ArrayList<>();
    private Button classic, mods;
    private final Anim.Timeline intro = new Anim.Timeline(0.6f);

    public Zer0TitleScreen() { super("ZER0 CLIENT", null); }

    @Override protected void build() {
        stack.clear();
        Zer0Settings z = Zer0Settings.get();
        Button play = new Button("Play", this::play).text(10f);
        play.selected(() -> true);
        play.tooltip = z.playAddress.isBlank() ? "Opens your server list" : "Join " + z.playAddress;
        stack.add(play);
        stack.add(new Button("Singleplayer", () -> client.setScreen(new SelectWorldScreen(this))).text(9f));
        stack.add(new Button("Multiplayer", () -> client.setScreen(new MultiplayerScreen(this))).text(9f));
        stack.add(new Button("Settings", () -> client.setScreen(new OptionsScreen(this, client.options))).text(9f));
        stack.add(new Button("Customize", () -> client.setScreen(new HubScreen(this, "appearance"))).text(9f));
        stack.add(new Button("Streamer Hub", () -> client.setScreen(new HubScreen(this, "home"))).text(9f));
        Button exit = new Button("Exit", () -> client.scheduleStop()).text(9f);
        exit.kind(Button.Kind.GHOST);
        stack.add(exit);
        classic = new Button("Classic menu", () -> { TitleScreen t = new TitleScreen(); ScreenRouter.allowClassic(t); client.setScreen(t); }).kind(Button.Kind.GHOST).text(7.5f);
        mods = FabricLoader.getInstance().isModLoaded("modmenu") ? new Button("Mods", this::openModMenu).kind(Button.Kind.GHOST).text(7.5f) : null;
        intro.restart();
    }

    private void play() {
        String a = Zer0Settings.get().playAddress;
        if (a == null || a.isBlank()) client.setScreen(new MultiplayerScreen(this));
        else Mc.connect(this, a.trim());
    }
    /** Mod Menu is optional, so it is opened reflectively. */
    private void openModMenu() {
        try {
            Class<?> c = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            client.setScreen((Screen) c.getConstructor(Screen.class).newInstance(this));
        } catch (Throwable t) { Ui.toast("Couldn't open Mod Menu", true); }
    }

    @Override protected void draw(DrawContext g, float mx, float my, float delta) {
        float left = Math.max(26, width * 0.075f);
        float bh = height < 330 ? 20 : 24, gap = height < 330 ? 5 : 7;
        float brand = Math.max(26, Math.min(54, height / 7.5f));
        float total = brand * 1.35f + 26 + stack.size() * (bh + gap);
        float top = Math.max(16, (height - total) / 2f - 8);

        // brand with entrance
        float t0 = Anim.easeOutCubic(intro.delayed(0));
        float bx = left - (1 - t0) * 24;
        int a = Math.round(255 * t0);
        float zw = Fonts.draw(g, Fonts.Role.HEADING, "ZER", bx, top, Col.withAlpha(0xFFFFFF, a), brand);
        Fonts.draw(g, Fonts.Role.HEADING, "0", bx + zw, top, Col.withAlpha(Theme.accent, a), brand);
        float ty = top + Fonts.px(Fonts.Role.HEADING, brand) + 6;
        Fonts.draw(g, Fonts.Role.UI, "C   L   I   E   N   T", bx + 2, ty, Col.withAlpha(0xFFFFFF, Math.round(a * 0.55f)), 7.5f);

        float y = ty + 24, bw = Math.min(200, width * 0.36f);
        for (int i = 0; i < stack.size(); i++) {
            Button b = stack.get(i);
            float t = Anim.easeOutCubic(intro.delayed(0.08f + i * 0.045f));
            if (i == stack.size() - 1) y += 4;
            b.place(left - (1 - t) * 30, y, bw, bh);
            if (t > 0.02f) b.render(g, t > 0.9f ? mx : -9999, my);
            y += bh + gap;
        }

        // clock (top right)
        LocalDateTime now = LocalDateTime.now();
        float ct = Anim.easeOutCubic(intro.delayed(0.25f));
        int ca = Math.round(255 * ct);
        Fonts.drawRight(g, Fonts.Role.HEADING, now.format(DateTimeFormatter.ofPattern("HH:mm")), width - 24, 20, Col.withAlpha(0xFFFFFF, ca), 22);
        Fonts.drawRight(g, Fonts.Role.UI, now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")), width - 24, 20 + Fonts.px(Fonts.Role.HEADING, 22) + 4, Col.withAlpha(0xFFFFFF, Math.round(ca * 0.6f)), 8f);

        // now playing (bottom right) when Spotify is connected
        if (SpotifyClient.get().state() == SpotifyClient.State.CONNECTED && width > 420) {
            float[] s = SpotifyHud.base();
            float sc = 1f, sx = width - s[0] * sc - 22, sy = height - s[1] * sc - 34;
            SpotifyHud.draw(g, sx, sy + (1 - ct) * 10, sc, ct, mx, my);
        }

        // footer
        String ver = FabricLoader.getInstance().getModContainer("zer0client").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("dev");
        Fonts.draw(g, Fonts.Role.UI, "ZER0 CLIENT " + ver + "  ·  Minecraft " + FabricLoader.getInstance().getModContainer("minecraft").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("1.21.11"), 12, height - 14, 0x80FFFFFF, 7f);
        Fonts.drawRight(g, Fonts.Role.UI, "Copyright Mojang AB. Do not distribute!", width - 12, height - 14, 0x66FFFFFF, 7f);
        float cx = 12;
        classic.place(cx, height - 36, classic.naturalWidth(), 18); classic.render(g, mx, my);
        if (mods != null) { mods.place(cx + classic.naturalWidth() + 4, height - 36, mods.naturalWidth(), 18); mods.render(g, mx, my); }
    }

    @Override protected boolean onMouseDown(float mx, float my, int b) {
        for (Button bt : stack) if (bt.mouseDown(mx, my, b)) return true;
        if (classic.mouseDown(mx, my, b)) return true;
        if (mods != null && mods.mouseDown(mx, my, b)) return true;
        if (SpotifyClient.get().state() == SpotifyClient.State.CONNECTED) {
            float[] s = SpotifyHud.base();
            if (SpotifyHud.click(width - s[0] - 22, height - s[1] - 34, 1f, mx, my)) return true;
        }
        return false;
    }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
