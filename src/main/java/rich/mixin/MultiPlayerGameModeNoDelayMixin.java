package rich.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rich.modules.impl.player.NoDelay;

@Mixin(ClientPlayerInteractionManager.class)
public class MultiPlayerGameModeNoDelayMixin {

    @Shadow
    private int blockBreakingCooldown;

    private boolean isNoDelayActive() {
        NoDelay noDelay = NoDelay.getInstance();
        return noDelay != null && noDelay.isState() && noDelay.ignoreSetting.isSelected("Задержка ломания");
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void onUpdateBlockBreakingHead(CallbackInfoReturnable<Boolean> cir) {
        if (isNoDelayActive()) {
            this.blockBreakingCooldown = 0;
        }
    }

    @Inject(method = "attackBlock", at = @At("HEAD"))
    private void onStartDestroyHead(CallbackInfoReturnable<Boolean> cir) {
        if (isNoDelayActive()) {
            this.blockBreakingCooldown = 0;
        }
    }
}
