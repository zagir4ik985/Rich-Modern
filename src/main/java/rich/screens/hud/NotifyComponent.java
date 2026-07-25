package rich.screens.hud;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import rich.client.draggables.AbstractHudElement;
import rich.util.render.font.Fonts;
import rich.modules.module.ModuleStructure;
import rich.modules.impl.render.Hud;
import rich.util.ColorUtil;
import rich.util.render.Render2D;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotifyComponent extends AbstractHudElement {

    private static NotifyComponent INSTANCE;
    private final List<BaseNotification> notifications = new ArrayList<>();

    public NotifyComponent() {
        super("Notifications", 400, 200, 150, 15, false);
        stopAnimation();
        INSTANCE = this;
    }

    public static NotifyComponent getInstance() {
        return INSTANCE;
    }

    public void addNotification(ModuleStructure module, boolean enabled) {
        notifications.add(new ModuleNotification(module, enabled));
    }

    public void addTextNotification(String icon, Text text) {
        notifications.add(new TextNotification(icon, text));
    }

    @Override
    public void tick() {
        Iterator<BaseNotification> it = notifications.iterator();
        while (it.hasNext()) {
            BaseNotification n = it.next();
            if (!n.fadingOut && System.currentTimeMillis() - n.timestamp > 1500) {
                n.fadingOut = true;
            }
            float target = n.fadingOut ? 0 : 1;
            n.alpha += (target - n.alpha) * 0.15f;
            if (n.fadingOut && n.alpha < 0.01f) {
                it.remove();
            }
        }
    }

    @Override
    public boolean visible() {
        return !notifications.isEmpty() || !scaleAnimation.isFinished(rich.util.animations.Direction.BACKWARDS);
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (notifications.isEmpty()) return;
        float a = alpha / 255.0f;
        int themeColor = ColorUtil.astolfo(10000, 0, 0.7f, 0.8f, 255);

        float centerX = (float) mc.getWindow().getScaledWidth() / 2.0f;
        float startY = (float) mc.getWindow().getScaledHeight() / 2.0f + 16.0f;
        float notifHeight = 14.5f;
        float gap = 1.5f;
        float y = startY;

        for (BaseNotification n : Lists.reverse(notifications)) {
            float elemAlpha = n.alpha * a;
            if (elemAlpha < 0.01f) continue;

            if (n instanceof ModuleNotification mn) {
                drawModuleNotification(context, mn, centerX, y, notifHeight, elemAlpha, themeColor);
            } else if (n instanceof TextNotification tn) {
                drawTextNotification(context, tn, centerX, y, notifHeight, elemAlpha, themeColor);
            }
            y += (notifHeight + gap) * n.alpha;
        }
    }

    private void drawModuleNotification(DrawContext ctx, ModuleNotification n, float centerX, float y, float h, float a, int themeColor) {
        String moduleName = "\"" + n.module.getName() + "\"";
        String statusText = n.enabled ? " Enabled" : " Disabled";
        float iconBgWidth = 12;

        float moduleWidth = Fonts.BOLD.getWidth(moduleName, 7.25f);
        float statusWidth = Fonts.REGULAR.getWidth(statusText, 7.25f);
        float totalWidth = iconBgWidth + 8 + moduleWidth + statusWidth;
        float x = centerX - totalWidth / 2;

        int bgAlpha = (int) (64 * a);
        Render2D.blur(x, y, totalWidth, h, 15, 2, new Color(255, 255, 255, (int) (255 * a)).getRGB());
        Render2D.rect(x, y, totalWidth, h, ColorUtil.rgba(30, 30, 30, bgAlpha), 2);

        String icon = n.enabled ? "J" : "K";
        Fonts.HUD_ICONS.draw(icon, x + (iconBgWidth - Fonts.HUD_ICONS.getWidth(icon, 11)) / 2, y + 1.5f, 11, ColorUtil.replAlpha(themeColor, (int) (255 * a)));

        float textX = x + iconBgWidth + 2.5f;
        float textY = y + (h - 7) / 2.0f;
        Fonts.BOLD.draw(moduleName, textX + 2.35f, textY, 7.25f, new Color(255, 255, 255, (int) (255 * a)).getRGB());
        Fonts.REGULAR.draw(statusText, textX + 2.35f + moduleWidth, textY, 7.25f, new Color(255, 255, 255, (int) (255 * a)).getRGB());
    }

    private void drawTextNotification(DrawContext ctx, TextNotification n, float centerX, float y, float h, float a, int themeColor) {
        float iconBgWidth = 14;
        String textStr = n.text.getString();
        float textWidth = Fonts.REGULAR.getWidth(textStr, 7.25f);
        float totalWidth = iconBgWidth + 6 + textWidth;
        float x = centerX - totalWidth / 2;

        int bgAlpha = (int) (64 * a);
        Render2D.blur(x, y, totalWidth, h, 15, 2, new Color(255, 255, 255, (int) (255 * a)).getRGB());
        Render2D.rect(x, y, totalWidth, h, ColorUtil.rgba(30, 30, 30, bgAlpha), 2);

        Fonts.ICONS.draw(n.icon, x + (iconBgWidth - Fonts.ICONS.getWidth(n.icon, 6.75f)) / 2, y + 1, 6.75f, ColorUtil.replAlpha(themeColor, (int) (255 * a)));

        float textX = x + iconBgWidth + 1.65f;
        float textY = y + (h - 7) / 2.0f;
        Fonts.REGULAR.draw(textStr, textX + 3.5f, textY, 7.25f, new Color(255, 255, 255, (int) (255 * a)).getRGB());
    }

    private static class ModuleNotification extends BaseNotification {
        final ModuleStructure module;
        final boolean enabled;
        ModuleNotification(ModuleStructure module, boolean enabled) {
            this.module = module;
            this.enabled = enabled;
        }
    }

    private static class TextNotification extends BaseNotification {
        final String icon;
        final Text text;
        TextNotification(String icon, Text text) {
            this.icon = icon;
            this.text = text;
        }
    }

    private static abstract class BaseNotification {
        long timestamp = System.currentTimeMillis();
        boolean fadingOut = false;
        float alpha = 0;
    }
}
