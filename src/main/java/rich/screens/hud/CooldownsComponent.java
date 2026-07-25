package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import rich.client.draggables.AbstractHudElement;
import rich.util.render.font.Fonts;
import rich.modules.impl.render.Hud;
import rich.util.ColorUtil;
import rich.util.render.Render2D;

import java.awt.Color;
import java.util.*;

public class CooldownsComponent extends AbstractHudElement {

    private float animatedWidth = 80;
    private float alphaAnim = 0;
    private final Map<Item, float[]> itemAnims = new HashMap<>();
    private final Map<Item, CooldownTrack> tracks = new HashMap<>();

    public CooldownsComponent() {
        super("Cooldowns", 300, 100, 80, 23, true);
        stopAnimation();
    }

    @Override
    public void tick() {
        if (mc.player == null) return;
        boolean hasCooldowns = false;
        Item[] vanillaItems = {
            Items.ENDER_PEARL, Items.CHORUS_FRUIT, Items.SHIELD, Items.CROSSBOW,
            Items.TRIDENT, Items.GOAT_HORN, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.FIREWORK_ROCKET
        };
        for (Item item : vanillaItems) {
            ItemStack stack = new ItemStack(item);
            float progress = mc.player.getItemCooldownManager().getCooldownProgress(stack, mc.getRenderTickCounter().getTickProgress(true));
            if (progress > 0 && progress < 1) {
                hasCooldowns = true;
                itemAnims.putIfAbsent(item, new float[]{0, 0});
                itemAnims.get(item)[0] = Math.min(1, itemAnims.get(item)[0] + 0.1f);
            } else {
                if (itemAnims.containsKey(item)) {
                    itemAnims.get(item)[0] = Math.max(0, itemAnims.get(item)[0] - 0.1f);
                    if (itemAnims.get(item)[0] <= 0) itemAnims.remove(item);
                }
                tracks.remove(item);
            }
        }
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            float progress = mc.player.getItemCooldownManager().getCooldownProgress(stack, mc.getRenderTickCounter().getTickProgress(true));
            if (progress > 0 && progress < 1 && getCustomItemName(stack) != null) {
                hasCooldowns = true;
                itemAnims.putIfAbsent(stack.getItem(), new float[]{0, 0});
                itemAnims.get(stack.getItem())[0] = Math.min(1, itemAnims.get(stack.getItem())[0] + 0.1f);
            }
        }
        float targetAlpha = (hasCooldowns || mc.currentScreen instanceof ChatScreen) ? 1.0f : 0.0f;
        alphaAnim += (targetAlpha - alphaAnim) * 0.1f;
    }

    @Override
    public boolean visible() {
        return alphaAnim > 0.01f || !scaleAnimation.isFinished(rich.util.animations.Direction.BACKWARDS);
    }

    @Override
    public void drawDraggable(DrawContext ctx, int alpha) {
        if (mc.player == null || alphaAnim < 0.01f) return;
        float a = (alpha / 255.0f) * alphaAnim;
        int themeColor = ColorUtil.astolfo(10000, 0, 0.7f, 0.8f, (int) (255 * a));
        int bgAlpha = (int) (64 * a);

        float x = getX();
        float y = getY();
        float defaultWidth = 75;
        float height = 14.5f;

        Render2D.blur(x, y, animatedWidth, 14.5f, 15, 2, new Color(255, 255, 255, (int) (255 * a)).getRGB());
        Render2D.rect(x, y, animatedWidth, 14.5f, ColorUtil.rgba(30, 30, 30, bgAlpha), 2);

        Fonts.HUD_ICONS.draw("T", x + 4.25f, y + 2, 10, themeColor);
        Fonts.BOLD.draw("|", x + 15.25f, y + 2, 8.3f, new Color(166, 166, 166, (int) (255 * a)).getRGB());
        Fonts.BOLD.draw("Cooldowns", x + 18.5f, y + 2, 7.5f, new Color(255, 255, 255, (int) (255 * a)).getRGB());
        y += 15.5f;

        Item[] vanillaItems = {
            Items.ENDER_PEARL, Items.CHORUS_FRUIT, Items.SHIELD, Items.CROSSBOW,
            Items.TRIDENT, Items.GOAT_HORN, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.FIREWORK_ROCKET
        };
        for (Item item : vanillaItems) {
            ItemStack stack = new ItemStack(item);
            float progress = mc.player.getItemCooldownManager().getCooldownProgress(stack, mc.getRenderTickCounter().getTickProgress(true));
            if (progress > 0 && progress < 1 && itemAnims.containsKey(item)) {
                float animVal = itemAnims.get(item)[0];
                float elemAlpha = animVal * a;
                float elemY = y + animVal * 3 - 3;
                height += 13 * animVal;

                Render2D.blur(x, elemY, animatedWidth, 11, 15, 2, new Color(255, 255, 255, (int) (255 * elemAlpha)).getRGB());
                Render2D.rect(x, elemY, animatedWidth, 11, ColorUtil.rgba(30, 30, 30, (int) (64 * elemAlpha)), 2);

                String name = getItemName(item);
                Fonts.BOLD.draw(name, x + 5, elemY + 3, 7, new Color(255, 255, 255, (int) (255 * elemAlpha)).getRGB());

                float remaining = trackAndEstimate(item, progress);
                String timeText = formatTime(remaining);
                float timeWidth = Fonts.REGULAR.getWidth(timeText, 6.75f);
                Fonts.REGULAR.draw(timeText, x + animatedWidth - 20 - timeWidth, elemY + 3, 6.75f, new Color(255, 255, 255, (int) (255 * elemAlpha)).getRGB());

                float elemWidth = Fonts.BOLD.getWidth(name, 7) + timeWidth + 35;
                if (elemWidth > defaultWidth) defaultWidth = elemWidth;
                y += 13 * animVal;
            }
        }

        animatedWidth += (defaultWidth - animatedWidth) * 0.1f;
        setWidth((int) animatedWidth);
        setHeight((int) height);
    }

    private float trackAndEstimate(Item item, float progress) {
        long now = System.currentTimeMillis();
        CooldownTrack track = tracks.get(item);
        if (track == null || progress > track.firstProgress + 0.05f) {
            track = new CooldownTrack();
            track.firstTimeMs = now;
            track.firstProgress = progress;
            tracks.put(item, track);
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
        if (customData.copyNbt().contains("pyrotechnic-item")) return "Pyrotechnic";
        if (customData.copyNbt().contains("kringeItems")) return "Kringe Item";
        if (customData.copyNbt().contains("desorientation")) return "Disorient";
        if (customData.copyNbt().contains("stratum")) return "Stratum";
        if (customData.copyNbt().contains("trap")) return "Trap";
        if (customData.copyNbt().contains("sheerdust")) return "Sheer Dust";
        if (customData.copyNbt().contains("godsaura")) return "Gods Aura";
        if (customData.copyNbt().contains("sphereEffect")) return "Sphere";
        return null;
    }

    private String formatTime(float seconds) {
        if (seconds <= 0) return "0s";
        if (seconds >= 60) {
            int totalSec = (int) Math.ceil(seconds);
            return String.format("%d:%02d", totalSec / 60, totalSec % 60);
        }
        if (seconds < 1) return String.format("%.1fs", seconds);
        return String.format("%ds", (int) Math.ceil(seconds));
    }

    private static class CooldownTrack {
        long firstTimeMs;
        float firstProgress;
    }
}
