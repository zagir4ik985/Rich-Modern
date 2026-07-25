package rich.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rich.modules.impl.movement.ElytraSpeed;

@Mixin(value = LivingEntity.class, priority = 950)
public class MixinLivingEntityElytraTravel {

    @Redirect(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;isGliding()Z"
            ),
            require = 0
    )
    private boolean redirectIsGliding(LivingEntity self) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player != (PlayerEntity) (Object) self) {
            return self.isGliding();
        }
        ElytraSpeed speed = ElytraSpeed.getInstance();
        if (speed != null && speed.isState()) return true;
        return self.isGliding();
    }
}
