package rich.screens.hud;

import net.minecraft.client.network.PlayerListEntry;
import rich.modules.impl.player.NameProtect;
import rich.screens.hud.port.ColorRGBA;
import rich.screens.hud.port.ColorUtil;
import rich.screens.hud.port.CustomDrawContext;
import rich.screens.hud.port.DrawUtil;
import rich.screens.hud.port.PortHudElement;
import rich.screens.hud.port.BorderRadius;
import rich.screens.hud.port.Theme;
import rich.util.Instance;
import rich.util.render.font.Font;
import rich.util.render.font.Fonts;
import rich.util.tps.TPSCalculate;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Watermark extends PortHudElement {

    private final ColorRGBA textColor = new ColorRGBA(255, 255, 255, 255);
    private final ColorRGBA iconColor = new ColorRGBA(255, 255, 255, 255);
    private final float elementSpacing = 3.375f;
    private final float padding = 5.625f;
    private final float cornerRadius = 2.4f;

    private final float blurStrength = 15.0f;
    private final float separatorSize = 8.0f;
    private final float separatorYOffset = -0.55f;
    private final ColorRGBA separatorColor = new ColorRGBA(128, 128, 128, 35);

    private long startTime = System.currentTimeMillis();

    public Watermark() {
        super("Watermark", 10, 10, 300, 36, false);
        startAnimation();
    }

    private void drawBlurBackground(CustomDrawContext ctx, float x, float y, float width, float height, Theme theme) {
        DrawUtil.drawBlur(
                ctx.getMatrices(), x, y, width, height,
                blurStrength,
                BorderRadius.all(cornerRadius),
                new ColorRGBA(255, 255, 255, 255)
        );

        ColorRGBA themeColor = theme.getColor();

        ColorRGBA backgroundColor = new ColorRGBA(
                (int) (Math.min(255, Math.max(0, themeColor.getRed() * 0.15f))),
                (int) (Math.min(255, Math.max(0, themeColor.getGreen() * 0.15f))),
                (int) (Math.min(255, Math.max(0, themeColor.getBlue() * 0.15f))),
                64
        );

        DrawUtil.drawRoundedRect(
                ctx.getMatrices(), x, y, width, height,
                BorderRadius.all(cornerRadius),
                backgroundColor
        );
    }

    @Override
    public void renderPort(CustomDrawContext ctx, int alpha) {
        float x = this.getPx();
        float y = this.getPy();
        Theme theme = new Theme();

        NameProtect nameProtect = Instance.get(NameProtect.class);
        String username = nameProtect != null && nameProtect.isState() ? nameProtect.getCustomName() : mc.player.getNameForScoreboard();
        String fps = mc.getCurrentFps() + " Fps";
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        PlayerListEntry list = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        String ping = (list != null ? list.getLatency() : 0) + " Ping";

        float tpsValue = 20.0f;
        if (TPSCalculate.getInstance() != null) {
            tpsValue = TPSCalculate.getInstance().getTpsRounded();
        }
        String tps = String.format("%.1f", tpsValue).replace(",", ".") + " Ticks";

        int playerX = (int) mc.player.getX();
        int playerY = (int) mc.player.getY();
        int playerZ = (int) mc.player.getZ();
        String coords = playerX + " " + playerY + " " + playerZ;

        double deltaX = mc.player.getX() - mc.player.lastX;
        double deltaZ = mc.player.getZ() - mc.player.lastZ;
        double bpsValue = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 20.0;
        String bps = String.format("%.1f", bpsValue).replace(",", ".") + " Bps";

        float currentX = x;

        currentX += drawWatermarkElementWithCustomIconLarge(ctx, currentX, y, "P", "Release", Fonts.NURIKI, theme);

        currentX += drawCombinedElementWithCustomIcons(ctx, currentX, y,
                new String[]{"W", "X", "V"},
                new String[]{username, fps, time},
                new Font[]{Fonts.NURIKI, Fonts.NURIKI, Fonts.NURIKI}, theme);

        currentX = x;
        float bottomY = y + 18f;

        currentX += drawSmallIconElement(ctx, currentX, bottomY, "U", Fonts.NURIKI, theme);

        currentX += drawWatermarkElementWithCustomIcon(ctx, currentX, bottomY, "F", coords, Fonts.NURIKI, theme, false, 9.6f, 2.8f);

        currentX += drawWatermarkElementWithCustomIcon(ctx, currentX, bottomY, "Q", ping, Fonts.NURIKI, theme, false, 8.19f, 2.6f);

        currentX += drawWatermarkElementWithCustomIcon(ctx, currentX, bottomY, "$", tps, Fonts.NURIKI, theme, false, 8.19f, 2.6f);

        drawWatermarkElementWithCustomIcon(ctx, currentX, bottomY, "@", bps, Fonts.NURIKI, theme, true, 8.19f, 2.07f);

        this.pw = 300.0f;
        this.ph = 36.0f;
        this.width = 300;
        this.height = 36;
    }

    private float drawSmallIconElement(CustomDrawContext ctx, float x, float y, String icon, Font customIconFont, Theme theme) {
        float iconSize = 9f;
        float iconWidth = customIconFont.getWidth(icon, iconSize);
        float elementWidth = padding * 1.15F + iconWidth;
        float elementHeight = 15.21f;

        drawBlurBackground(ctx, x, y, elementWidth, elementHeight, theme);

        float iconX = x + 3.55F;
        float iconCenterY = y + (elementHeight - iconSize) / 2.0f + 2.5f;

        long currentTime = System.currentTimeMillis();
        float blinkProgress = (float) (Math.sin((currentTime - startTime) / 450.0) * 0.7 + 0.5);
        int alphaValue = (int) (80 + 175 * blinkProgress);

        ColorRGBA blinkingIconColor = new ColorRGBA(
                theme.getColor().getRed(),
                theme.getColor().getGreen(),
                theme.getColor().getBlue(),
                alphaValue
        );

        ctx.drawText(customIconFont, icon, iconX, iconCenterY, iconSize, blinkingIconColor);

        return elementWidth + elementSpacing;
    }

    private float drawWatermarkElementWithCustomIcon(CustomDrawContext ctx, float x, float y, String icon, String text, Font customIconFont, Theme theme, boolean lowerText, float iconSize, float iconYOffset) {
        Font textFont = Fonts.SEMIBOLD;

        float iconWidth = customIconFont.getWidth(icon, iconSize);
        float separatorWidth = textFont.getWidth(":", separatorSize);
        float textWidth = textFont.getWidth(text, 7.0f);
        float elementWidth = padding * 2 + iconWidth + 3.6f + separatorWidth + 3.6f + textWidth;
        float elementHeight = 15.21f;

        drawBlurBackground(ctx, x, y, elementWidth, elementHeight, theme);

        float iconX = x + padding;
        float separatorX = iconX + iconWidth + 3.6f;
        float textX = separatorX + separatorWidth + 3.6f;

        float iconCenterY = y + (elementHeight - iconSize) / 2.0f + iconYOffset;
        float textCenterY = y + (elementHeight - 6.39f) / 2.0f + 0.6f;

        if (lowerText) {
            textCenterY += 0.0f;
        }

        ColorRGBA themeIconColor = theme.getColor();

        ctx.drawText(customIconFont, icon, iconX, iconCenterY, iconSize, themeIconColor);
        ctx.drawText(textFont, ":", separatorX, textCenterY + separatorYOffset, separatorSize, separatorColor);
        ctx.drawText(textFont, text, textX, textCenterY, 7.0f, textColor);

        return elementWidth + elementSpacing;
    }

    private float drawCombinedElementWithCustomIcons(CustomDrawContext ctx, float x, float y, String[] icons, String[] texts, Font[] iconFonts, Theme theme) {
        Font textFont = Fonts.SEMIBOLD;

        float totalWidth = padding * 2;
        for (int i = 0; i < texts.length; i++) {
            totalWidth += iconFonts[i].getWidth(icons[i], 8.19f) + 4.0f + textFont.getWidth(texts[i], 7.0f);
            if (i < texts.length - 1) {
                totalWidth += 6.75f;
            }
        }

        float elementHeight = 15.21f;

        drawBlurBackground(ctx, x, y, totalWidth, elementHeight, theme);

        float currentX = x + padding;
        float textCenterY = y + (elementHeight - 6.39f) / 2.0f + 0.6f;

        ColorRGBA themeIconColor = theme.getColor();

        for (int i = 0; i < texts.length; i++) {
            float iconCenterY;
            if (i == 1) {
                iconCenterY = y + (elementHeight - 6.85f) / 2.0f + 1.8f;
            } else if (i == 2) {
                iconCenterY = y + (elementHeight - 7.75f) / 2.0f + 2.6f;
            } else {
                iconCenterY = y + (elementHeight - 7.75f) / 2.0f + 1.9f;
            }

            ctx.drawText(iconFonts[i], icons[i], currentX, iconCenterY, 8.19f, themeIconColor);
            currentX += iconFonts[i].getWidth(icons[i], 8.19f) + 4.0f;

            ctx.drawText(textFont, texts[i], currentX, textCenterY, 7.0f, textColor);
            currentX += textFont.getWidth(texts[i], 7.0f);

            if (i < texts.length - 1) {
                currentX += 3.375f;
                ctx.drawText(textFont, ":", currentX, textCenterY + separatorYOffset, separatorSize, separatorColor);
                currentX += 3.375f;
            }
        }

        return totalWidth + elementSpacing;
    }

    private float drawWatermarkElementWithCustomIconLarge(CustomDrawContext ctx, float x, float y, String icon, String text, Font customIconFont, Theme theme) {
        Font textFont = Fonts.SEMIBOLD;

        float iconWidth = customIconFont.getWidth(icon, 9.6f);
        float separatorWidth = textFont.getWidth(":", separatorSize);
        float textWidth = textFont.getWidth(text, 7.0f);
        float elementWidth = padding * 2 + iconWidth + 3.6f + separatorWidth + 3.6f + textWidth;
        float elementHeight = 15.21f;

        drawBlurBackground(ctx, x, y, elementWidth, elementHeight, theme);

        float iconX = x + padding;
        float separatorX = iconX + iconWidth + 3.6f;
        float textX = separatorX + separatorWidth + 3.6f;
        float iconCenterY = y + (elementHeight - 9.6f) / 2.0f + 2.5f;
        float textCenterY = y + (elementHeight - 6.39f) / 2.0f + 0.1f;

        long currentTime = System.currentTimeMillis();

        float blinkProgress = (float) (Math.sin((currentTime - startTime) / 275.0) * 0.8 + 0.55);
        int alphaValue = (int) (80 + 175 * blinkProgress);

        ColorRGBA blinkingIconColor = new ColorRGBA(
                theme.getColor().getRed(),
                theme.getColor().getGreen(),
                theme.getColor().getBlue(),
                alphaValue
        );

        ctx.drawText(customIconFont, icon, iconX, iconCenterY, 9.7f, blinkingIconColor);
        ctx.drawText(textFont, ":", separatorX, textCenterY + separatorYOffset, separatorSize, separatorColor);

        float charX = textX;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String charStr = String.valueOf(c);

            float waveOffset = i * 0.5f;
            float charProgress = (float) Math.sin((currentTime - startTime) / 135.0 + waveOffset) * 0.3f + 0.40f;
            ColorRGBA charColor = ColorUtil.interpolate(theme.getColor(), theme.getSecondColor(), charProgress);

            ctx.drawText(textFont, charStr, charX, textCenterY, 7.0f, charColor);
            charX += textFont.getWidth(charStr, 7.0f);
        }

        return elementWidth + elementSpacing;
    }
}
