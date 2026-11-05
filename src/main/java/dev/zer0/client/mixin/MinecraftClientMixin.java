package dev.zer0.client.mixin;

import dev.zer0.client.ScreenRouter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Routes every screen change through ZER0 (title screen + original mod screens -> redesigned screens). */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen zer0$route(Screen screen) {
        return ScreenRouter.route(screen);
    }
}
