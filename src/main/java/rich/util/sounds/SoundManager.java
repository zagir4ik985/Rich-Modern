package rich.util.sounds;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import rich.IMinecraft;
import rich.util.string.PlayerInteractionHelper;

@Setter
@Getter
@UtilityClass
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SoundManager implements IMinecraft {
    public SoundEvent KOLOKOLNIA_KILL = SoundEvent.of(Identifier.of("rich:kolokolnia_kill"));
    public SoundEvent MOAN1 = SoundEvent.of(Identifier.of("rich:moan1"));
    public SoundEvent MOAN2 = SoundEvent.of(Identifier.of("rich:moan2"));
    public SoundEvent MOAN3 = SoundEvent.of(Identifier.of("rich:moan3"));
    public SoundEvent MOAN4 = SoundEvent.of(Identifier.of("rich:moan4"));
    public SoundEvent MODULE_DISABLE = SoundEvent.of(Identifier.of("rich:module_disable"));
    public SoundEvent MODULE_ENABLE = SoundEvent.of(Identifier.of("rich:module_enable"));
    public SoundEvent OFF = SoundEvent.of(Identifier.of("rich:off"));
    public SoundEvent ON = SoundEvent.of(Identifier.of("rich:on"));
    public SoundEvent CRIME = SoundEvent.of(Identifier.of("rich:crime"));
    public SoundEvent METALLIC = SoundEvent.of(Identifier.of("rich:metallic"));
    public SoundEvent WELCOME = SoundEvent.of(Identifier.of("rich:welcome"));
    public SoundEvent DOUBLE_KILL = SoundEvent.of(Identifier.of("rich:double_kill"));
    public SoundEvent FIRSTBLOOD = SoundEvent.of(Identifier.of("rich:firstblood"));
    public SoundEvent KILLING_SPREE = SoundEvent.of(Identifier.of("rich:killing_spree"));
    public SoundEvent MEGA_KILL = SoundEvent.of(Identifier.of("rich:mega_kill"));
    public SoundEvent MONSTER_KILL = SoundEvent.of(Identifier.of("rich:monster_kill"));
    public SoundEvent RAMPAGE = SoundEvent.of(Identifier.of("rich:rampage"));
    public SoundEvent TRIPLE_KILL = SoundEvent.of(Identifier.of("rich:triple_kill"));
    public SoundEvent ULTRA_KILL = SoundEvent.of(Identifier.of("rich:ultra_kill"));

    public void init() {
        Registry.register(Registries.SOUND_EVENT, KOLOKOLNIA_KILL.id(), KOLOKOLNIA_KILL);
        Registry.register(Registries.SOUND_EVENT, MOAN1.id(), MOAN1);
        Registry.register(Registries.SOUND_EVENT, MOAN2.id(), MOAN2);
        Registry.register(Registries.SOUND_EVENT, MOAN3.id(), MOAN3);
        Registry.register(Registries.SOUND_EVENT, MOAN4.id(), MOAN4);
        Registry.register(Registries.SOUND_EVENT, MODULE_DISABLE.id(), MODULE_DISABLE);
        Registry.register(Registries.SOUND_EVENT, MODULE_ENABLE.id(), MODULE_ENABLE);
        Registry.register(Registries.SOUND_EVENT, OFF.id(), OFF);
        Registry.register(Registries.SOUND_EVENT, ON.id(), ON);
        Registry.register(Registries.SOUND_EVENT, CRIME.id(), CRIME);
        Registry.register(Registries.SOUND_EVENT, METALLIC.id(), METALLIC);
        Registry.register(Registries.SOUND_EVENT, WELCOME.id(), WELCOME);
        Registry.register(Registries.SOUND_EVENT, DOUBLE_KILL.id(), DOUBLE_KILL);
        Registry.register(Registries.SOUND_EVENT, FIRSTBLOOD.id(), FIRSTBLOOD);
        Registry.register(Registries.SOUND_EVENT, KILLING_SPREE.id(), KILLING_SPREE);
        Registry.register(Registries.SOUND_EVENT, MEGA_KILL.id(), MEGA_KILL);
        Registry.register(Registries.SOUND_EVENT, MONSTER_KILL.id(), MONSTER_KILL);
        Registry.register(Registries.SOUND_EVENT, RAMPAGE.id(), RAMPAGE);
        Registry.register(Registries.SOUND_EVENT, TRIPLE_KILL.id(), TRIPLE_KILL);
        Registry.register(Registries.SOUND_EVENT, ULTRA_KILL.id(), ULTRA_KILL);
    }

    public void playSound(SoundEvent sound) {
        playSound(sound, 1, 1);
    }

    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!PlayerInteractionHelper.nullCheck()) {
            mc.world.playSound(mc.player, mc.player.getBlockPos(), sound, SoundCategory.BLOCKS, volume, pitch);
        }
    }

    public void playSoundDirect(SoundEvent sound, float volume, float pitch) {
        mc.getSoundManager().play(PositionedSoundInstance.ui(sound, pitch, volume));
    }
}