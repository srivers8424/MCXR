package net.sorenon.minexraft.client.mixin.flatgui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.sorenon.minexraft.client.MineXRaftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "openScreen", at = @At("HEAD"))
    void openScreen(Screen screen, CallbackInfo ci) {
        MineXRaftClient.INSTANCE.flatGuiManager.openScreen(screen);
    }
}