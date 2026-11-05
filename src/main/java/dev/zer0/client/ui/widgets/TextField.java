package dev.zer0.client.ui.widgets;

import dev.zer0.client.compat.Mc;
import dev.zer0.client.ui.core.*;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** Rounded text input: caret with blink, selection (shift/ctrl+A), clipboard, focus ring, placeholder, submit on Enter. */
public class TextField extends Widget {
    public String text = "";
    public String placeholder = "";
    public int maxLen = 64;
    public Consumer<String> onChange, onSubmit;
    public Predicate<Integer> filter = c -> c >= 32 && c != 127;
    public String icon;  // e.g. search glyph
    private int caret, anchor;
    private float scrollX;
    private double blinkFrom;
    private final Anim.Smooth focus = new Anim.Smooth(0, 16), hover = new Anim.Smooth(0, 16);

    public TextField(String placeholder) { this.placeholder = placeholder; prefW = 150; prefH = 20; }
    public TextField value(String v) { text = v == null ? "" : v; caret = anchor = text.length(); return this; }
    public TextField max(int m) { maxLen = m; return this; }
    public TextField changed(Consumer<String> c) { onChange = c; return this; }
    public TextField submitted(Consumer<String> c) { onSubmit = c; return this; }
    public TextField hexOnly() { filter = c -> Character.digit(c, 16) >= 0 || c == '#'; maxLen = 7; return this; }
    public TextField icon(String i) { icon = i; return this; }

    public boolean isFocused() { return Ui.focused == this; }
    @Override public boolean wantsKeyboard() { return true; }

    @Override public void render(DrawContext g, float mx, float my) {
        boolean f = isFocused(), en = isEnabled();
        float fv = focus.to(f ? 1 : 0), hv = hover.to(en && over(mx, my) ? 1 : 0);
        float r = Math.min(h / 2, Layout.current().radius - 2);
        if (fv > 0.01f) Render.rect(g, x - 2, y - 2, w + 4, h + 4, r + 2, Col.mulAlpha(Theme.accentSoft, fv));
        Render.rect(g, x, y, w, h, r, Col.mix(Col.mix(Theme.field, Theme.fieldFocus, hv * 0.5f), Theme.fieldFocus, fv));
        Render.stroke(g, x, y, w, h, r, 0.5f, Col.mix(Theme.panelEdge, Theme.accent, fv * 0.8f));
        float size = 8f, px = Fonts.px(Fonts.Role.UI, size), ty = y + (h - px) / 2f;
        float left = x + 8 + (icon != null ? 12 : 0), inner = w - (left - x) - 8;
        if ("search".equals(icon)) {   // vector magnifier
            float cx = x + 11, cy = y + h / 2f - 0.6f;
            Render.stroke(g, cx - 3.2f, cy - 3.2f, 6.4f, 6.4f, 3.2f, 1f, Theme.textMuted);
            for (int i = 0; i < 4; i++) Render.rect(g, cx + 2.2f + i * 0.7f - 0.6f, cy + 2.2f + i * 0.7f - 0.6f, 1.3f, 1.3f, 0.65f, Theme.textMuted);
        } else if (icon != null) Fonts.drawWith(g, "minecraft", false, icon, x + 7, ty, Theme.textMuted, px);
        if (text.isEmpty() && !f) { Fonts.draw(g, Fonts.Role.UI, placeholder, left, ty, Theme.textFaint, size); }
        else {
            float caretX = Fonts.width(Fonts.Role.UI, text.substring(0, caret), size);
            if (caretX - scrollX > inner) scrollX = caretX - inner;
            if (caretX - scrollX < 0) scrollX = caretX;
            g.enableScissor((int) Math.floor(left - 1), (int) y, (int) Math.ceil(left + inner + 1), (int) (y + h));
            if (caret != anchor) {
                int a = Math.min(caret, anchor), b = Math.max(caret, anchor);
                float sx = left - scrollX + Fonts.width(Fonts.Role.UI, text.substring(0, a), size);
                float ex = left - scrollX + Fonts.width(Fonts.Role.UI, text.substring(0, b), size);
                Render.rect(g, sx, ty - 1, ex - sx, px + 2, 2, Col.withAlpha(Theme.accent, 110));
            }
            Fonts.draw(g, Fonts.Role.UI, text.isEmpty() ? "" : text, left - scrollX, ty, en ? Theme.text : Theme.textFaint, size);
            if (f && ((long) ((Anim.seconds() - blinkFrom) * 1.8) % 2 == 0))
                Render.rect(g, left - scrollX + caretX, ty - 1, 1f, px + 2, 0.5f, Theme.accent);
            g.disableScissor();
        }
        if (hv > 0.5f && tooltip != null) Ui.hoverTip(this, tooltip, mx, my);
    }

    @Override public boolean mouseDown(float mx, float my, int b) {
        if (!isEnabled() || !over(mx, my)) return false;
        Ui.focused = this; blinkFrom = Anim.seconds();
        float left = x + 8 + (icon != null ? 12 : 0);
        caret = anchor = indexAt(mx - left + scrollX);
        if (b == 1) { set(""); }      // right-click clears, like most launchers
        return true;
    }
    private int indexAt(float px) {
        for (int i = 0; i <= text.length(); i++) if (Fonts.width(Fonts.Role.UI, text.substring(0, i), 8f) > px) return Math.max(0, i - 1);
        return text.length();
    }
    private void set(String s) {
        if (s.length() > maxLen) s = s.substring(0, maxLen);
        if (!s.equals(text)) { text = s; caret = Math.min(caret, s.length()); anchor = Math.min(anchor, s.length()); if (onChange != null) onChange.accept(text); }
    }
    private void insert(String s) {
        int a = Math.min(caret, anchor), b = Math.max(caret, anchor);
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) if (filter.test((int) c)) sb.append(c);
        String n = text.substring(0, a) + sb + text.substring(b);
        if (n.length() > maxLen) return;
        caret = anchor = a + sb.length();
        set(n);
    }

    @Override public boolean key(int key, int mods) {
        if (!isFocused()) return false;
        boolean ctrl = (mods & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0, shift = (mods & GLFW.GLFW_MOD_SHIFT) != 0;
        blinkFrom = Anim.seconds();
        switch (key) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { if (onSubmit != null) onSubmit.accept(text); return true; }
            case GLFW.GLFW_KEY_ESCAPE -> { Ui.focused = null; return true; }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (caret != anchor) insert("");
                else if (caret > 0) { int from = ctrl ? wordLeft() : caret - 1; String n = text.substring(0, from) + text.substring(caret); caret = anchor = from; set(n); }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (caret != anchor) insert("");
                else if (caret < text.length()) set(text.substring(0, caret) + text.substring(caret + 1));
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> { caret = ctrl ? wordLeft() : Math.max(0, caret - 1); if (!shift) anchor = caret; return true; }
            case GLFW.GLFW_KEY_RIGHT -> { caret = ctrl ? wordRight() : Math.min(text.length(), caret + 1); if (!shift) anchor = caret; return true; }
            case GLFW.GLFW_KEY_HOME -> { caret = 0; if (!shift) anchor = caret; return true; }
            case GLFW.GLFW_KEY_END -> { caret = text.length(); if (!shift) anchor = caret; return true; }
            default -> {}
        }
        if (ctrl) {
            if (key == GLFW.GLFW_KEY_A) { anchor = 0; caret = text.length(); return true; }
            if (key == GLFW.GLFW_KEY_C) { if (caret != anchor) Mc.setClipboard(text.substring(Math.min(caret, anchor), Math.max(caret, anchor))); return true; }
            if (key == GLFW.GLFW_KEY_X) { if (caret != anchor) { Mc.setClipboard(text.substring(Math.min(caret, anchor), Math.max(caret, anchor))); insert(""); } return true; }
            if (key == GLFW.GLFW_KEY_V) { insert(Mc.clipboard().replace("\n", " ")); return true; }
        }
        return true;   // focused field swallows other keys (so typing "e" never closes the screen)
    }
    private int wordLeft() { int i = caret; while (i > 0 && text.charAt(i - 1) == ' ') i--; while (i > 0 && text.charAt(i - 1) != ' ') i--; return i; }
    private int wordRight() { int i = caret; while (i < text.length() && text.charAt(i) == ' ') i++; while (i < text.length() && text.charAt(i) != ' ') i++; return i; }

    @Override public boolean chr(int cp) {
        if (!isFocused() || !filter.test(cp)) return isFocused();
        insert(new String(Character.toChars(cp)));
        return true;
    }
}
