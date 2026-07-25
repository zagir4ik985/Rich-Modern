package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import rich.client.draggables.AbstractHudElement;
import rich.util.render.font.Fonts;
import rich.modules.impl.render.Hud;
import rich.util.ColorUtil;
import rich.util.render.Render2D;

import java.awt.Color;

public class HotbarComponent extends AbstractHudElement {

    public HotbarComponent() {
        super("HotBar", 200, 400, 216, 24, true);
        stopAnimation();
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(rich.util.animations.Direction.BACKWARDS);
    }

    @Override
    public void drawDraggable(DrawContext ctx, int alpha) {
        if (mc.player == null || alpha <= 0) return;
        float a = alpha / 255.0f;
        int themeColor = ColorUtil.astolfo(10000, 0, 0.7f, 0.8f, (int) (255 * a));
        int bgAlpha = (int) (180 * a);

        float slotSize = 24;
        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        float x = (mc.getWindow().getScaledWidth() - slotSize * 9) / 2.0f;
        float y = getY();
        float totalWidth = slotSize * 9;

        setWidth((int) totalWidth);
        setHeight((int) slotSize);

        Render2D.blur(x, y, totalWidth, slotSize, 21, 4, new Color(255, 255, 255, (int) (255 * a)).getRGB());
        Render2D.rect(x, y, totalWidth, slotSize, ColorUtil.rgba(30, 30, 30, bgAlpha), 4);

        for (int i = 0; i < 9; i++) {
            float slotX = x + i * slotSize;
            ItemStack stack = mc.player.getInventory().getMainStacks().get(i);

            if (i == selectedSlot) {
                Render2D.rect(slotX, y, slotSize, slotSize, ColorUtil.replAlpha(themeColor, (int) (80 * a)), 4);
            }

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(slotX + 5.6f, y + 5.6f);
            ctx.getMatrices().scale(0.8f, 0.8f);
            ctx.drawItem(stack, 0, 0);
            ctx.getMatrices().popMatrix();

            Fonts.REGULAR.draw(String.valueOf(i + 1), slotX + 2, y + 2, 6, new Color(255, 255, 255, (int) (200 * a)).getRGB());
            if (stack.getCount() > 1) {
                String countText = "x" + stack.getCount();
                Fonts.REGULAR.draw(countText, slotX + slotSize - Fonts.REGULAR.getWidth(countText, 6) - 1, y + slotSize - 9, 6, new Color(255, 255, 255, (int) (200 * a)).getRGB());
            }
        }

        Render2D.outline(x, y, totalWidth, slotSize, 0.35f, ColorUtil.rgba(90, 90, 90, bgAlpha), 4);

        ItemStack offHand = mc.player.getOffHandStack();
        if (!offHand.isEmpty()) {
            float offX = x - slotSize - 12;
            Render2D.blur(offX, y, slotSize, slotSize, 21, 4, new Color(255, 255, 255, (int) (255 * a)).getRGB());
            Render2D.rect(offX, y, slotSize, slotSize, ColorUtil.rgba(30, 30, 30, bgAlpha), 4);
            Render2D.outline(offX, y, slotSize, slotSize, 0.35f, ColorUtil.rgba(90, 90, 90, bgAlpha), 4);
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(offX + 5.6f, y + 5.6f);
            ctx.getMatrices().scale(0.8f, 0.8f);
            ctx.drawItem(offHand, 0, 0);
            ctx.getMatrices().popMatrix();
            if (offHand.getCount() > 1) {
                String countText = "x" + offHand.getCount();
                Fonts.REGULAR.draw(countText, offX + slotSize - Fonts.REGULAR.getWidth(countText, 6) - 1, y + slotSize - 9, 6, new Color(255, 255, 255, (int) (200 * a)).getRGB());
            }
        }
    }
}
