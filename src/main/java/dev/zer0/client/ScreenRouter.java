package dev.zer0.client;

import com.example.donutqueue.Zer0Bridge;
import dev.zer0.client.compat.Mc;
import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.ui.screens.HubScreen;
import dev.zer0.client.ui.screens.HudEditorScreen;
import dev.zer0.client.ui.screens.Zer0TitleScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;

import java.lang.ref.WeakReference;

/**
 * Decides which screen is really shown. Called from MinecraftClient.setScreen (mixin):
 *  - Minecraft's title screen  -> ZER0 main menu (unless the user chose "Classic menu").
 *  - Any screen of the original mod -> the matching page of the ZER0 Hub. The original logic behind those
 *    screens is untouched; only its interface is replaced.
 */
public final class ScreenRouter {
    private ScreenRouter() {}
    private static WeakReference<TitleScreen> classic = new WeakReference<>(null);

    public static void allowClassic(TitleScreen t) { classic = new WeakReference<>(t); }

    public static Screen route(Screen s) {
        try {
            if (s instanceof TitleScreen t) {
                if (!Zer0Settings.get().customTitle || classic.get() == t) return s;
                return new Zer0TitleScreen();
            }
            if (Zer0Bridge.isOriginalScreen(s)) {
                Screen cur = Mc.mc().currentScreen;
                Screen parent = cur == null || Zer0Bridge.isOriginalScreen(cur) ? null : cur;
                String n = s.getClass().getSimpleName();
                if (n.equals("MovableBoxScreen")) return new HudEditorScreen(parent, null);
                if (cur instanceof HubScreen h) { h.open(pageFor(n)); return h; }   // already in the hub: just switch page
                return new HubScreen(parent, pageFor(n));
            }
        } catch (Throwable e) {
            System.err.println("[ZER0] screen routing failed, showing the original screen: " + e);
        }
        return s;
    }

    static String pageFor(String simpleName) {
        return switch (simpleName) {
            case "BiddingMenuScreen", "PresetEditorScreen" -> "auctions";
            case "BiddingSettingsScreen", "BiddingConfigScreen" -> "bidding";
            case "ChatConfigScreen" -> "chat";
            case "DonutQueueConfigScreen" -> "queue";
            case "EarningsConfigScreen" -> "earnings";
            case "InfoDisplayConfigScreen" -> "info";
            case "StartBalConfigScreen" -> "startbal";
            case "RankConfigScreen" -> "ranks";
            case "MenuCustomisationScreen" -> "appearance";
            case "NewAdditionsScreen" -> "about";
            default -> "home";      // FuturisticMenuScreen, DonutQueueLandingScreen, TutorialScreen
        };
    }
}
