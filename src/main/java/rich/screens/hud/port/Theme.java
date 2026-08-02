package rich.screens.hud.port;

import rich.modules.impl.render.Hud;

import java.awt.*;

public class Theme {

    public static ColorRGBA getColor() {
        return fromArgb(getAccentArgb());
    }

    public static ColorRGBA getSecondColor() {
        int accent = getAccentArgb();
        int r = (accent >> 16) & 0xFF;
        int g = (accent >> 8) & 0xFF;
        int b = accent & 0xFF;
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hue = (hsb[0] + 0.09f) % 1.0f;
        int shifted = Color.HSBtoRGB(hue, Math.min(1.0f, hsb[1] + 0.15f), Math.min(1.0f, hsb[2] * 0.85f));
        return fromArgb(shifted);
    }

    public static ColorRGBA getForegroundColor() {
        return new ColorRGBA(25, 25, 25, 180);
    }

    public static ColorRGBA getForegroundLight() {
        return new ColorRGBA(42, 42, 42, 180);
    }

    public static ColorRGBA getForegroundStroke() {
        return new ColorRGBA(255, 255, 255, 25);
    }

    public static ColorRGBA getGray() {
        return new ColorRGBA(128, 128, 128, 255);
    }

    public static ColorRGBA getWhite() {
        return ColorRGBA.WHITE;
    }

    private static int getAccentArgb() {
        Hud hud = Hud.getInstance();
        if (hud != null && hud.color != null) {
            return hud.color.getColorNoAlpha();
        }
        return 0xFF5FA5FF;
    }

    public static ColorRGBA fromArgb(int argb) {
        return new ColorRGBA(argb);
    }
}
