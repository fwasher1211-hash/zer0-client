package dev.zer0.client.ui.screens;

import dev.zer0.client.compat.Mc;
import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.ui.core.*;
import dev.zer0.client.ui.widgets.Ui;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Base for every ZER0 screen: background (procedural / custom / blurred world), frame timing, theme,
 * input routing to widgets (focus, drag capture, floating popups), tooltips and toasts.
 */
public abstract class Zer0Screen extends Screen {
    protected final Screen parent;
    private boolean bgDrawn;
    protected final Anim.Timeline opened = new Anim.Timeline(0.35f);

    protected Zer0Screen(String title, Screen parent) {
        super(Text.literal(title));
        this.parent = parent;
    }

    @Override protected void init() {
        Ui.reset();
        Fonts.invalidate();          // picks up resource-pack / reload changes
        Theme.refresh();
        build();
    }
    /** Create / lay out widgets for the current size. Called on open and on every resize. */
    protected abstract void build();
    protected abstract void draw(DrawContext g, float mx, float my, float delta);
    protected boolean onMouseDown(float mx, float my, int button) { return false; }
    protected void onMouseUp(float mx, float my, int button) {}
    protected boolean onScroll(float mx, float my, double amount) { return false; }
    protected boolean onKey(int key, int mods) { return false; }

    /** In-world menus show the (vanilla-blurred) game behind the glass when enabled. */
    protected boolean showWorld() { return Mc.inWorld() && Zer0Settings.get().worldBehindMenus; }

    @Override public void renderBackground(DrawContext g, int mouseX, int mouseY, float delta) {
        if (bgDrawn) return;
        bgDrawn = true;
        Theme.refresh();
        Zer0Settings s = Zer0Settings.get();
        if (showWorld()) {
            super.renderBackground(g, mouseX, mouseY, delta);          // vanilla frosted blur of the world (honours the Menu Background Blur option)
            Backgrounds.draw(g, 0, 0, width, height, s.bgOpacity * 0.85f);
            g.fill(0, 0, width, height, Theme.dark ? 0x30000000 : 0x18FFFFFF);
        } else {
            g.fill(0, 0, width, height, 0xFF000000);
            Backgrounds.draw(g, 0, 0, width, height, Math.max(0.15f, s.bgOpacity));
        }
    }

    @Override public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        Anim.frame();
        Theme.refresh();
        if (!bgDrawn) renderBackground(g, mouseX, mouseY, delta);   // in case the game did not call it this frame
        // while a popup is open the page underneath shows no hover state
        draw(g, Ui.overlay != null ? -9999 : mouseX, Ui.overlay != null ? -9999 : mouseY, delta);
        if (Ui.overlay != null) Ui.overlay.renderPopup(g, mouseX, mouseY);
        Ui.drawTooltip(g, width, height);
        Ui.drawToast(g, width / 2f, height);
        bgDrawn = false;
    }

    // ------------------------------------------------------------------ input (Minecraft 1.21.9+ input records)
    @Override public boolean mouseClicked(Click click, boolean doubled) {
        float mx = (float) Mc.mx(click), my = (float) Mc.my(click);
        int b = Mc.button(click);
        if (Ui.overlay != null) {
            Ui.Popup p = Ui.overlay;
            if (p.popupMouseDown(mx, my, b)) return true;
        }
        Ui.focused = null;
        return onMouseDown(mx, my, b);
    }
    @Override public boolean mouseReleased(Click click) {
        float mx = (float) Mc.mx(click), my = (float) Mc.my(click);
        int b = Mc.button(click);
        if (Ui.captured != null) { Ui.captured.mouseUp(mx, my, b); Ui.captured = null; }
        onMouseUp(mx, my, b);
        return true;
    }
    @Override public boolean mouseDragged(Click click, double dx, double dy) {
        if (Ui.captured != null) Ui.captured.mouseDrag((float) Mc.mx(click), (float) Mc.my(click), Mc.button(click));
        return true;
    }
    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (Ui.overlay != null && Ui.overlay.popupScroll((float) mx, (float) my, v)) return true;
        if (Ui.overlay != null) Ui.overlay.close();
        return onScroll((float) mx, (float) my, v);
    }
    @Override public boolean keyPressed(KeyInput input) {
        int key = Mc.key(input), mods = Mc.mods(input);
        if (key == GLFW.GLFW_KEY_ESCAPE && Ui.overlay != null) { Ui.overlay.close(); return true; }
        if (Ui.focused != null && Ui.focused.key(key, mods)) return true;
        if (onKey(key, mods)) return true;
        if (key == GLFW.GLFW_KEY_ESCAPE && shouldCloseOnEsc()) { close(); return true; }
        return false;
    }
    @Override public boolean charTyped(CharInput input) {
        if (Ui.focused != null) return Ui.focused.chr(Mc.codepoint(input));
        return false;
    }

    @Override public void close() { if (client != null) client.setScreen(parent); }
    @Override public void removed() { Ui.reset(); Zer0Settings.flush(); }
}
