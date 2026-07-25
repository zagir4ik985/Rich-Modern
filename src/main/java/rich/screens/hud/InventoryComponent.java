package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import rich.client.draggables.AbstractHudElement;
import rich.util.render.font.Fonts;
import rich.modules.impl.render.Hud;
import rich.util.ColorUtil;
import rich.util.render.Render2D;

import java.awt.Color;

public class InventoryComponent extends AbstractHudElement {

    private float toggleAnim = 1.0f;
    private float changeAnim = 1.0f;
    private String lastHash = "";

    public InventoryComponent() {
        super("Inventory", 400, 300, 180, 60, true);
        stopAnimation();
    }

    @Override
    public void tick() {
        if (mc.player == null) {
            toggleAnim = Math.max(0, toggleAnim - 0.05f);
            return;
        }
        toggleAnim = Math.min(1.0f, toggleAnim + 0.05f);

        StringBuilder sb = new StringBuilder();
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            sb.append(stack.getItem().toString()).append(stack.getCount());
        }
        String hash = sb.toString();
        if (!hash.equals(lastHash)) {
            changeAnim = 0;
            lastHash = hash;
        }
        changeAnim = Math.min(1.0f, changeAnim + 0.07f);
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(rich.util.animations.Direction.BACKWARDS);
    }

    @Override
    public void drawDraggable(DrawContext ctx, int alpha) {
        if (mc.player == null || alpha <= 0 || toggleAnim <= 0.01f) return;
        float a = alpha / 255.0f;
        float anim = toggleAnim * changeAnim;

        int themeColor = ColorUtil.astolfo(10000, 0, 0.7f, 0.8f, 255);
        int slotSize = 20;
        int columns = 9;
        int rows = 3;
        float gridWidth = columns * slotSize;
        float gridHeight = rows * slotSize;
        float x = getX();
        float y = getY();

        setWidth((int) gridWidth);
        setHeight((int) gridHeight);

        int bgAlpha = (int) (180 * a);
        Render2D.blur(x, y, gridWidth, gridHeight, 21, 4, new Color(255, 255, 255, (int) (255 * a)).getRGB());

        int graySlot = ColorUtil.rgba(35, 35, 35, bgAlpha);
        int lightSlot = ColorUtil.rgba(42, 42, 42, bgAlpha);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int slotIndex = 9 + row * 9 + col;
                ItemStack stack = mc.player.getInventory().getStack(slotIndex);
                float slotX = x + col * slotSize;
                float slotY = y + row * slotSize;
                int slotColor = (row + col) % 2 == 0 ? graySlot : lightSlot;
                Render2D.rect(slotX, slotY, slotSize, slotSize, slotColor, 2);

                if (!stack.isEmpty()) {
                    ctx.getMatrices().pushMatrix();
                    ctx.getMatrices().translate(slotX + (slotSize - 12.8f) / 2, slotY + (slotSize - 12.8f) / 2);
                    ctx.getMatrices().scale(0.8f, 0.8f);
                    ctx.drawItem(stack, 0, 0);
                    ctx.getMatrices().popMatrix();
                }
            }
        }

        Render2D.outline(x, y, gridWidth, gridHeight, 0.35f, ColorUtil.rgba(90, 90, 90, bgAlpha), 4);
        int cornerAlpha = (int) (200 * a);
        Render2D.arc(x, y, 20, 90, themeColor, 4);
        Render2D.arc(x + gridWidth, y, 20, 90, themeColor, 4);
        Render2D.arc(x, y + gridHeight, 20, 90, themeColor, 4);
        Render2D.arc(x + gridWidth, y + gridHeight, 20, 90, themeColor, 4);
    }
}
