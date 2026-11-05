package dev.zer0.client.mixin;

import dev.zer0.client.GlassHud;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Restyles the original mod's HUD boxes without touching their logic: only the two drawing calls that
 * paint each box's background and border are redirected to ZER0's rounded glass versions. Everything the
 * boxes show (queue, rings, graphs, heads, timers) is still drawn by the original code.
 *
 * The target class belongs to another mod, so names are given in Minecraft's runtime ("intermediary")
 * form, which is what the shipped game uses: class_332 = DrawContext, method_25294 = fill,
 * method_73198 = 1px outline (both recovered from the original jar). require = 0: if a future update
 * changes these, the boxes simply keep their original look instead of crashing.
 */
@Mixin(targets = "com.example.donutqueue.DonutQueueClient", remap = false)
public abstract class DonutHudMixin {
    @Redirect(method = {"renderHud", "renderStartBalBox", "renderBiddingBox", "renderInfoBox", "drawBalanceGraph"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/class_332;method_25294(IIIII)V"), require = 0)
    private void zer0$fill(DrawContext g, int x1, int y1, int x2, int y2, int color) {
        GlassHud.fill(g, x1, y1, x2, y2, color);
    }

    @Redirect(method = {"renderHud", "renderStartBalBox", "renderBiddingBox", "renderInfoBox", "drawBalanceGraph"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/class_332;method_73198(IIIII)V"), require = 0)
    private void zer0$outline(DrawContext g, int x, int y, int w, int h, int color) {
        GlassHud.outline(g, x, y, w, h, color);
    }
}
