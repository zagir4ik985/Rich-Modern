package rich.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.modules.impl.player.NoDelay;

@Mixin(LivingEntity.class)
public class LivingEntityNoDelayMixin {

    @Shadow
    private int jumpingCooldown;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovementHead(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == player) {
                NoDelay noDelay = NoDelay.getInstance();
                if (noDelay != null && noDelay.isState() && noDelay.ignoreSetting.isSelected("Прыжок")) {
                    this.jumpingCooldown = 0;
                }
            }
        }
    }
}
