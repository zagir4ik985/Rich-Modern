package rich.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import rich.Initialization;
import rich.events.api.EventHandler;
import rich.events.impl.InputEvent;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.util.Instance;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpeedSpooky extends ModuleStructure {

    private static final float CHARGE_TIMER = 0.05F;
    private static final float BOOST_TIMER = 1.7F;
    private static final long CHARGE_DURATION_NANOS = 1_250_000_000L;
    private static final long MAX_BOOST_DURATION_NANOS = 2_400_000_000L;
    private static final int FULL_BOOST_JUMPS = 4;

    @NonFinal Phase phase = Phase.CHARGING;
    @NonFinal boolean cycleActive;
    @NonFinal boolean airborne;
    @NonFinal int completedJumps;
    @NonFinal int groundTicks;
    @NonFinal long phaseStartedAt;
    @NonFinal boolean sendingSilent;

    final Queue<Packet<?>> delayedTransactions = new ConcurrentLinkedQueue<>();

    public static SpeedSpooky getInstance() {
        return Instance.get(SpeedSpooky.class);
    }

    public SpeedSpooky() {
        super("SpeedSpooky", "TimerSpooky Grim bypass", ModuleCategory.MOVEMENT);
        settings();
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (mc.player == null) return;
        if (sendingSilent) return;

        if (cycleActive) {
            if (e.isSend() && e.getPacket() != null) {
                delayedTransactions.add(e.getPacket());
                e.setCancelled(true);
                return;
            }

            if (!e.isSend() && e.getPacket() instanceof PlayerPositionLookS2CPacket) {
                beginCharging();
            }
        }
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (mc.player == null) return;

        if (!isMoving()) {
            groundTicks = 0;
            stopCycle();
            return;
        }

        if (phase == Phase.BOOSTING) {
            groundTicks = mc.player.horizontalCollision ? groundTicks + 1 : 0;
            e.setSprinting(true);
            if (groundTicks > 0) {
                e.setJumping(true);
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null) return;

        if (!isMoving()) {
            stopCycle();
            return;
        }

        long now = System.nanoTime();

        if (!cycleActive) {
            cycleActive = true;
            beginCharging();
            return;
        }

        if (phase == Phase.BOOSTING) {
            if (mc.player.horizontalCollision) {
                if (airborne) {
                    airborne = false;
                    completedJumps++;
                }
            } else {
                airborne = true;
                if (completedJumps >= FULL_BOOST_JUMPS && mc.player.getVelocity().y <= 0.0D) {
                    beginCharging();
                    return;
                }
            }
        }

        if (phase == Phase.CHARGING && now - phaseStartedAt >= CHARGE_DURATION_NANOS) {
            beginBoosting();
        } else if (phase == Phase.BOOSTING && now - phaseStartedAt >= MAX_BOOST_DURATION_NANOS) {
            beginCharging();
        }
    }

    private boolean isMoving() {
        if (mc.player == null || mc.player.input == null) return false;
        return mc.player.input.playerInput.forward() || mc.player.input.playerInput.backward()
                || mc.player.input.playerInput.left() || mc.player.input.playerInput.right();
    }

    private void beginCharging() {
        flushTransactions();
        phase = Phase.CHARGING;
        airborne = false;
        completedJumps = 0;
        groundTicks = 0;
        phaseStartedAt = System.nanoTime();
        Initialization.TIMER = CHARGE_TIMER;
    }

    private void beginBoosting() {
        phase = Phase.BOOSTING;
        airborne = false;
        completedJumps = 0;
        groundTicks = 0;
        phaseStartedAt = System.nanoTime();
        Initialization.TIMER = BOOST_TIMER;
    }

    private void stopCycle() {
        flushTransactions();
        cycleActive = false;
        phase = Phase.CHARGING;
        airborne = false;
        completedJumps = 0;
        groundTicks = 0;
        phaseStartedAt = 0L;
        Initialization.TIMER = 1.0f;
    }

    private void flushTransactions() {
        Packet<?> packet;
        while ((packet = delayedTransactions.poll()) != null) {
            sendSilentPacket(packet);
        }
    }

    private void sendSilentPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) return;
        try {
            sendingSilent = true;
            mc.getNetworkHandler().sendPacket(packet);
        } finally {
            sendingSilent = false;
        }
    }

    @Override
    public void activate() {
        stopCycle();
    }

    @Override
    public void deactivate() {
        stopCycle();
    }

    private enum Phase {
        CHARGING,
        BOOSTING
    }
}
