package rich.screens.hud;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import rich.screens.hud.port.Animation;
import rich.screens.hud.port.BorderRadius;
import rich.screens.hud.port.ColorRGBA;
import rich.screens.hud.port.CustomDrawContext;
import rich.screens.hud.port.DrawUtil;
import rich.screens.hud.port.Easing;
import rich.screens.hud.port.PortHudElement;
import rich.screens.hud.port.Theme;
import rich.util.render.font.Fonts;

import java.util.*;

public class CoolDowns extends PortHudElement {

    private final Animation widthAnimation;
    private final Animation alpha;
    private final Map<Item, Animation> itemAnimations = new HashMap<>();
    private final Map<Item, CooldownTrack> cooldownTracks = new HashMap<>();

    private static final float blurStrength = 15.0f;
    private static final float cornerRadius = 2.25f;

    public CoolDowns() {
        super("CoolDowns", 200, 100, 75, 30, true);
        this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
        this.alpha = new Animation(200L, Easing.CUBIC_OUT);
    }

    private void drawBlurBackground(CustomDrawContext ctx, float x, float y, float width, float height, Theme theme, float animation) {
        DrawUtil.drawBlur(
                ctx.getMatrices(), x, y, width, height,
                blurStrength,
                BorderRadius.all(cornerRadius),
                new ColorRGBA(255, 255, 255, (int) (animation * 255))
        );

        ColorRGBA themeColor = theme.getColor();
        ColorRGBA backgroundColor = new ColorRGBA(
                (int) (Math.min(255, Math.max(0, themeColor.getRed() * 0.15f))),
                (int) (Math.min(255, Math.max(0, themeColor.getGreen() * 0.15f))),
                (int) (Math.min(255, Math.max(0, themeColor.getBlue() * 0.15f))),
                (int) (64 * animation)
        );
        DrawUtil.drawRoundedRect(
                ctx.getMatrices(), x, y, width, height,
                BorderRadius.all(cornerRadius),
                backgroundColor
        );
    }

    @Override
    public void renderPort(CustomDrawContext ctx, int alpha) {
        if (mc.player == null) return;

        float posX = this.getPx();
        float posY = this.getPy();
        float defaultWidth = 75.0F;
        float height = 14.5F;

        Theme theme = new Theme();

        List<CooldownEntry> cooldowns = new ArrayList<>();

        Item[] vanillaItems = {
                Items.ENDER_PEARL,
                Items.CHORUS_FRUIT,
                Items.SHIELD,
                Items.CROSSBOW,
                Items.TRIDENT,
                Items.GOAT_HORN,
                Items.GOLDEN_APPLE,
                Items.ENCHANTED_GOLDEN_APPLE,
                Items.FIREWORK_ROCKET
        };

        for (Item item : vanillaItems) {
            ItemStack stack = new ItemStack(item);
            float progress = mc.player.getItemCooldownManager().getCooldownProgress(stack, mc.getRenderTickCounter().getTickProgress(true));
            if (progress > 0.0F && progress < 1.0F) {
                float remainingSeconds = trackAndEstimate(item, progress);
                cooldowns.add(new CooldownEntry(item, getItemName(item), remainingSeconds, stack));
            } else {
                cooldownTracks.remove(item);
            }
        }

        Set<Item> customChecked = new HashSet<>();
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            float progress = mc.player.getItemCooldownManager().getCooldownProgress(stack, mc.getRenderTickCounter().getTickProgress(true));
            if (progress > 0.0F && progress < 1.0F) {
                String customName = getCustomItemName(stack);
                if (customName != null && !customChecked.contains(stack.getItem())) {
                    customChecked.add(stack.getItem());
                    float remainingSeconds = trackAndEstimate(stack.getItem(), progress);
                    cooldowns.add(new CooldownEntry(stack.getItem(), customName, remainingSeconds, stack));
                }
            } else {
                if (getCustomItemName(stack) != null) {
                    cooldownTracks.remove(stack.getItem());
                }
            }
        }

        for (CooldownEntry entry : cooldowns) {
            itemAnimations.putIfAbsent(entry.item, new Animation(150L, Easing.CUBIC_OUT));
            itemAnimations.get(entry.item).update(1.0F);
        }

        itemAnimations.entrySet().removeIf(e -> {
            boolean found = cooldowns.stream().anyMatch(c -> c.item == e.getKey());
            if (!found) {
                e.getValue().update(0.0F);
                return e.getValue().getValue() < 0.01F;
            }
            return false;
        });

        boolean hasCooldowns = !cooldowns.isEmpty();

        if (!hasCooldowns && !(mc.currentScreen instanceof ChatScreen)) {
            this.alpha.update(0.0F);
        } else {
            this.alpha.update(1.0F);
        }

        if (this.alpha.getValue() < 0.01F) {
            this.pw = 0.0F;
            this.ph = 0.0F;
            this.width = 0;
            this.height = 0;
            return;
        }

        drawBlurBackground(ctx, posX, posY, this.widthAnimation.getValue(), 14.5F, theme, this.alpha.getValue());

        ctx.drawText(Fonts.NURIKI, "T", posX + 4.25F, posY + 5.5F, 10.0F, theme.getColor().withAlpha(255.0F * this.alpha.getValue()));

        ctx.drawText(Fonts.SEMIBOLD, ":", posX + 15.25F, posY + 4F, 8.3F, new ColorRGBA(166, 166, 166, 255.0F * this.alpha.getValue()));

        ctx.drawText(Fonts.SEMIBOLD, "Cooldowns", posX + 18.5F, posY + 4.75F, 7.5F, (new ColorRGBA(-1)).withAlpha(255.0F * this.alpha.getValue()));

        posY += 14.5F + 1.0F;

        for (CooldownEntry entry : cooldowns) {
            Animation anim = itemAnimations.get(entry.item);
            if (anim == null || anim.getValue() < 0.01F) continue;

            float elementAlpha = anim.getValue() * this.alpha.getValue();
            float elementY = posY + anim.getValue() * 3.0F - 3.0F;

            height += (11.0F + 2.0F) * anim.getValue();

            drawBlurBackground(ctx, posX, elementY, this.widthAnimation.getValue(), 11.0F, theme, elementAlpha);

            ctx.drawText(Fonts.SEMIBOLD, entry.name, posX + 5.0F, elementY + 3.25F, 7.0F, (new ColorRGBA(-1)).withAlpha(elementAlpha * 255.0F));

            String timeText = formatTime(entry.remainingSeconds);
            float timeWidth = Fonts.SEMIBOLD.getWidth(timeText, 6.75F);
            float timerX = posX + this.widthAnimation.getValue() - 20.0F - timeWidth;
            ctx.drawText(Fonts.SEMIBOLD, timeText, timerX, elementY + 3.25F, 6.75F, (new ColorRGBA(-1)).withAlpha(elementAlpha * 255.0F));

            float separatorX = posX + this.widthAnimation.getValue() - 12.0F;
            ctx.drawText(Fonts.SEMIBOLD, ":", separatorX, elementY + 3.25F, 6.5F, new ColorRGBA(166, 166, 166, 255.0F * elementAlpha));

            ctx.pushMatrix();
            ctx.getMatrices().translate(posX + this.widthAnimation.getValue() - 14.0F, elementY + 1.5F);
            ctx.getMatrices().scale(0.5F, 0.5F);
            ctx.drawItem(entry.stack, 0, 0);
            ctx.popMatrix();

            float elementsWidth = Fonts.SEMIBOLD.getWidth(entry.name, 7.0F) + timeWidth + 35.0F;
            if (elementsWidth > defaultWidth) {
                defaultWidth = elementsWidth;
            }

            posY += (11.0F + 2.0F) * anim.getValue();
        }

        this.widthAnimation.update(defaultWidth);
        this.pw = this.widthAnimation.getValue();
        this.ph = height;
        this.width = (int) this.pw;
        this.height = (int) this.ph;
    }

    private float trackAndEstimate(Item item, float progress) {
        long now = System.currentTimeMillis();
        CooldownTrack track = cooldownTracks.get(item);

        if (track == null || progress > track.firstProgress + 0.05f) {
            track = new CooldownTrack();
            track.firstTimeMs = now;
            track.firstProgress = progress;
            cooldownTracks.put(item, track);
        }

        long elapsedMs = now - track.firstTimeMs;
        float progressDecreased = track.firstProgress - progress;

        if (progressDecreased > 0.02f && elapsedMs > 200) {
            float elapsedSeconds = elapsedMs / 1000.0f;
            float ratePerSecond = progressDecreased / elapsedSeconds;
            return Math.max(0, progress / ratePerSecond);
        }

        return progress * 15.0f;
    }

    private String getItemName(Item item) {
        if (item == Items.ENDER_PEARL) return "Ender Pearl";
        if (item == Items.CHORUS_FRUIT) return "Chorus Fruit";
        if (item == Items.SHIELD) return "Shield";
        if (item == Items.CROSSBOW) return "Crossbow";
        if (item == Items.TRIDENT) return "Trident";
        if (item == Items.GOAT_HORN) return "Goat Horn";
        if (item == Items.GOLDEN_APPLE) return "Golden Apple";
        if (item == Items.ENCHANTED_GOLDEN_APPLE) return "Enchanted Apple";
        if (item == Items.FIREWORK_ROCKET) return "Firework";
        return item.getName().getString();
    }

    private String getCustomItemName(ItemStack stack) {
        if (stack.isEmpty()) return null;

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return null;

        if (customData.copyNbt().contains("pyrotechnic-item")) {
            String pyroType = customData.copyNbt().getString("pyrotechnic-item").orElse("");
            if ("ALTERNATIVE_TRAP".equals(pyroType)) return "Trap";
            if ("EXPLOSIVE_TRAP".equals(pyroType)) return "Explosive Trap";
            if ("STUN_STAR".equals(pyroType)) return "Stun";
        }

        if (customData.copyNbt().contains("kringeItems")) {
            String kringeType = customData.copyNbt().getString("kringeItems").orElse("");
            if ("ExplosiveStuff".equals(kringeType)) return "Explosive Thing";
        }

        if (customData.copyNbt().contains("desorientation")) {
            return "Disorientation";
        }

        if (customData.copyNbt().contains("stratum")) {
            return "Stratum";
        }

        if (customData.copyNbt().contains("trap")) {
            return "Trap";
        }

        if (customData.copyNbt().contains("sheerdust")) {
            return "Sheer Dust";
        }

        if (customData.copyNbt().contains("godsaura")) {
            return "Gods Aura";
        }

        if (customData.copyNbt().contains("sphereEffect")) {
            String sphereType = customData.copyNbt().getString("sphereEffect").orElse("");
            if (sphereType.contains("speed")) return "Speed Sphere";
            if (sphereType.contains("strength")) return "Strength Sphere";
            if (sphereType.contains("regeneration")) return "Regen Sphere";
            if (sphereType.contains("resistance")) return "Resistance Sphere";
            if (sphereType.contains("fire_resistance")) return "Fire Res Sphere";
            if (sphereType.contains("invisibility")) return "Invisibility Sphere";
            if (sphereType.contains("night_vision")) return "Night Vision Sphere";
            if (sphereType.contains("water_breathing")) return "Water Breathing Sphere";
            if (sphereType.contains("jump_boost")) return "Jump Boost Sphere";
            if (sphereType.contains("absorption")) return "Absorption Sphere";
            if (sphereType.contains("health_boost")) return "Health Boost Sphere";
            return "Sphere";
        }

        return null;
    }

    private String formatTime(float seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        if (seconds >= 60.0F) {
            int totalSec = (int) Math.ceil(seconds);
            int minutes = totalSec / 60;
            int secs = totalSec % 60;
            return String.format("%d:%02d", minutes, secs);
        }
        if (seconds < 1.0F) {
            return String.format("%.1fs", seconds);
        }
        return String.format("%ds", (int) Math.ceil(seconds));
    }

    private static class CooldownEntry {
        final Item item;
        final String name;
        final float remainingSeconds;
        final ItemStack stack;

        CooldownEntry(Item item, String name, float remainingSeconds, ItemStack stack) {
            this.item = item;
            this.name = name;
            this.remainingSeconds = remainingSeconds;
            this.stack = stack;
        }
    }

    private static class CooldownTrack {
        long firstTimeMs;
        float firstProgress;
    }
}
