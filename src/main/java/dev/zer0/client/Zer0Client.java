package dev.zer0.client;

import dev.zer0.client.compat.Mc;
import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.spotify.SpotifyClient;
import dev.zer0.client.spotify.SpotifyHud;
import dev.zer0.client.ui.core.Anim;
import dev.zer0.client.ui.screens.Zer0Screen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * ZER0 CLIENT entry point. Runs next to the original mod's own entry point (which keeps doing all the
 * streamer logic: chat parsing, queue, commands, HUD boxes, keybinds).
 */
public class Zer0Client implements ClientModInitializer {
    private static KeyBinding spotifyToggle, spotifyNext, spotifyPrev;

    @Override public void onInitializeClient() {
        Zer0Settings.get();

        KeyBinding.Category cat = Mc.keyCategory("main");
        spotifyToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.zer0client.spotify_toggle", GLFW.GLFW_KEY_UNKNOWN, cat));
        spotifyNext = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.zer0client.spotify_next", GLFW.GLFW_KEY_UNKNOWN, cat));
        spotifyPrev = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.zer0client.spotify_prev", GLFW.GLFW_KEY_UNKNOWN, cat));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> Zer0Settings.flush());

        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, Identifier.of("zer0client", "spotify"), (ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden) return;
            if (mc.currentScreen == null) Anim.frame();
            SpotifyHud.renderHud(ctx);
        });

        // Spotify HUD controls are clickable while chat is open (the mouse is free there).
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof ChatScreen) {
                ScreenMouseEvents.allowMouseClick(screen).register((scr, click) -> {
                    if (!SpotifyHud.visibleNow()) return true;
                    Zer0Settings.Spotify s = Zer0Settings.get().spotify;
                    return !SpotifyHud.click(s.x, s.y, s.scale, (float) Mc.mx(click), (float) Mc.my(click));
                });
            }
        });
    }

    private void tick(MinecraftClient mc) {
        Zer0Settings.tick();
        while (spotifyToggle.wasPressed()) SpotifyClient.get().togglePlay();
        while (spotifyNext.wasPressed()) SpotifyClient.get().next();
        while (spotifyPrev.wasPressed()) SpotifyClient.get().previous();
        boolean wanted = (Zer0Settings.get().spotify.hudEnabled && mc.world != null) || mc.currentScreen instanceof Zer0Screen;
        SpotifyClient.get().tick(wanted);
    }
}
