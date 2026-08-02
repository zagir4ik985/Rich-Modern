package rich.screens.hud.port;

public class ColorRGBA {
    private float r, g, b, a;

    public ColorRGBA(int r, int g, int b, int a) {
        this(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
    }

    public ColorRGBA(int r, int g, int b, float a) {
        this(r / 255.0f, g / 255.0f, b / 255.0f, Math.max(0.0f, Math.min(1.0f, a / 255.0f)));
    }

    public ColorRGBA(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = Math.max(0.0f, Math.min(1.0f, a));
    }

    public ColorRGBA(int packed) {
        this.r = ((packed >> 16) & 0xFF) / 255.0f;
        this.g = ((packed >> 8) & 0xFF) / 255.0f;
        this.b = (packed & 0xFF) / 255.0f;
        this.a = ((packed >> 24) & 0xFF) / 255.0f;
    }

    public static final ColorRGBA WHITE = new ColorRGBA(255, 255, 255, 255);
    public static final ColorRGBA GREEN = new ColorRGBA(32, 255, 32, 255);
    public static final ColorRGBA TRANSPARENT = new ColorRGBA(0, 0, 0, 0);

    public float getRed() {
        return r * 255.0f;
    }

    public float getGreen() {
        return g * 255.0f;
    }

    public float getBlue() {
        return b * 255.0f;
    }

    public float getAlpha() {
        return a * 255.0f;
    }

    public ColorRGBA withAlpha(float alpha) {
        return new ColorRGBA(r * 255.0f, g * 255.0f, b * 255.0f, Math.max(0.0f, Math.min(255.0f, alpha)));
    }

    public ColorRGBA darker(float factor) {
        return new ColorRGBA(r * 255.0f * (1.0f - factor), g * 255.0f * (1.0f - factor), b * 255.0f * (1.0f - factor), a * 255.0f);
    }

    public ColorRGBA mix(ColorRGBA other, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        return new ColorRGBA(
                r * 255.0f + (other.r - r) * t * 255.0f,
                g * 255.0f + (other.g - g) * t * 255.0f,
                b * 255.0f + (other.b - b) * t * 255.0f,
                a * 255.0f + (other.a - a) * t * 255.0f
        );
    }

    public int toArgb() {
        int R = clamp((int) (r * 255.0f));
        int G = clamp((int) (g * 255.0f));
        int B = clamp((int) (b * 255.0f));
        int A = clamp((int) (a * 255.0f));
        return (A << 24) | (R << 16) | (G << 8) | B;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
