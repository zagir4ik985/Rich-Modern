package rich.screens.hud.port;

import org.joml.Matrix3x2fStack;
import net.minecraft.util.Identifier;
import rich.util.render.Render2D;

public class DrawUtil {

    public static void drawBlur(Matrix3x2fStack matrices, float x, float y, float width, float height, float blurStrength, BorderRadius radius, ColorRGBA tint) {
        Render2D.blur(x, y, width, height, blurStrength, radius.uniform(), tint.toArgb());
    }

    public static void drawBlurHud(Matrix3x2fStack matrices, float x, float y, float width, float height, float blurStrength, BorderRadius radius, ColorRGBA tint) {
        Render2D.blur(x, y, width, height, blurStrength, radius.uniform(), tint.toArgb());
    }

    public static void drawRoundedRect(Matrix3x2fStack matrices, float x, float y, float width, float height, BorderRadius radius, ColorRGBA color) {
        Render2D.rect(x, y, width, height, color.toArgb(), radius.uniform());
    }

    public static void drawRoundedRect(Matrix3x2fStack matrices, float x, float y, float width, float height, BorderRadius radius, ColorRGBA c1, ColorRGBA c2, ColorRGBA c3, ColorRGBA c4) {
        Render2D.gradientRect(x, y, width, height,
                new int[]{c1.toArgb(), c2.toArgb(), c3.toArgb(), c4.toArgb()},
                radius.uniform());
    }

    public static void drawRoundedBorder(Matrix3x2fStack matrices, float x, float y, float width, float height, float thickness, BorderRadius radius, ColorRGBA color) {
        Render2D.outline(x, y, width, height, thickness, color.toArgb(), radius.uniform());
    }

    public static void drawRoundedCorner(Matrix3x2fStack matrices, float x, float y, float width, float height, float thickness, float blur, ColorRGBA color, BorderRadius radius) {
        Render2D.outline(x, y, width, height, thickness, color.toArgb(), radius.uniform());
    }

    public static void drawPlayerHeadWithRoundedShader(Matrix3x2fStack matrices, Identifier texture, float x, float y, float size, BorderRadius radius, ColorRGBA color) {
        Render2D.texture(texture, x, y, size, size, 1.0f, radius.uniform(), color.toArgb());
    }
}
