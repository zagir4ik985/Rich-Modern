package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.util.math.Vec3d;
import rich.events.api.EventHandler;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.util.Instance;
import rich.util.timer.StopWatch;

import java.util.Random;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ElytraMotion extends ModuleStructure {

    public static Fly getInstance() {
        return Instance.get(Fly.class);
    }
    @NonFinal

    StopWatch timer = new StopWatch();
    @NonFinal
    Vec3d targetPosition = null;
    @NonFinal
    Random random = new Random();
    @NonFinal double rotationAngle = 0.0;

    public ElytraMotion() {
        super("ElytraMotion", "Elytra Motion", ModuleCategory.MOVEMENT);
        this.settings();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (!state || mc.player == null || mc.world == null || !mc.player.isGliding()) return;
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        super.deactivate();
    }
}