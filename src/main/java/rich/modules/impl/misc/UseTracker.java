package rich.modules.impl.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.screens.hud.NotifyComponent;
import rich.util.repository.friend.FriendUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UseTracker extends ModuleStructure {

    BooleanSetting trackSelf = new BooleanSetting("Track Self", "Track your own effects").setValue(false);
    BooleanSetting skipFriends = new BooleanSetting("Skip Friends", "Skip friend players").setValue(true);
    BooleanSetting totemPop = new BooleanSetting("Track Totems", "Track totem pops").setValue(true);
    BooleanSetting onlyInstant = new BooleanSetting("Only Instant", "Only track instant potions").setValue(false);

    final Map<String, Map<String, Integer>> playerPotionEffects = new HashMap<>();
    final Map<String, Integer> playerTotemCount = new HashMap<>();

    public UseTracker() {
        super("UseTracker", "Tracks potion effects and totem pops", ModuleCategory.MISC);
        settings(trackSelf, skipFriends, totemPop, onlyInstant);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.world == null || mc.player == null) return;

        Set<String> onlinePlayers = new HashSet<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            String playerName = player.getName().getString();
            onlinePlayers.add(playerName);

            if (!trackSelf.isValue() && player == mc.player) continue;
            if (skipFriends.isValue() && FriendUtils.isFriend(playerName)) continue;

            int currentTotemCount = countTotems(player);
            Integer lastCount = playerTotemCount.get(playerName);

            Map<String, Integer> previousEffects = playerPotionEffects.getOrDefault(playerName, new HashMap<>());
            Map<String, Integer> currentEffects = new HashMap<>();
            StringBuilder effectMessage = new StringBuilder();

            for (Map.Entry<RegistryEntry<net.minecraft.entity.effect.StatusEffect>, StatusEffectInstance> entry : player.getActiveStatusEffects().entrySet()) {
                StatusEffectInstance instance = entry.getValue();
                String effectId = instance.getEffectType().value().getTranslationKey();
                boolean isInstant = instance.getDuration() == 0;

                if (onlyInstant.isValue() && !isInstant) continue;

                int level = instance.getAmplifier() + 1;
                currentEffects.put(effectId, level);

                if (!previousEffects.containsKey(effectId)) {
                    String name = getEffectName(instance);
                    String duration = formatDuration(instance.getDuration());

                    effectMessage.append(String.format("- %s %d (%s)\n", name, level, duration));
                }
            }

            if (totemPop.isValue() && lastCount != null && currentTotemCount < lastCount) {
                int pops = lastCount - currentTotemCount;
                String message = String.format("%s: Totem x%d", playerName, pops);
                NotifyComponent.getInstance().addTextNotification("\u2694", Text.literal(message));
            }

            if (effectMessage.length() > 0) {
                String message = String.format("[%s] Effects:\n%s", playerName, effectMessage.toString().trim());
                NotifyComponent.getInstance().addTextNotification("\u2697", Text.literal(message));
            }

            playerPotionEffects.put(playerName, currentEffects);
            playerTotemCount.put(playerName, currentTotemCount);
        }

        playerPotionEffects.keySet().removeIf(name -> !onlinePlayers.contains(name));
        playerTotemCount.keySet().removeIf(name -> !onlinePlayers.contains(name));
    }

    private int countTotems(PlayerEntity player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                count += stack.getCount();
            }
        }
        if (player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            count += player.getOffHandStack().getCount();
        }
        return count;
    }

    private String getEffectName(StatusEffectInstance instance) {
        RegistryEntry<net.minecraft.entity.effect.StatusEffect> effectType = instance.getEffectType();
        if (effectType == StatusEffects.SPEED) return "Speed";
        if (effectType == StatusEffects.SLOWNESS) return "Slowness";
        if (effectType == StatusEffects.STRENGTH) return "Strength";
        if (effectType == StatusEffects.WEAKNESS) return "Weakness";
        if (effectType == StatusEffects.INSTANT_HEALTH) return "Instant Health";
        if (effectType == StatusEffects.INSTANT_DAMAGE) return "Instant Damage";
        if (effectType == StatusEffects.RESISTANCE) return "Resistance";
        if (effectType == StatusEffects.HASTE) return "Haste";
        if (effectType == StatusEffects.MINING_FATIGUE) return "Mining Fatigue";
        if (effectType == StatusEffects.REGENERATION) return "Regeneration";
        if (effectType == StatusEffects.WITHER) return "Wither";
        if (effectType == StatusEffects.POISON) return "Poison";
        if (effectType == StatusEffects.FIRE_RESISTANCE) return "Fire Resistance";
        if (effectType == StatusEffects.ABSORPTION) return "Absorption";
        if (effectType == StatusEffects.INVISIBILITY) return "Invisibility";
        if (effectType == StatusEffects.NIGHT_VISION) return "Night Vision";
        if (effectType == StatusEffects.JUMP_BOOST) return "Jump Boost";
        if (effectType == StatusEffects.WATER_BREATHING) return "Water Breathing";
        if (effectType == StatusEffects.SLOW_FALLING) return "Slow Falling";
        if (effectType == StatusEffects.LEVITATION) return "Levitation";
        if (effectType == StatusEffects.GLOWING) return "Glowing";
        if (effectType == StatusEffects.HUNGER) return "Hunger";
        if (effectType == StatusEffects.NAUSEA) return "Nausea";
        if (effectType == StatusEffects.BLINDNESS) return "Blindness";
        if (effectType == StatusEffects.LUCK) return "Luck";
        if (effectType == StatusEffects.UNLUCK) return "Unluck";
        if (effectType == StatusEffects.CONDUIT_POWER) return "Conduit Power";
        if (effectType == StatusEffects.DOLPHINS_GRACE) return "Dolphin's Grace";
        if (effectType == StatusEffects.BAD_OMEN) return "Bad Omen";
        if (effectType == StatusEffects.HERO_OF_THE_VILLAGE) return "Hero of the Village";
        if (effectType == StatusEffects.HEALTH_BOOST) return "Health Boost";
        return effectType.value().getTranslationKey();
    }

    private String formatDuration(int ticks) {
        if (ticks == 0) return "Instant";
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds %= 60;
        if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        }
        return String.format("%ds", seconds);
    }
}
