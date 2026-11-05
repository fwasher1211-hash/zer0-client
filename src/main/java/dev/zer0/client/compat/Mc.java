package dev.zer0.client.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Path;

/**
 * Every Minecraft API call that ZER0 makes, other than DrawContext.fill / drawText / matrices and the
 * standard Screen lifecycle, is collected in this one class.
 *
 * Lines tagged "VERIFY" were written for the Minecraft 1.21.9+ API (records for mouse/keyboard input,
 * new font-style and key-category APIs) and could not be compiled against the real game in the authoring
 * environment. If a Yarn update names something differently, the compiler will point to this file only.
 * README.md -> "If the build fails" lists the known alternatives for each.
 */
public final class Mc {
    private Mc() {}

    public static MinecraftClient mc() { return MinecraftClient.getInstance(); }
    public static TextRenderer tr() { return mc().textRenderer; }
    public static double guiScale() { double s = mc().getWindow().getScaleFactor(); return s <= 0 ? 1 : s; }
    public static int sw() { return mc().getWindow().getScaledWidth(); }
    public static int sh() { return mc().getWindow().getScaledHeight(); }
    public static boolean inWorld() { return mc().world != null; }

    // ---------------------------------------------------------------- input records (VERIFY)
    public static double mx(Click c) { return c.x(); }
    public static double my(Click c) { return c.y(); }
    public static int button(Click c) { return c.button(); }
    public static int key(KeyInput k) { return k.key(); }
    public static int mods(KeyInput k) { return k.modifiers(); }
    public static int codepoint(CharInput c) { return c.codepoint(); }

    public static String clipboard() { return mc().keyboard.getClipboard(); }
    public static void setClipboard(String s) { mc().keyboard.setClipboard(s); }

    // ---------------------------------------------------------------- fonts (VERIFY: 1.21.9 font-style API)
    public static Style fontStyle(Identifier fontId) {
        return Style.EMPTY.withFont(new StyleSpriteSource.Font(fontId));
    }
    public static boolean resourceExists(Identifier id) {
        try { return mc().getResourceManager().getResource(id).isPresent(); } catch (Exception e) { return false; }
    }

    // ---------------------------------------------------------------- key categories (VERIFY)
    public static KeyBinding.Category keyCategory(String path) {
        return KeyBinding.Category.create(Identifier.of("zer0client", path));
    }

    // ---------------------------------------------------------------- textures (APIs confirmed from the original mod's bytecode)
    /** Uploads PNG bytes as a texture. Must be called on the render thread. */
    public static Identifier uploadPng(String path, byte[] png) {
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
            Identifier id = Identifier.of("zer0client", path);
            mc().getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "zer0client:" + path, img));
            return id;
        } catch (Exception e) {
            System.err.println("[ZER0] texture upload failed: " + e);
            return null;
        }
    }
    public static void destroyTexture(Identifier id) { if (id != null) mc().getTextureManager().destroyTexture(id); }

    /** Draws the whole texture (texW x texH) stretched into x,y,w,h, tinted by argb (alpha = fade). VERIFY: colour overload. */
    public static void drawImage(DrawContext g, Identifier tex, int x, int y, int w, int h, int texW, int texH, int argb) {
        g.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, w, h, texW, texH, texW, texH, argb);
    }

    // ---------------------------------------------------------------- misc
    /** Vanilla UI click (VERIFY: PositionedSoundInstance.master signature). */
    public static void clickSound() {
        try { mc().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f)); } catch (Throwable ignored) {}
    }
    public static void openUrl(String url) {
        try { Util.getOperatingSystem().open(URI.create(url)); } catch (Exception e) { System.err.println("[ZER0] open url failed: " + e); }
    }

    /** Join a server straight from the title screen (VERIFY: ConnectScreen.connect signature). */
    public static void connect(Screen from, String address) {
        ServerInfo info = new ServerInfo(address, address, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(from, mc(), ServerAddress.parse(address), info, false, null);
    }

    // ---------------------------------------------------------------- vanilla options used by ZER0 pages
    /** Vanilla "Menu Background Blur" (0-10). ZER0's glass uses Minecraft's own blur, so this is the real blur control. */
    public static int menuBlur() { return mc().options.getMenuBackgroundBlurriness().getValue(); }
    public static void setMenuBlur(int v) { mc().options.getMenuBackgroundBlurriness().setValue(Math.max(0, Math.min(10, v))); mc().options.write(); }
    public static Screen keybindsScreen(Screen parent) { return new KeybindsScreen(parent, mc().options); }
    public static void openFolder(Path p) {
        try { Util.getOperatingSystem().open(p.toFile()); } catch (Exception e) { System.err.println("[ZER0] open folder failed: " + e); }
    }
    /** Native "open file" dialog (LWJGL tinyfd, shipped with Minecraft). Runs off the render thread. */
    public static void pickImage(java.util.function.Consumer<String> done) {
        Thread t = new Thread(() -> {
            String path = null;
            try { path = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Choose a background image", "", null, "Images", false); }
            catch (Throwable e) { System.err.println("[ZER0] file dialog failed: " + e); }
            String fp = path;
            mc().execute(() -> done.accept(fp));
        }, "ZER0-FileDialog");
        t.setDaemon(true); t.start();
    }
}
