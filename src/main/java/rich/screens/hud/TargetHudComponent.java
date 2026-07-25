package rich.screens.hud;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import rich.client.draggables.AbstractHudElement;
import rich.util.render.font.Fonts;
import rich.modules.impl.render.Hud;
import rich.modules.impl.player.NameProtect;
import rich.util.ColorUtil;
import rich.util.render.Render2D;

import java.awt.Color;

public class TargetHudComponent extends AbstractHudElement {

    private float toggleAnim = 0;
    private float healthAnim = 0;
    private float outdatedHealthAnim = 0;
    private float gappleAnim = 0;
    private LivingEntity target;

    public TargetHudComponent() {
        super("TargetHud", 200, 200, 101, 40, true);
        stopAnimation();
    }

    @Override
    public void tick() {
        LivingEntity newTarget = findTarget();
        if (newTarget == null) {
            toggleAnim = Math.max(0, toggleAnim - 0.05f);
            if (toggleAnim <= 0) target = null;
        } else {
            target = newTarget;
            toggleAnim = Math.min(1, toggleAnim + 0.05f);
        }

        if (target != null) {
            float hp = target.getHealth();
            float maxHp = target.getMaxHealth();
            float targetHealth = maxHp > 0 ? hp / maxHp : 0;
            healthAnim += (targetHealth - healthAnim) * 0.1f;

            if (outdatedHealthAnim < healthAnim) {
                outdatedHealthAnim = healthAnim;
            } else {
                outdatedHealthAnim += (targetHealth - outdatedHealthAnim) * 0.05f;
            }

            float absorption = maxHp > 0 ? target.getAbsorptionAmount() / maxHp : 0;
            gappleAnim += (absorption - gappleAnim) * 0.1f;
        }
    }

    @Override
    public boolean visible() {
        return (target != null && toggleAnim > 0.01f) || !scaleAnimation.isFinished(rich.util.animations.Direction.BACKWARDS);
    }

    @Override
    public void drawDraggable(DrawContext ctx, int alpha) {
        if (target == null || toggleAnim < 0.01f || alpha <= 0) return;
        float a = (alpha / 255.0f) * toggleAnim;
        int themeColor = ColorUtil.astolfo(10000, 0, 0.7f, 0.8f, (int) (255 * a));
        int themeSecondColor = ColorUtil.astolfo(10000, 300, 0.7f, 0.8f, (int) (255 * a));

        float x = getX();
        float y = getY();
        float width = 100.5f;
        float height = 40;
        int bgAlpha = (int) (64 * a);

        Render2D.blur(x, y, width, height, 15, 5, ColorUtil.replAlpha(new Color(255, 255, 255).getRGB(), (int) (255 * a)));
        Render2D.rect(x, y, width, height, ColorUtil.rgba(30, 30, 30, bgAlpha), 5);

        Identifier skinTextures = null;
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                if (entry.getProfile().name().equals(target.getNameForScoreboard())) {
                    skinTextures = entry.getSkinTextures().body().texturePath();
                    break;
                }
            }
        }
        if (skinTextures == null) skinTextures = DefaultSkinHelper.getSteve().body().texturePath();

        float headSize = 32;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, skinTextures, (int) (x + 4), (int) (y + 4), 0, 0, (int) headSize, (int) headSize, 64, 64);
        Render2D.outline(x + 4, y + 4, headSize, headSize, 0.5f, ColorUtil.replAlpha(new Color(255, 255, 255).getRGB(), (int) (255 * a)), 3);

        String playerName = target == mc.player ? NameProtect.getCustomName() : target.getNameForScoreboard();
        if (playerName.length() > 11) playerName = playerName.substring(0, 7) + "...";
        Fonts.BOLD.draw(playerName, x + headSize + 8, y + 3, 8, ColorUtil.replAlpha(new Color(255, 255, 255).getRGB(), (int) (255 * a)));

        float hp = target.getHealth();
        String hpText = "HP: " + String.format("%.1f", hp).replace(",", ".")
            + (target.getAbsorptionAmount() > 0 ? " (" + String.format("%.1f", target.getAbsorptionAmount()).replace(",", ".") + ")" : "");
        Fonts.BOLD.draw(hpText, x + headSize + 8, y + 14, 6.5f, ColorUtil.replAlpha(new Color(255, 255, 255).getRGB(), (int) (255 * a)));

        float barX = x + headSize + 7.2f;
        float barY = y + 27.8f;
        float barWidth = width - headSize - 12;
        float barHeight = 7.4f;

        Render2D.rect(barX, barY, barWidth, barHeight, ColorUtil.rgba(50, 50, 50, (int) (150 * a)), 3);

        float healthWidth = MathHelper.clamp(barWidth * healthAnim, 0, barWidth);
        if (healthWidth > 0) {
            Render2D.rect(barX, barY, healthWidth, barHeight, ColorUtil.interpolate(themeSecondColor, themeColor, 0.5f), 3);
        }

        float absorptionWidth = MathHelper.clamp(barWidth * gappleAnim, 0, barWidth);
        if (absorptionWidth > 0) {
            Render2D.rect(barX, barY, absorptionWidth, barHeight, ColorUtil.rgba(255, 220, 0, (int) (255 * a)), 3);
        }

        if (target instanceof PlayerEntity player) {
            drawArmor(ctx, player, x + width - 65, y - 12);
        }

        setWidth((int) width);
        setHeight((int) height);
    }

    private void drawArmor(DrawContext ctx, PlayerEntity player, float posX, float posY) {
        float boxSize = 10;
        float iconX = posX + (5 - toggleAnim * 5);
        float iconY = posY + 1 + (5 - toggleAnim * 5);
        ItemStack[] items = {
            player.getMainHandStack(), player.getOffHandStack(),
            player.getEquippedStack(EquipmentSlot.HEAD), player.getEquippedStack(EquipmentSlot.CHEST),
            player.getEquippedStack(EquipmentSlot.LEGS), player.getEquippedStack(EquipmentSlot.FEET)
        };

        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate(iconX + (boxSize - 9.6f) / 2, iconY + (boxSize - 9.6f) / 2);
                ctx.getMatrices().scale(0.6f * toggleAnim, 0.6f * toggleAnim);
                ctx.drawItem(stack, 0, 0);
                ctx.getMatrices().popMatrix();
                iconX += boxSize;
            }
        }
    }

    private LivingEntity findTarget() {
        if (mc.currentScreen instanceof ChatScreen) return mc.player;
        if (mc.world == null || mc.player == null) return null;
        LivingEntity closest = null;
        double closestDist = 6.0;
        for (var entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living && living != mc.player && living.isAlive()) {
                double dist = mc.player.distanceTo(living);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = living;
                }
            }
        }
        return closest;
    }
}
