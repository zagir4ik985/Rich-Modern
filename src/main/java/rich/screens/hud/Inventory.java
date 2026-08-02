package rich.screens.hud;

import net.minecraft.item.ItemStack;
import rich.screens.hud.port.Animation;
import rich.screens.hud.port.BorderRadius;
import rich.screens.hud.port.ColorRGBA;
import rich.screens.hud.port.CustomDrawContext;
import rich.screens.hud.port.DrawUtil;
import rich.screens.hud.port.Easing;
import rich.screens.hud.port.PortHudElement;
import rich.screens.hud.port.Theme;
import rich.util.render.font.Fonts;

public class Inventory extends PortHudElement {

    private final Animation toggleAnimation;
    private final Animation inventoryChangeAnimation;
    private String lastInventoryHash;
    private float lastWidth;
    private float lastHeight;

    public Inventory() {
        super("Inventory", 250, 150, 180, 60, true);
        this.toggleAnimation = new Animation(300L, Easing.SINE_IN_OUT);
        this.inventoryChangeAnimation = new Animation(150L, Easing.SINE_IN_OUT);
        this.lastInventoryHash = "";
        this.lastWidth = 0.0F;
        this.lastHeight = 0.0F;
    }

    @Override
    public void renderPort(CustomDrawContext ctx, int alpha) {
        if (mc.player == null) {
            this.toggleAnimation.update(0.0F);
            if (this.toggleAnimation.getValue() > 0.01F) {
                this.renderInventory(ctx, this.toggleAnimation.getValue());
            }
        } else {
            this.toggleAnimation.update(1.0F);
            if (!(this.toggleAnimation.getValue() <= 0.01F)) {
                String currentInventoryHash = "";

                for (int i = 9; i < 36; ++i) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    currentInventoryHash = currentInventoryHash + stack.getItem().toString() + stack.getCount();
                }

                if (!currentInventoryHash.equals(this.lastInventoryHash)) {
                    this.inventoryChangeAnimation.update(0.0F);
                    this.lastInventoryHash = currentInventoryHash;
                }

                this.inventoryChangeAnimation.update(1.0F);
                this.renderInventory(ctx, this.toggleAnimation.getValue() * this.inventoryChangeAnimation.getValue());
            }
        }
    }

    private void renderInventory(CustomDrawContext ctx, float animationValue) {
        if (mc.player == null) {
            Theme theme = new Theme();
            ColorRGBA bgColor = theme.getForegroundColor();
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(this.px + this.lastWidth / 2.0F, this.py + this.lastHeight / 2.0F);
            ctx.getMatrices().scale(animationValue, animationValue);
            ctx.getMatrices().translate(-(this.px + this.lastWidth / 2.0F), -(this.py + this.lastHeight / 2.0F));
            ctx.drawRoundedRect(this.px, this.py, this.lastWidth, this.lastHeight, BorderRadius.all(4.0F), bgColor);
            ctx.getMatrices().popMatrix();
        } else {
            float slotSize = 20.0F;
            float borderRadius = 4.0F;
            Theme theme = new Theme();
            ColorRGBA graySlotColor = theme.getForegroundColor();
            ColorRGBA themeSlotColor = theme.getForegroundLight();
            int columns = 9;
            int rows = 3;
            float gridWidth = (float) columns * slotSize;
            float gridHeight = (float) rows * slotSize;
            this.pw = gridWidth;
            this.ph = gridHeight;
            this.width = (int) gridWidth;
            this.height = (int) gridHeight;
            this.lastWidth = this.pw;
            this.lastHeight = this.ph;
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(this.px + this.pw / 2.0F, this.py + this.ph / 2.0F);
            ctx.getMatrices().scale(animationValue, animationValue);
            ctx.getMatrices().translate(-(this.px + this.pw / 2.0F), -(this.py + this.ph / 2.0F));
            DrawUtil.drawBlurHud(ctx.getMatrices(), this.px, this.py, this.pw, this.ph, 21.0F, BorderRadius.all(4.0F), ColorRGBA.WHITE);

            for (int row = 0; row < rows; ++row) {
                for (int col = 0; col < columns; ++col) {
                    int slotIndex = 9 + row * 9 + col;
                    ItemStack stack = mc.player.getInventory().getStack(slotIndex);
                    float slotX = this.px + (float) col * slotSize;
                    float slotY = this.py + (float) row * slotSize;
                    ColorRGBA slotColor = (row + col) % 2 == 0 ? graySlotColor : themeSlotColor;
                    float round = 4.0F;
                    BorderRadius radius = col == 0 && row == 0 ? BorderRadius.top(round, 0.0F) : (col == 8 && row == 0 ? BorderRadius.top(0.0F, round) : (col == 0 && row == 2 ? BorderRadius.bottom(round, 0.0F) : (col == 8 && row == 2 ? BorderRadius.bottom(0.0F, round) : BorderRadius.ZERO)));
                    ctx.drawRoundedRect(slotX, slotY, slotSize, slotSize, radius, slotColor);
                    if (!stack.isEmpty()) {
                        ctx.pushMatrix();
                        ctx.getMatrices().translate(slotX + (slotSize - 12.8F) / 2.0F, slotY + (slotSize - 12.8F) / 2.0F);
                        ctx.getMatrices().scale(0.8F, 0.8F);
                        ctx.drawItem(stack, 0, 0);
                        ctx.drawItemBar(stack, 0, 0);
                        ctx.drawCooldownProgress(stack, 0, 0);
                        ctx.popMatrix();
                    }
                }
            }

            ctx.drawRoundedBorder(this.px, this.py, gridWidth, gridHeight, 0.1F, BorderRadius.all(4.0F), theme.getForegroundStroke());
            DrawUtil.drawRoundedCorner(ctx.getMatrices(), this.px, this.py, gridWidth, gridHeight, 0.1F, 20.0F, theme.getColor(), BorderRadius.all(4.0F));
            ctx.getMatrices().popMatrix();
        }
    }
}
