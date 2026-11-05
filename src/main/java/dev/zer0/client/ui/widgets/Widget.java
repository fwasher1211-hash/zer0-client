package dev.zer0.client.ui.widgets;

import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;

/** Base of every ZER0 control. Coordinates are GUI units (floats, so layouts stay crisp at any GUI scale). */
public abstract class Widget {
    public float x, y, w, h;
    public String tooltip;
    public BooleanSupplier enabled = () -> true;
    /** Preferred size when placed as a row control. */
    public float prefW = 120, prefH = 20;

    public Widget size(float w, float h) { this.prefW = w; this.prefH = h; return this; }
    public Widget tip(String t) { this.tooltip = t; return this; }
    public Widget when(BooleanSupplier b) { this.enabled = b; return this; }

    public void place(float x, float y, float w, float h) { this.x = x; this.y = y; this.w = w; this.h = h; }
    public boolean over(float mx, float my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    public boolean isEnabled() { return enabled.getAsBoolean(); }

    public abstract void render(DrawContext g, float mx, float my);
    public boolean mouseDown(float mx, float my, int button) { return false; }
    public void mouseUp(float mx, float my, int button) {}
    public void mouseDrag(float mx, float my, int button) {}
    public boolean scroll(float mx, float my, double amount) { return false; }
    public boolean key(int key, int mods) { return false; }
    public boolean chr(int codepoint) { return false; }
    /** Text fields report true so the screen routes typing to them. */
    public boolean wantsKeyboard() { return false; }
}
