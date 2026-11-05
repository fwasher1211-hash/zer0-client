package dev.zer0.client.ui.core;

import dev.zer0.client.compat.Mc;
import dev.zer0.client.config.Zer0Settings;

/** Soft UI click (vanilla button sound), can be switched off in General. */
public final class Sounds {
    private Sounds() {}
    public static void click() { if (Zer0Settings.get().uiSounds) Mc.clickSound(); }
}
