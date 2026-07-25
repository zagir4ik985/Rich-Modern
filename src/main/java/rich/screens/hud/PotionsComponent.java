package rich.screens.hud;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import rich.client.draggables.AbstractHudElement;
import rich.util.render.font.Fonts;
import rich.modules.impl.render.Hud;
import rich.util.ColorUtil;
import rich.util.render.Render2D;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class PotionsComponent extends AbstractHudElement {

    private float animatedWidth = 80;
    private float alphaAnim = 0;
    private final List<PotionItem> potionItems = new CopyOnWriteArrayList<>();

    public PotionsComponent() {
        super("Potions", 10, 150, 80, 23, true);
        stopAnimation();
    }

    @Override
    public void tick() {
        if (mc.player == null) return;
        updatePotions();

        boolean isFound = false;
        for (PotionItem item : potionItems) {
            float target = item.active ? 1 : 0;
            item.animVal += (target - item.animVal) * 0.08f;
            if (item.animVal > 0.01f) isFound = true;
        }
        potionItems.removeIf(item -> !item.active && item.animVal <= 0.01f);

        float targetAlpha = (isFound || mc.currentScreen instanceof ChatScreen) ? 1.0f : 0.0f;
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

        potionItems.sort(Comparator.comparing(pi -> pi.name));

        Render2D.blur(x, y, animatedWidth, 14.5f, 15, 2, new Color(255, 255, 255, (int) (255 * a)).getRGB());
        Render2D.rect(x, y, animatedWidth, 14.5f, ColorUtil.rgba(30, 30, 30, bgAlpha), 2);

        Fonts.HUD_ICONS.draw("E", x + 4, y + 2, 9, themeColor);
        Fonts.BOLD.draw("|", x + 15.25f, y + 2, 8.3f, new Color(166, 166, 166, (int) (255 * a)).getRGB());
        Fonts.BOLD.draw("Active potions", x + 18.5f, y + 2, 7.5f, new Color(255, 255, 255, (int) (255 * a)).getRGB());
        y += 15.5f;

        for (PotionItem item : potionItems) {
            if (item.animVal < 0.01f) continue;

            String name = I18n.translate(item.name);
            String amp = String.valueOf(item.amplifier + 1);
            int totalSeconds = item.durationTicks / 20;
            String duration = String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);

            float elemAlpha = item.animVal * a;
            float elemY = y + item.animVal * 3 - 3;
            height += 12 * item.animVal;

            Render2D.blur(x, elemY, animatedWidth, 11, 15, 2, new Color(255, 255, 255, (int) (255 * elemAlpha)).getRGB());
            Render2D.rect(x, elemY, animatedWidth, 11, ColorUtil.rgba(30, 30, 30, (int) (64 * elemAlpha)), 2);

            Fonts.BOLD.draw(name, x + 5, elemY + 3, 7, new Color(255, 255, 255, (int) (255 * elemAlpha)).getRGB());

            if (item.amplifier > 0) {
                float ampX = x + 5 + Fonts.BOLD.getWidth(name, 7);
                Fonts.REGULAR.draw(" " + amp, ampX, elemY + 3, 6.75f, ColorUtil.replAlpha(themeColor, (int) (255 * elemAlpha)));
            }

            float timerX = x + animatedWidth - 18 - Fonts.REGULAR.getWidth(duration, 6.75f);
            Fonts.REGULAR.draw(duration, timerX, elemY + 3, 6.5f, new Color(255, 255, 255, (int) (255 * elemAlpha)).getRGB());

            float sepX = x + animatedWidth - 12;
            Fonts.REGULAR.draw("|", sepX, elemY + 3, 6.5f, new Color(166, 166, 166, (int) (255 * elemAlpha)).getRGB());

            Identifier icon = getEffectIcon(item.effectType);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, icon, (int) (x + animatedWidth - 9), (int) (elemY + 2.25f), 0, 0, 6, 6, 64, 64);

            float elementsWidth = Fonts.BOLD.getWidth(name, 7) + Fonts.REGULAR.getWidth(duration, 6.75f) + 35;
            if (elementsWidth > defaultWidth) defaultWidth = elementsWidth;
            y += 12 * item.animVal;
        }

        animatedWidth += (defaultWidth - animatedWidth) * 0.1f;
        setWidth((int) animatedWidth);
        setHeight((int) height);
    }

    private void updatePotions() {
        if (mc.player == null) return;
        Map<String, StatusEffectInstance> current = new HashMap<>();
        for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
            String key = Text.translatable(effect.getTranslationKey()).getString() + ":" + effect.getAmplifier();
            current.put(key, effect);
        }
        for (PotionItem item : potionItems) {
            String key = item.name + ":" + item.amplifier;
            StatusEffectInstance effect = current.get(key);
            if (effect != null) {
                item.durationTicks = effect.getDuration();
                item.active = true;
                current.remove(key);
            } else {
                item.active = false;
            }
        }
        for (Map.Entry<String, StatusEffectInstance> entry : current.entrySet()) {
            StatusEffectInstance eff = entry.getValue();
            potionItems.add(new PotionItem(Text.translatable(eff.getTranslationKey()).getString(), eff.getAmplifier(), eff.getDuration(), eff.getEffectType().value()));
        }
    }

    private Identifier getEffectIcon(StatusEffect effect) {
        String id = effect.getTranslationKey().replace("effect.minecraft.", "").replace("effect.", "");
        return Identifier.of("minecraft", "textures/mob_effect/" + id + ".png");
    }

    private static class PotionItem {
        String name;
        int amplifier;
        int durationTicks;
        boolean active;
        StatusEffect effectType;
        float animVal = 0;
        PotionItem(String name, int amplifier, int durationTicks, StatusEffect effectType) {
            this.name = name;
            this.amplifier = amplifier;
            this.durationTicks = durationTicks;
            this.active = true;
            this.effectType = effectType;
        }
    }
}
