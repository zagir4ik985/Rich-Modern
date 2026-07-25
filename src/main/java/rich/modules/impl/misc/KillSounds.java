package rich.modules.impl.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import rich.events.api.EventHandler;
import rich.events.impl.PacketEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SelectSetting;
import rich.util.Instance;

public class KillSounds extends ModuleStructure {

    public static KillSounds getInstance() {
        return Instance.get(KillSounds.class);
    }

    public static final SoundEvent DOUBLE_KILL = SoundEvent.of(Identifier.of("rich:double_kill"));
    public static final SoundEvent FIRST_BLOOD = SoundEvent.of(Identifier.of("rich:firstblood"));
    public static final SoundEvent KILLING_SPREE = SoundEvent.of(Identifier.of("rich:killing_spree"));
    public static final SoundEvent MEGA_KILL = SoundEvent.of(Identifier.of("rich:mega_kill"));
    public static final SoundEvent MONSTER_KILL = SoundEvent.of(Identifier.of("rich:monster_kill"));
    public static final SoundEvent RAMPAGE = SoundEvent.of(Identifier.of("rich:rampage"));
    public static final SoundEvent TRIPLE_KILL = SoundEvent.of(Identifier.of("rich:triple_kill"));
    public static final SoundEvent ULTRA_KILL = SoundEvent.of(Identifier.of("rich:ultra_kill"));

    private final SelectSetting sound = new SelectSetting("Звук", "Звук убийства")
            .value("Double Kill", "First Blood", "Killing Spree", "Mega Kill",
                    "Monster Kill", "Rampage", "Triple Kill", "Ultra Kill")
            .selected("First Blood");

    public KillSounds() {
        super("KillSounds", "Звуки при убийстве", ModuleCategory.MISC);
        settings(sound);
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getType() != PacketEvent.Type.RECEIVE) return;
        if (!(event.getPacket() instanceof EntityStatusS2CPacket packet)) return;

        byte status = packet.getStatus();

        if (status == 3) {
            Entity entity = packet.getEntity(mc.world);
            if (entity instanceof PlayerEntity player && player != mc.player) {
                playSelectedSound();
            }
        }
    }

    private void playSelectedSound() {
        SoundEvent soundEvent = getSelectedSound();
        if (soundEvent != null && mc.world != null && mc.player != null) {
            mc.world.playSound(mc.player, mc.player.getX(), mc.player.getY(), mc.player.getZ(), soundEvent, SoundCategory.MASTER, 1.0F, 1.0F);
        }
    }

    private SoundEvent getSelectedSound() {
        if (sound.isSelected("Double Kill")) return DOUBLE_KILL;
        if (sound.isSelected("First Blood")) return FIRST_BLOOD;
        if (sound.isSelected("Killing Spree")) return KILLING_SPREE;
        if (sound.isSelected("Mega Kill")) return MEGA_KILL;
        if (sound.isSelected("Monster Kill")) return MONSTER_KILL;
        if (sound.isSelected("Rampage")) return RAMPAGE;
        if (sound.isSelected("Triple Kill")) return TRIPLE_KILL;
        if (sound.isSelected("Ultra Kill")) return ULTRA_KILL;
        return FIRST_BLOOD;
    }
}
