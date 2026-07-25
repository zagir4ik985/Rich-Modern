package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import rich.client.draggables.AbstractHudElement;
import rich.util.render.font.Fonts;
import rich.modules.impl.render.Hud;
import rich.util.ColorUtil;
import rich.util.render.Render2D;

import java.awt.Color;
import java.util.Locale;

public class InformationComponent extends AbstractHudElement {

    private float targetY = 0;
    private float currentY = 0;
    private boolean chatOpen = false;

    public InformationComponent() {
        super("Information", 10, 560, 200, 15, false);
        stopAnimation();
    }

    @Override
    public void tick() {
        if (mc.player == null) return;
        int scaledHeight = mc.getWindow().getScaledHeight();
        chatOpen = mc.currentScreen instanceof ChatScreen;
        targetY = chatOpen ? scaledHeight - 15 : scaledHeight;
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(rich.util.animations.Direction.BACKWARDS);
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (mc.player == null || alpha <= 0) return;
        float a = alpha / 255.0f;
        int themeColor = ColorUtil.astolfo(10000, 0, 0.7f, 0.8f, (int) (255 * a));

        currentY += (targetY - currentY) * 0.15f;

        int px = (int) Math.floor(mc.player.getX());
        int py = (int) Math.floor(mc.player.getY());
        int pz = (int) Math.floor(mc.player.getZ());
        double speed = Math.hypot(mc.player.getX() - mc.player.lastRenderX, mc.player.getZ() - mc.player.lastRenderZ);
        String coordsText = String.format(Locale.US, "%d %d %d", px, py, pz);
        String speedText = String.format("%.2f", speed * 20.0).replace(",", ".") + " B/s";

        float totalWidth = Fonts.REGULAR.getWidth(coordsText, 7.5f) + Fonts.REGULAR.getWidth(speedText, 7.5f) + 50;
        float bgX = 4.0f;
        float bgY = currentY - 17.0f;
        float bgW = totalWidth;
        float bgH = 14.0f;

        Render2D.blur(bgX, bgY, bgW, bgH, 11, 2, new Color(80, 80, 80, (int) (255 * a)).getRGB());
        Render2D.rect(bgX, bgY, bgW, bgH, ColorUtil.rgba(30, 30, 30, (int) (100 * a)), 2);

        float textY = currentY - 12.5f;
        Fonts.REGULAR.draw(coordsText, 18.0f, textY, 7.5f, new Color(255, 255, 255, (int) (255 * a)).getRGB());

        float dotX = 18.5f + Fonts.REGULAR.getWidth(coordsText, 7.5f) + 3.0f;
        Render2D.rect(dotX, currentY - 11.0f, 2, 2, themeColor, 1);

        float speedX = dotX + 5.5f;
        Fonts.REGULAR.draw(speedText, speedX, textY, 7.5f, new Color(255, 255, 255, (int) (255 * a)).getRGB());

        setWidth((int) totalWidth);
        setHeight((int) bgH);
    }
}
