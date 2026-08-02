package rich.screens.hud.port;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import rich.util.render.Render2D;
import rich.util.render.font.Font;

public class CustomDrawContext {

    private final DrawContext context;

    public CustomDrawContext(DrawContext context) {
        this.context = context;
    }

    public DrawContext getRawContext() {
        return context;
    }

    public Matrix3x2fStack getMatrices() {
        return context.getMatrices();
    }

    public void pushMatrix() {
        context.getMatrices().pushMatrix();
    }

    public void popMatrix() {
        context.getMatrices().popMatrix();
    }

    public void drawText(Font font, String text, float x, float y, float size, ColorRGBA color) {
        font.draw(text, x, y, size, color.toArgb());
    }

    public void drawText(Font font, Text text, float x, float y, float size, ColorRGBA color) {
        font.draw(text.getString(), x, y, size, color.toArgb());
    }

    public void drawText(Font font, String text, float x, float y, float size, float alpha255) {
        font.draw(text, x, y, size, new ColorRGBA(255, 255, 255, alpha255).toArgb());
    }

    public void drawItem(ItemStack stack, int x, int y) {
        context.drawItem(stack, x, y);
    }

    public void drawItem(ItemStack stack, int x, int y, int seed) {
        context.drawItem(stack, x, y, seed);
    }

    public void drawItemBar(ItemStack stack, int x, int y) {
        if (!stack.isItemBarVisible()) {
            return;
        }
        int step = stack.getItemBarStep();
        int color = stack.getItemBarColor();
        Render2D.rect(x + 2, y + 13, 12, 2, 0xFF000000, 0.0F);
        Render2D.rect(x + 2, y + 13, step, 1, color | 0xFF000000, 0.0F);
    }

    public void drawItemBar(ItemStack stack, int x, int y, int seed) {
        drawItemBar(stack, x, y);
    }

    public void drawCooldownProgress(ItemStack stack, int x, int y) {
        float progress = MinecraftClient.getInstance().player.getItemCooldownManager().getCooldownProgress(stack, 0.0F);
        if (progress <= 0.0F) {
            return;
        }
        int top = y + Math.round(16.0F * (1.0F - progress));
        if (progress < 1.0F) {
            Render2D.rect(x, top, 16, y + 16 - top - 1, 0x80A0A0A0, 0.0F);
        } else {
            Render2D.rect(x, y, 16, top - y, 0xFF000000, 0.0F);
        }
    }

    public void drawCooldownProgress(ItemStack stack, int x, int y, float tickDelta) {
        drawCooldownProgress(stack, x, y);
    }

    public void drawTextWithBackground(TextRenderer textRenderer, Text text, int x, int y, int width, int color) {
        context.drawTextWithBackground(textRenderer, text, x, y, width, color);
    }

    public void drawTexture(Identifier id, float x, float y, float width, float height, ColorRGBA color) {
        Render2D.texture(id, x, y, width, height, color.toArgb());
    }

    public void drawRoundedRect(float x, float y, float width, float height, BorderRadius radius, ColorRGBA color) {
        DrawUtil.drawRoundedRect(getMatrices(), x, y, width, height, radius, color);
    }

    public void drawRoundedBorder(float x, float y, float width, float height, float thickness, BorderRadius radius, ColorRGBA color) {
        DrawUtil.drawRoundedBorder(getMatrices(), x, y, width, height, thickness, radius, color);
    }

    public int getScaledWindowWidth() {
        return MinecraftClient.getInstance().getWindow().getScaledWidth();
    }

    public int getScaledWindowHeight() {
        return MinecraftClient.getInstance().getWindow().getScaledHeight();
    }
}
