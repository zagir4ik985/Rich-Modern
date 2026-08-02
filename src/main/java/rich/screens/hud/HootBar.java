package rich.screens.hud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.GameMode;
import rich.mixin.InGameHudAccessor;
import rich.screens.hud.port.Animation;
import rich.screens.hud.port.BorderRadius;
import rich.screens.hud.port.ColorRGBA;
import rich.screens.hud.port.ColorUtil;
import rich.screens.hud.port.CustomDrawContext;
import rich.screens.hud.port.DrawUtil;
import rich.screens.hud.port.Easing;
import rich.screens.hud.port.PortHudElement;
import rich.screens.hud.port.Theme;
import rich.util.render.font.Fonts;

import java.util.ArrayList;
import java.util.List;

public class HootBar extends PortHudElement {

    private final List<HotBarSlot> slots = new ArrayList<>();

    public HootBar() {
        super("HootBar", 50, 0, 216, 24, true);
        float itemSize = 24.0F;
        this.pw = itemSize * 9.0F;
        this.ph = itemSize;
        this.width = (int) this.pw;
        this.height = (int) this.ph;

        for (int i = 0; i < 9; ++i) {
            this.slots.add(new HotBarSlot(i));
        }
    }

    @Override
    public void renderPort(CustomDrawContext ctx, int alpha) {
        this.px = ((float) ctx.getScaledWindowWidth() - this.pw) / 2.0F;
        this.x = (int) this.px;
        float posX = this.getPx();
        float posY = this.getPy();
        Theme theme = new Theme();
        int k;
        ItemStack offHand;
        float xSlot;
        String countText;
        float countWidth;
        float countX;
        float countY;
        HotBarSlot slot;

        InGameHudAccessor hudAccessor = (InGameHudAccessor) mc.inGameHud;

        if (mc.interactionManager.getCurrentGameMode() == GameMode.CREATIVE) {
            this.renderHeldItemTooltip(ctx, posY - 35.0F);
            this.renderOverlayMessage(ctx, mc.getRenderTickCounter(), posY - 35.0F - 9.0F);
                        k = mc.player.experienceLevel;
                ctx.drawText(Fonts.MEDIUM, String.valueOf(k), posX + this.pw / 2.0F - Fonts.MEDIUM.getWidth(String.valueOf(k), 7.0F) / 2.0F, posY - 15.0F + Fonts.MEDIUM.getHeight(7.0F) / 2.0F, 7.0F, ColorRGBA.GREEN);
            DrawUtil.drawBlurHud(ctx.getMatrices(), this.px, this.py, this.pw, this.ph, 21.0F, BorderRadius.all(4.0F), ColorRGBA.WHITE);
            ctx.drawRoundedRect(posX, posY, this.pw, 24.0F, BorderRadius.all(4.0F), theme.getForegroundColor());
            offHand = mc.player.getOffHandStack();
            if (!offHand.isEmpty()) {
                xSlot = posX - this.ph - 12.0F;
                DrawUtil.drawBlurHud(ctx.getMatrices(), xSlot, posY, this.ph, this.ph, 21.0F, BorderRadius.all(4.0F), ColorRGBA.WHITE);
                ctx.drawRoundedRect(xSlot, posY, this.ph, this.ph, BorderRadius.all(4.0F), theme.getForegroundColor());
                ctx.drawRoundedBorder(xSlot, posY, this.ph, this.ph, 0.1F, BorderRadius.all(4.0F), theme.getForegroundStroke());
                DrawUtil.drawRoundedCorner(ctx.getMatrices(), posX - this.ph - 12.0F, posY, this.ph, this.ph, 0.1F, 15.0F, theme.getColor(), BorderRadius.all(4.0F));
                ctx.pushMatrix();
                ctx.getMatrices().translate(xSlot + 5.6F, posY + 5.6F);
                ctx.getMatrices().scale(0.8F, 0.8F);
                ctx.drawItem(offHand, 0, 0);
                ctx.drawItemBar(offHand, 0, 0);
                ctx.drawCooldownProgress(offHand, 0, 0);
                ctx.popMatrix();
                if (offHand.getCount() > 1) {
                    countText = "x" + offHand.getCount();
                    countWidth = Fonts.MEDIUM.getWidth(countText, 7.0F);
                    countX = xSlot + 24.0F - countWidth - 1.0F;
                    countY = posY + 24.0F - Fonts.MEDIUM.getHeight(7.0F) - 3.0F;
                    ctx.drawText(Fonts.MEDIUM, countText, countX, countY, 7.0F, theme.getGray());
                }
            }

            xSlot = posX;

            for (HotBarSlot hotBarSlot : this.slots) {
                slot = hotBarSlot;
                slot.render(ctx, xSlot, posY, theme);
                xSlot += this.ph;
            }

            ctx.drawRoundedBorder(this.px, this.py, this.pw, 24.0F, 0.1F, BorderRadius.all(4.0F), theme.getForegroundStroke());            DrawUtil.drawRoundedCorner(ctx.getMatrices(), this.px, this.py, this.pw, 24.0F, 0.1F, 15.0F, theme.getColor(), BorderRadius.all(4.0F));
        } else {
            if (mc.interactionManager.hasStatusBars()) {
                ctx.pushMatrix();
                ctx.getMatrices().translate((float) (-(ctx.getScaledWindowWidth() / 2 - 91)), (float) (-(ctx.getScaledWindowHeight() - 39)));
                ctx.getMatrices().scale(1.0F, 1.0F);
                ctx.getMatrices().translate(posX, 0.0F);
                ctx.getMatrices().translate(0.0F, posY - 15.0F);
                if (!(mc.interactionManager.getCurrentGameMode() == GameMode.CREATIVE)) {
                    hudAccessor.invokeRenderStatusBars(ctx.getRawContext());
                }

                ctx.popMatrix();
                this.renderHeldItemTooltip(ctx, posY - 35.0F);
                this.renderOverlayMessage(ctx, mc.getRenderTickCounter(), posY - 35.0F - 9.0F);
                                k = mc.player.experienceLevel;
            ctx.drawText(Fonts.MEDIUM, String.valueOf(k), posX + this.pw / 2.0F - Fonts.MEDIUM.getWidth(String.valueOf(k), 7.0F) / 2.0F, posY - 15.0F + Fonts.MEDIUM.getHeight(7.0F) / 2.0F, 7.0F, ColorRGBA.GREEN);
                DrawUtil.drawBlurHud(ctx.getMatrices(), this.px, this.py, this.pw, this.ph, 21.0F, BorderRadius.all(4.0F), ColorRGBA.WHITE);
                DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, this.pw, 24.0F, 11.0F, BorderRadius.all(4.0F), new ColorRGBA(80, 80, 80, 255));
                offHand = mc.player.getOffHandStack();
                if (!offHand.isEmpty()) {
                    xSlot = posX - this.ph - 12.0F;
                    DrawUtil.drawBlurHud(ctx.getMatrices(), xSlot, posY, this.ph, this.ph, 21.0F, BorderRadius.all(4.0F), ColorRGBA.WHITE);
                    ctx.drawRoundedRect(xSlot, posY, this.ph, this.ph, BorderRadius.all(4.0F), theme.getForegroundColor());
                    ctx.drawRoundedBorder(xSlot, posY, this.ph, this.ph, 0.1F, BorderRadius.all(4.0F), theme.getForegroundStroke());
                    DrawUtil.drawRoundedCorner(ctx.getMatrices(), posX - this.ph - 12.0F, posY, this.ph, this.ph, 0.1F, 15.0F, theme.getColor(), BorderRadius.all(4.0F));
                    ctx.pushMatrix();
                    ctx.getMatrices().translate(xSlot + 5.6F, posY + 5.6F);
                    ctx.getMatrices().scale(0.8F, 0.8F);
                    ctx.drawItem(offHand, 0, 0);
                    ctx.drawItemBar(offHand, 0, 0);
                    ctx.drawCooldownProgress(offHand, 0, 0);
                    ctx.popMatrix();
                    if (offHand.getCount() > 1) {
                        countText = "x" + offHand.getCount();
                        countWidth = Fonts.MEDIUM.getWidth(countText, 7.0F);
                        countX = xSlot + 24.0F - countWidth - 1.0F;
                        countY = posY + 24.0F - Fonts.MEDIUM.getHeight(7.0F) - 3.0F;
                        ctx.drawText(Fonts.MEDIUM, countText, countX, countY, 7.0F, theme.getGray());
                    }
                }

                xSlot = posX;

                for (HotBarSlot hotBarSlot : this.slots) {
                    slot = hotBarSlot;
                    slot.render(ctx, xSlot, posY, theme);
                    xSlot += this.ph;
                }
            }
        }
    }

    private void renderHeldItemTooltip(CustomDrawContext context, float y) {
        InGameHudAccessor hudAccessor = (InGameHudAccessor) mc.inGameHud;
        Profilers.get().push("selectedItemName");
        if (hudAccessor.getHeldItemTooltipFade() > 0 && !hudAccessor.getCurrentStack().isEmpty()) {
            MutableText mutableText = Text.empty().append(hudAccessor.getCurrentStack().getName()).formatted(hudAccessor.getCurrentStack().getRarity().getFormatting());
            if (hudAccessor.getCurrentStack().contains(DataComponentTypes.CUSTOM_NAME)) {
                mutableText.formatted(Formatting.ITALIC);
            }

            int i = mc.textRenderer.getWidth(mutableText);
            int j = (context.getScaledWindowWidth() - i) / 2;
            int k = (int) y;
            if (!mc.interactionManager.hasStatusBars() || mc.interactionManager.getCurrentGameMode() == GameMode.CREATIVE) {
                k += 14;
            }

            int l = (int) ((float) hudAccessor.getHeldItemTooltipFade() * 256.0F / 10.0F);
            if (l > 255) {
                l = 255;
            }

            if (l > 0) {
                context.getMatrices().pushMatrix();
                context.getMatrices().translate((float) j, (float) k);
                Theme theme = new Theme();
                context.drawTextWithBackground(mc.inGameHud.getTextRenderer(), mutableText, 0, 0, i, ColorHelper.withAlpha(l, -1));
                context.getMatrices().popMatrix();
            }
        }

        Profilers.get().pop();
    }

    private void renderOverlayMessage(CustomDrawContext context, RenderTickCounter tickCounter, float y) {
        InGameHudAccessor hudAccessor = (InGameHudAccessor) mc.inGameHud;
        TextRenderer textRenderer = mc.inGameHud.getTextRenderer();
        if (hudAccessor.getOverlayMessage() != null && hudAccessor.getOverlayRemaining() > 0) {
            Profilers.get().push("overlayMessage");
            float f = (float) hudAccessor.getOverlayRemaining() - tickCounter.getTickProgress(false);
            int i = (int) (f * 255.0F / 20.0F);
            if (i > 255) {
                i = 255;
            }

            if (i > 8) {
                context.getMatrices().pushMatrix();
                context.getMatrices().translate((float) (context.getScaledWindowWidth() / 2), y);
                int j;
                if (hudAccessor.getOverlayTinted()) {
                    j = MathHelper.hsvToArgb(f / 50.0F, 0.7F, 0.6F, i);
                } else {
                    j = ColorHelper.withAlpha(i, -1);
                }

                int k = textRenderer.getWidth(hudAccessor.getOverlayMessage());
                context.getMatrices().translate((float) (-k) / 2.0F, -4.0F);
                context.drawTextWithBackground(textRenderer, hudAccessor.getOverlayMessage(), 0, 0, k, j);
                context.getMatrices().popMatrix();
            }

            Profilers.get().pop();
        }
    }

    private class HotBarSlot {
        private final Animation animationEnable;
        private final BorderRadius borderRadius;
        private final int index;

        public HotBarSlot(int index) {
            this.animationEnable = new Animation(150L, 0.0F, Easing.QUAD_IN_OUT);
            this.borderRadius = index == 0 ? BorderRadius.left(4.0F, 4.0F) : (index == 8 ? BorderRadius.right(4.0F, 4.0F) : BorderRadius.ZERO);
            this.index = index;
        }

        public void render(CustomDrawContext ctx, float x, float y, Theme theme) {
            this.animationEnable.setDuration(80L);
            this.animationEnable.update(this.index == mc.player.getInventory().getSelectedSlot() ? 1.0F : 0.0F);
            ColorRGBA bgColor = ColorUtil.interpolate(ColorRGBA.TRANSPARENT, theme.getColor(), this.animationEnable.getValue());
            ColorRGBA textColor = theme.getGray().mix(theme.getWhite(), this.animationEnable.getValue());
            ItemStack stack = mc.player.getInventory().getMainStacks().get(this.index);
            ctx.drawRoundedRect(x, y, 24.0F, 24.0F, this.borderRadius, bgColor);
            ctx.pushMatrix();
            ctx.getMatrices().translate(x + 5.6F, y + 5.6F);
            ctx.getMatrices().scale(0.8F, 0.8F);
            ctx.drawItem(stack, 0, 0);
            ctx.drawItemBar(stack, 0, 0);
            ctx.drawCooldownProgress(stack, 0, 0);
            ctx.popMatrix();
            ctx.drawText(Fonts.MEDIUM, String.valueOf(this.index + 1), x + 2.0F, y + 2.0F, 6.0F, textColor);
            if (stack.getCount() > 1) {
                String countText = "x" + stack.getCount();
                float countWidth = Fonts.MEDIUM.getWidth(countText, 6.0F);
                float countX = x + 24.0F - countWidth - 1.0F;
                float countY = y + 24.0F - Fonts.MEDIUM.getHeight(6.0F) - 3.0F;
                ctx.drawText(Fonts.MEDIUM, countText, countX, countY, 6.0F, textColor);
            }
        }
    }
}
