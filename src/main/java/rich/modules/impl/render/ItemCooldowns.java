package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.ItemStack;
import rich.events.api.EventHandler;
import rich.events.impl.DrawEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.ColorUtil;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.util.HashMap;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItemCooldowns extends ModuleStructure {

    private static final float HOTBAR_WIDTH = 182.0f;
    private static final float HOTBAR_HEIGHT = 22.0f;
    private static final float SLOT_WIDTH = 20.0f;
    private static final float ITEM_SIZE = 16.0f;
    private static final float ITEM_PADDING = 3.0f;

    SliderSettings alpha = new SliderSettings("Alpha", "Cooldown overlay opacity")
            .range(50.0f, 255.0f)
            .setValue(145.0f);

    BooleanSetting showTimer = new BooleanSetting("Timer", "Show remaining cooldown time").setValue(true);

    SliderSettings timerSize = new SliderSettings("Timer Size", "Cooldown timer text size")
            .range(3.0f, 8.0f)
            .setValue(5.0f);

    final Map<Integer, CooldownInfo> cooldowns = new HashMap<>();

    public ItemCooldowns() {
        super("ItemCooldowns", "Displays cooldown overlays on hotbar items", ModuleCategory.RENDER);
        settings(alpha, showTimer, timerSize);
    }

    @Override
    public void deactivate() {
        cooldowns.clear();
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        if (mc.player == null || mc.getWindow() == null || mc.options.hudHidden) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        float hotbarX = (sw - HOTBAR_WIDTH) * 0.5f;
        float hotbarY = sh - HOTBAR_HEIGHT;

        long now = mc.player.age;

        Render2D.beginOverlay();

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                cooldowns.remove(i);
                continue;
            }

            float cooldownPercent = mc.player.getItemCooldownManager().getCooldownProgress(stack, mc.getRenderTickCounter().getTickProgress(true));
            if (cooldownPercent <= 0.0f) {
                cooldowns.remove(i);
                continue;
            }

            CooldownInfo info = cooldowns.computeIfAbsent(i, k -> new CooldownInfo());
            info.update(cooldownPercent, now);

            float slotX = hotbarX + i * SLOT_WIDTH;
            float itemX = slotX + ITEM_PADDING;
            float itemY = hotbarY + ITEM_PADDING;

            float overlayHeight = ITEM_SIZE * Math.max(0.0f, Math.min(1.0f, cooldownPercent));
            int overlayAlpha = Math.round(alpha.getValue());
            Render2D.rect(
                    itemX,
                    itemY + ITEM_SIZE - overlayHeight,
                    ITEM_SIZE,
                    overlayHeight,
                    ColorUtil.rgba(0, 0, 0, overlayAlpha)
            );

            if (showTimer.isValue()) {
                String text = info.remainingText();
                float textSize = timerSize.getValue();
                float textWidth = Fonts.REGULAR.getWidth(text, textSize);
                float textX = slotX + (SLOT_WIDTH - textWidth) * 0.5f;
                float textY = hotbarY - textSize - 2.0f;

                int bgAlpha = Math.round(160.0f * (overlayAlpha / 255.0f));
                Render2D.rect(
                        textX - 2.0f,
                        textY - 1.0f,
                        textWidth + 4.0f,
                        textSize + 2.0f,
                        ColorUtil.rgba(0, 0, 0, bgAlpha)
                );
                Fonts.REGULAR.draw(
                        text,
                        textX,
                        textY,
                        textSize,
                        ColorUtil.rgba(255, 255, 255, Math.round(245.0f * (overlayAlpha / 255.0f)))
                );
            }
        }

        Render2D.endOverlay();
    }

    private static final class CooldownInfo {
        float lastProgress = -1.0f;
        long lastTick = -1L;
        int totalTicks = -1;
        int remainingTicks;

        void update(float progress, long tick) {
            progress = Math.max(0.0f, Math.min(1.0f, progress));
            if (lastTick >= 0L && tick > lastTick && lastProgress > progress) {
                float diff = lastProgress - progress;
                if (diff > 0.00001f) {
                    int estimate = Math.round((float) (tick - lastTick) / diff);
                    if (estimate > 0 && estimate < 12000) {
                        totalTicks = totalTicks <= 0 ? estimate : Math.round(totalTicks * 0.75f + estimate * 0.25f);
                    }
                }
            }
            lastProgress = progress;
            lastTick = tick;
            remainingTicks = totalTicks <= 0
                    ? Math.max(1, Math.round(progress * 20.0f))
                    : Math.max(1, Math.round(progress * totalTicks));
        }

        String remainingText() {
            if (remainingTicks < 0) return "**:**";
            int totalSeconds = Math.max(0, remainingTicks / 20);
            int minutes = Math.min(99, totalSeconds / 60);
            int seconds = totalSeconds % 60;
            return twoDigits(minutes) + ":" + twoDigits(seconds);
        }

        private static String twoDigits(int value) {
            return value < 10 ? "0" + value : String.valueOf(value);
        }
    }
}
