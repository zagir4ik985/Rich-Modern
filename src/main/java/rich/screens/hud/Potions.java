package rich.screens.hud;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.screens.hud.port.Animation;
import rich.screens.hud.port.BorderRadius;
import rich.screens.hud.port.ColorRGBA;
import rich.screens.hud.port.CustomDrawContext;
import rich.screens.hud.port.DrawUtil;
import rich.screens.hud.port.Easing;
import rich.screens.hud.port.PortHudElement;
import rich.screens.hud.port.Theme;
import rich.util.render.font.Fonts;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class Potions extends PortHudElement {

    private final BooleanSetting s1 = new BooleanSetting("1", "Показ списка зелий");
    private final BooleanSetting s2 = new BooleanSetting("2", "Фон элемента");
    private final BooleanSetting s3 = new BooleanSetting("3", "Разделитель");
    private final BooleanSetting s4 = new BooleanSetting("4", "Иконка эффекта");
    private final BooleanSetting s5 = new BooleanSetting("5", "Название эффекта");
    private final BooleanSetting s6 = new BooleanSetting("6", "Уровень эффекта");
    private final BooleanSetting s7 = new BooleanSetting("7", "Таймер эффекта");
    private final Animation widthAnimation;
    private final Animation xLine;
    private final Animation alpha;
    private final List<PotionItem> potionItems;

    private static final float blurStrength = 15.0f;
    private static final float cornerRadius = 2.25f;

    public Potions() {
        super("Potions", 200, 40, 75, 30, true);
        this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
        this.xLine = new Animation(170L, Easing.SINE_OUT);
        this.alpha = new Animation(200L, Easing.CUBIC_OUT);
        this.potionItems = new CopyOnWriteArrayList<>();
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
        if (mc.player != null) {
            this.updatePotions();
            float posX = this.getPx();
            float posY = this.getPy();
            float defaultWidth = 75.0F;
            float height = 14.5F;
            this.potionItems.sort(Comparator.comparing((pi) -> pi.name));
            boolean isFound = false;
            float durationWidth = 0.0F;
            Iterator<PotionItem> var8 = this.potionItems.iterator();

            String duration;
            while (var8.hasNext()) {
                PotionItem item = var8.next();
                item.animation.update(item.active ? 1.0F : 0.0F);
                if (item.animation.getValue() != 0.0F) {
                    int seconds = item.durationTicks / 20;
                    int minutes = seconds / 60;
                    int sec = seconds % 60;
                    duration = String.format("%d:%02d", minutes, sec);
                    durationWidth = Fonts.SEMIBOLD.getWidth(duration, 6.75F) + 4.0F;
                    height += 11.0F * item.animation.getValue();
                    if (item.animation.getValue() != 0.0F) {
                        this.alpha.update(1.0F);
                        isFound = true;
                    }
                }
            }

            this.xLine.update(durationWidth);
            if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
                this.alpha.update(0.0F);
            }

            if (mc.currentScreen instanceof ChatScreen) {
                this.alpha.update(1.0F);
            }

            Theme theme = new Theme();

            drawBlurBackground(ctx, posX, posY, this.widthAnimation.getValue(), 14.5F, theme, this.alpha.getValue());

            ctx.drawText(Fonts.NURIKI, "E", posX + 4F, posY + 5.5F, 9F, theme.getColor().withAlpha(255.0F * this.alpha.getValue()));

            ctx.drawText(Fonts.SEMIBOLD, ":", posX + 15.25F, posY + 4F, 8.3F, new ColorRGBA(166, 166, 166, 255.0F * this.alpha.getValue()));

            ctx.drawText(Fonts.SEMIBOLD, "Active potions", posX + 18.5F, posY + 4.75F, 7.5F, (new ColorRGBA(-1)).withAlpha(255.0F * this.alpha.getValue()));
            posY += 14.5F + 1.0F;
            if (this.s1.isValue()) {
                Iterator<PotionItem> var17 = this.potionItems.iterator();

                while (var17.hasNext()) {
                    PotionItem item = var17.next();
                    if (item.animation.getValue() != 0.0F) {
                        String name = I18n.translate(item.name, new Object[0]);
                        String amp = this.getAmplifierText(item.amplifier);
                        duration = this.formatDuration(item.durationTicks);
                        Identifier icon = this.getEffectIcon(item.effect.getEffectType().value());
                        height += 11.0F + 1.0F;
                        float elementsWidth = Fonts.SEMIBOLD.getWidth(name, 7.0F) + Fonts.SEMIBOLD.getWidth(amp, 6.75F) + Fonts.SEMIBOLD.getWidth(duration, 6.75F) + 35.0F;

                        float elementAlpha = item.animation.getValue() * this.alpha.getValue();
                        float elementY = posY + item.animation.getValue() * 3.0F - 3.0F;

                        if (this.s2.isValue()) {
                            drawBlurBackground(ctx, posX, elementY, this.widthAnimation.getValue(), 11.0F, theme, elementAlpha);
                        }

                        if (this.s5.isValue()) {
                            ctx.drawText(Fonts.SEMIBOLD, name, posX + 5.0F, elementY + 3.25F, 7.0F, (new ColorRGBA(-1)).withAlpha(elementAlpha * 255.0F));
                        }

                        if (Integer.parseInt(amp) > 0 && this.s6.isValue()) {
                            ctx.drawText(Fonts.SEMIBOLD, "   " + amp, posX + Fonts.SEMIBOLD.getWidth(name, 7.0F) + 5.0F, elementY + 3.25F, 6.75F, theme.getColor().withAlpha(elementAlpha * 255.0F));
                        }

                        if (this.s7.isValue()) {
                            float timerX = posX + this.widthAnimation.getValue() - 18.0F - Fonts.SEMIBOLD.getWidth(duration, 6.75F);
                            ctx.drawText(Fonts.SEMIBOLD, duration, timerX, elementY + 3.25F, 6.5F, (new ColorRGBA(-1)).withAlpha(elementAlpha * 255.0F));
                        }

                        if (this.s3.isValue()) {
                            float separatorX = posX + this.widthAnimation.getValue() - 12.0F;
                            ctx.drawText(Fonts.SEMIBOLD, ":", separatorX, elementY + 3.25F, 6.5F, new ColorRGBA(166, 166, 166, 255.0F * elementAlpha));
                        }

                        if (this.s4.isValue()) {
                            ctx.drawTexture(icon, posX + this.widthAnimation.getValue() - 9.0F, elementY + 2.25F, 6.25F, 6.25F, ColorRGBA.WHITE.withAlpha(elementAlpha * 255.0F));
                        }

                        if (elementsWidth > defaultWidth) {
                            defaultWidth = elementsWidth;
                        }

                        posY += (11.0F + 1.0F) * item.animation.getValue();
                    }
                }
            }

            this.widthAnimation.update(defaultWidth);
            this.pw = this.widthAnimation.getValue();
            this.ph = height;
            this.width = (int) this.pw;
            this.height = (int) this.ph;
        }
    }

    private String getAmplifierText(int amplifier) {
        return String.valueOf(amplifier + 1);
    }

    private String formatDuration(int durationTicks) {
        int totalSeconds = durationTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private Identifier getEffectIcon(StatusEffect effect) {
        String id = effect.getTranslationKey().replace("effect.minecraft.", "").replace("effect.", "");
        return Identifier.of("minecraft", "textures/mob_effect/" + id + ".png");
    }

    public void updatePotions() {
        if (mc.player != null) {
            Map<String, StatusEffectInstance> currentEffects = mc.player.getStatusEffects().stream().collect(Collectors.toMap((e) -> {
                String var10000 = Text.translatable(e.getTranslationKey()).getString();
                return var10000 + ":" + e.getAmplifier();
            }, (e) -> e, (e1, e2) -> e1));
            this.potionItems.forEach((item) -> {
                String key = item.name + ":" + item.amplifier;
                StatusEffectInstance effect = currentEffects.get(key);
                if (effect != null) {
                    item.durationTicks = effect.getDuration();
                    if (!item.active) {
                        item.animation.setValue(1.0F);
                    }

                    item.active = true;
                    currentEffects.remove(key);
                } else {
                    item.active = false;
                }
            });
            currentEffects.forEach((key, effect) -> {
                this.potionItems.add(new PotionItem(Text.translatable(effect.getTranslationKey()).getString(), effect.getAmplifier(), effect.getDuration(), effect));
            });
            this.potionItems.removeIf((item) -> !item.active && item.animation.getValue() == 0.0F);
        }
    }

    private static class PotionItem {
        String name;
        int amplifier;
        int durationTicks;
        boolean active;
        StatusEffectInstance effect;
        Animation animation;

        PotionItem(String name, int amplifier, int durationTicks, StatusEffectInstance effect) {
            this.animation = new Animation(250L, Easing.CUBIC_OUT);
            this.name = name;
            this.amplifier = amplifier;
            this.durationTicks = durationTicks;
            this.active = true;
            this.effect = effect;
        }
    }
}
