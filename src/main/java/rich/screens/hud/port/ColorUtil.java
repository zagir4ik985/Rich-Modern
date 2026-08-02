package rich.screens.hud.port;

public class ColorUtil {

    public static ColorRGBA interpolate(ColorRGBA from, ColorRGBA to, float progress) {
        return from.mix(to, progress);
    }
}
