package rich.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;
import rich.util.network.NetworkUtility;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ElytraSpeed extends ModuleStructure {

    private final SliderSettings cooldown = new SliderSettings("Cooldown", "Ticks between attempts")
            .range(5, 40)
            .setValue(10);

    @NonFinal int ticksSinceLastAttempt = 0;
    @NonFinal boolean attemptedThisSession = false;

    public static ElytraSpeed getInstance() {
        return Instance.get(ElytraSpeed.class);
    }

    public ElytraSpeed() {
        super("ElytraSpeed", "Automatic elytra activation", ModuleCategory.MOVEMENT);
        settings(cooldown);
    }

    @Override
    public void activate() {
        ticksSinceLastAttempt = 0;
        attemptedThisSession = false;
    }

    @Override
    public void deactivate() {
        ticksSinceLastAttempt = 0;
        attemptedThisSession = false;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.player.isGliding()) {
            attemptedThisSession = false;
            ticksSinceLastAttempt = 0;
            return;
        }

        ticksSinceLastAttempt++;
        if (ticksSinceLastAttempt < (int) cooldown.getValue()) return;

        int slot = findElytraInHotbar();
        if (slot == -1) return;

        int currentSlot = mc.player.getInventory().getSelectedSlot();
        ticksSinceLastAttempt = 0;

        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        NetworkUtility.sendUse(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
    }

    private int findElytraInHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.ELYTRA) {
                return i;
            }
        }
        return -1;
    }
}
