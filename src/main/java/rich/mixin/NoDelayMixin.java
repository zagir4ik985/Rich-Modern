package rich.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.modules.impl.player.NoDelay;

@Mixin(MinecraftClient.class)
public class NoDelayMixin {

    @Shadow
    private int itemUseCooldown;

    @Inject(method = "doItemUse", at = @At("RETURN"))
    private void onDoItemUseReturn(CallbackInfo ci) {
        NoDelay noDelay = NoDelay.getInstance();
        if (noDelay != null && noDelay.isState() && noDelay.ignoreSetting.isSelected("Правый клик")) {
            this.itemUseCooldown = 0;
        }
    }
}
