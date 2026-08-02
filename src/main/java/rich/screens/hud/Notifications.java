package rich.screens.hud;

import com.google.common.collect.Lists;
import net.minecraft.text.Text;
import rich.screens.hud.port.Animation;
import rich.screens.hud.port.BorderRadius;
import rich.screens.hud.port.ColorRGBA;
import rich.screens.hud.port.CustomDrawContext;
import rich.screens.hud.port.DrawUtil;
import rich.screens.hud.port.Easing;
import rich.screens.hud.port.PortHudElement;
import rich.screens.hud.port.Theme;
import rich.util.render.font.Font;
import rich.util.render.font.Fonts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Notifications extends PortHudElement {

    private static final float blurStrength = 15.0f;
    private static final float cornerRadius = 2.25f;

    private static Notifications instance;

    public static Notifications getInstance() {
        return instance;
    }

    private final Animation toggleAnimation;
    private final List<BaseNotification> notifications;

    public Notifications() {
        super("Notifications", 0, 0, 110, 16, false);
        instance = this;
        this.toggleAnimation = new Animation(200L, Easing.CUBIC_OUT);
        this.notifications = new ArrayList<>();
    }

    @Override
    public boolean visible() {
        return !notifications.isEmpty();
    }

    public void addNotification(String text, long duration) {
        addTextNotification("C", Text.literal(text), duration);
    }

    public void addTextNotification(String icon, Text text) {
        addTextNotification(icon, text, 1500L);
    }

    public void addTextNotification(String icon, Text text, long duration) {
        this.notifications.add(new TextNotification(icon, text, duration));
    }

    private static void drawBlurBackground(CustomDrawContext ctx, float x, float y, float width, float height, Theme theme, float animation) {
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
        Iterator<BaseNotification> iterator = this.notifications.iterator();
        Theme theme = new Theme();
        Font textFont = Fonts.MEDIUM;
        float notificationHeight = 14.5F;
        float y = (float) mc.getWindow().getScaledHeight() / 2.0F + 16.0F;

        BaseNotification n;
        float gap = 1.5F;

        for (Iterator<BaseNotification> var11 = Lists.reverse(this.notifications).iterator(); var11.hasNext(); ) {
            n = var11.next();
            n.render(ctx, (float) mc.getWindow().getScaledWidth() / 2.0F, y, textFont, theme, notificationHeight);
            y += (notificationHeight + gap) * n.alphaAnimation.getValue();
        }

        while (iterator.hasNext()) {
            BaseNotification notification = iterator.next();
            if (!notification.fadingOut && System.currentTimeMillis() - notification.timestamp > notification.duration) {
                notification.fadingOut = true;
                notification.alphaAnimation.update(0.0F);
            }

            if (notification.fadingOut && notification.alphaAnimation.getValue() < 0.01F) {
                iterator.remove();
            } else {
                notification.alphaAnimation.update(notification.fadingOut ? 0.0F : 1.0F);
            }
        }
    }

    private static class TextNotification extends BaseNotification {
        final String icon;
        final Text text;
        final long duration;

        TextNotification(String icon, Text text, long duration) {
            this.icon = icon;
            this.text = text;
            this.duration = duration;
        }

        @Override
        void render(CustomDrawContext ctx, float x, float y, Font textFont, Theme theme, float notificationHeight) {
            if (this.timestamp == 0L) {
                this.timestamp = System.currentTimeMillis();
            }

            float iconBgWidth = 14.0F;
            Text moduleName = this.text;
            float moduleNameWidth = textFont.getWidth(moduleName.getString(), 7.25F);
            float width = iconBgWidth + 6.0F + moduleNameWidth;

            Font iconFont = Fonts.ICONS;

            x -= width / 2.0F;

            drawBlurBackground(ctx, x, y, width, notificationHeight, theme, this.alphaAnimation.getValue());

            float iconX = x + (17F - iconFont.getWidth(this.icon, 6.75F)) / 2.0F;
            float iconY = y + 1F + (notificationHeight - iconFont.getHeight(6.75F)) / 2.0F;
            ctx.drawText(iconFont, this.icon, iconX, iconY, 6.75F, theme.getColor().withAlpha(this.alphaAnimation.getValue() * 255.0F));

            float textX = x + iconBgWidth + 1.65F;
            float textY = y + (notificationHeight - textFont.getHeight(7.25F)) / 2.0F;
            ctx.drawText(textFont, moduleName.getString(), textX + 3.5F, textY, 7.25F, this.alphaAnimation.getValue() * 255.0F);
        }
    }

    private abstract static class BaseNotification {
        long timestamp;
        boolean fadingOut = false;
        final Animation alphaAnimation;
        long duration = 1500L;

        private BaseNotification() {
            this.alphaAnimation = new Animation(300L, Easing.CUBIC_OUT);
        }

        abstract void render(CustomDrawContext ctx, float x, float y, Font textFont, Theme theme, float notificationHeight);
    }
}
