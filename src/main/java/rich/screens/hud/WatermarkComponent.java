package rich.screens.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import rich.client.draggables.AbstractHudElement;
import rich.util.render.font.Fonts;
import rich.modules.impl.player.NameProtect;
import rich.modules.impl.render.Hud;
import rich.util.ColorUtil;
import rich.util.render.Render2D;

import java.awt.Color;

public class WatermarkComponent extends AbstractHudElement {

    private long startTime = System.currentTimeMillis();
    private float animatedWidth = 300;

    public WatermarkComponent() {
        super("Watermark", 10, 10, 300, 36, false);
        stopAnimation();
    }

    @Override
    public void tick() {
        if (mc.player == null) return;
        float targetWidth = calculateWidth();
        animatedWidth += (targetWidth - animatedWidth) * 0.15f;
        setWidth((int) animatedWidth);
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(rich.util.animations.Direction.BACKWARDS);
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (mc.player == null || alpha <= 0) return;
        float a = alpha / 255.0f;

        float x = getX();
        float y = getY();
        int themeColor = ColorUtil.astolfo(10000, 0, 0.7f, 0.8f, (int) (255 * a));
        int bgAlpha = (int) (64 * a);
        int radius = 4;

        String username = NameProtect.isEnabled() && mc.player != null
            ? NameProtect.getCustomName() : mc.player.getNameForScoreboard();
        String fps = mc.getCurrentFps() + " Fps";
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        PlayerListEntry list = mc.getNetworkHandler() != null
            ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) : null;
        String ping = (list != null ? list.getLatency() : 0) + " Ping";

        int playerX = (int) mc.player.getX();
        int playerY = (int) mc.player.getY();
        int playerZ = (int) mc.player.getZ();
        String coords = playerX + " " + playerY + " " + playerZ;

        double deltaX = mc.player.getX() - mc.player.lastRenderX;
        double deltaZ = mc.player.getZ() - mc.player.lastRenderZ;
        double bpsValue = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 20.0;
        String bps = String.format("%.1f", bpsValue).replace(",", ".") + " Bps";

        Render2D.blur(x, y, animatedWidth, 36, 15, radius, new Color(255, 255, 255, (int) (255 * a)).getRGB());
        Render2D.rect(x, y, animatedWidth, 36, ColorUtil.rgba(30, 30, 30, (int) (64 * a)), radius);

        float topY = y + 2;
        float bottomY = y + 18;

        long currentTime = System.currentTimeMillis();
        float blinkProgress = (float) (Math.sin((currentTime - startTime) / 275.0) * 0.8 + 0.55);
        int iconAlpha = (int) (80 + 175 * blinkProgress);
        int iconColor = ColorUtil.replAlpha(themeColor, (int) (iconAlpha * a));

        Fonts.BOLD.draw("zagaDLC", x + 5.6f, topY + 3, 7, iconColor);
        Fonts.BOLD.draw("|", x + 5.6f + Fonts.BOLD.getWidth("zagaDLC", 7) + 3.6f, topY + 2, 7, new Color(128, 128, 128, (int) (35 * a)).getRGB());

        float waveX = x + 5.6f + Fonts.BOLD.getWidth("zagaDLC", 7) + 3.6f + Fonts.BOLD.getWidth("|", 7) + 3.6f;
        for (int i = 0; i < username.length(); i++) {
            char c = username.charAt(i);
            String charStr = String.valueOf(c);
            float waveOffset = i * 0.5f;
            float charProgress = (float) Math.sin((currentTime - startTime) / 135.0 + waveOffset) * 0.3f + 0.40f;
            int charColor = ColorUtil.interpolate(themeColor, ColorUtil.astolfo(10000, 300, 0.7f, 0.8f, 255), charProgress);
            Fonts.BOLD.draw(charStr, waveX, topY + 2, 7, charColor);
            waveX += Fonts.BOLD.getWidth(charStr, 7);
        }

        float sepX = waveX + 3.375f;
        Fonts.BOLD.draw("|", sepX, topY + 2, 7, new Color(128, 128, 128, (int) (35 * a)).getRGB());
        Fonts.BOLD.draw(fps, sepX + Fonts.BOLD.getWidth("|", 7) + 3.375f, topY + 2, 7, new Color(255, 255, 255, (int) (255 * a)).getRGB());

        String[] bottomIcons = {"", "", "", ""};
        String[] bottomTexts = {coords, ping, "TPS", bps};
        float bx = x + 3;

        int fpsSepX = (int) (sepX + Fonts.BOLD.getWidth("|", 7) + 3.375f + Fonts.BOLD.getWidth(fps, 7) + 3.375f);
        Fonts.BOLD.draw("|", fpsSepX, topY + 2, 7, new Color(128, 128, 128, (int) (35 * a)).getRGB());
        Fonts.BOLD.draw(time, fpsSepX + Fonts.BOLD.getWidth("|", 7) + 3.375f, topY + 2, 7, new Color(255, 255, 255, (int) (255 * a)).getRGB());

        float infoY = bottomY + 1;
        float spacing = 4.0f;
        for (int i = 0; i < bottomTexts.length; i++) {
            String text = bottomTexts[i];
            float textWidth = Fonts.REGULAR.getWidth(text, 6.5f);
            Fonts.REGULAR.draw(text, bx, infoY, 6.5f, new Color(255, 255, 255, (int) (200 * a)).getRGB());
            bx += textWidth + spacing;
            if (i < bottomTexts.length - 1) {
                Fonts.REGULAR.draw("|", bx, infoY, 6.5f, new Color(128, 128, 128, (int) (35 * a)).getRGB());
                bx += Fonts.REGULAR.getWidth("|", 6.5f) + spacing;
            }
        }
    }

    private float calculateWidth() {
        if (mc.player == null) return 300;
        String username = NameProtect.isEnabled()
            ? NameProtect.getCustomName() : mc.player.getNameForScoreboard();
        String fps = mc.getCurrentFps() + " Fps";
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String coords = (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ();

        double deltaX = mc.player.getX() - mc.player.lastRenderX;
        double deltaZ = mc.player.getZ() - mc.player.lastRenderZ;
        String bps = String.format("%.1f", Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 20.0) + " Bps";

        PlayerListEntry list = mc.getNetworkHandler() != null
            ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) : null;
        String ping = (list != null ? list.getLatency() : 0) + " Ping";

        float topWidth = Fonts.BOLD.getWidth("zagaDLC", 7) + 3.6f + Fonts.BOLD.getWidth("|", 7) + 3.6f
            + Fonts.BOLD.getWidth(username, 7) + 3.375f + Fonts.BOLD.getWidth("|", 7) + 3.375f
            + Fonts.BOLD.getWidth(fps, 7) + 3.375f + Fonts.BOLD.getWidth("|", 7) + 3.375f + Fonts.BOLD.getWidth(time, 7);
        float bottomWidth = Fonts.REGULAR.getWidth(coords, 6.5f) + Fonts.REGULAR.getWidth(ping, 6.5f)
            + Fonts.REGULAR.getWidth("TPS", 6.5f) + Fonts.REGULAR.getWidth(bps, 6.5f) + 40;
        return Math.max(topWidth, bottomWidth) + 12;
    }
}
