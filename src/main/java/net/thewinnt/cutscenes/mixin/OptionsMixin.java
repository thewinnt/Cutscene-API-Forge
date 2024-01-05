package net.thewinnt.cutscenes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import net.thewinnt.cutscenes.client.ClientCutsceneManager;
import net.thewinnt.cutscenes.client.ClientCutsceneManager.CutsceneStatus;

@Mixin(Options.class)
public class OptionsMixin {
    @Inject(method = "setCameraType", at = @At("HEAD"), cancellable = true)
    public void setPerspective(CameraType cameraType, CallbackInfo callback) {
        if (ClientCutsceneManager.cutsceneStatus != CutsceneStatus.NONE) {
            callback.cancel();
        }
    }
}
