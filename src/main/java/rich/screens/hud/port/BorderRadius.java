package rich.screens.hud.port;

public class BorderRadius {
    public final float tl, tr, bl, br;

    public static final BorderRadius ZERO = new BorderRadius(0, 0, 0, 0);

    public BorderRadius(float tl, float tr, float bl, float br) {
        this.tl = tl;
        this.tr = tr;
        this.bl = bl;
        this.br = br;
    }

    public static BorderRadius all(float radius) {
        return new BorderRadius(radius, radius, radius, radius);
    }

    public static BorderRadius left(float topLeft, float bottomLeft) {
        return new BorderRadius(topLeft, 0, bottomLeft, 0);
    }

    public static BorderRadius right(float topRight, float bottomRight) {
        return new BorderRadius(0, topRight, 0, bottomRight);
    }

    public static BorderRadius top(float topLeft, float topRight) {
        return new BorderRadius(topLeft, topRight, 0, 0);
    }

    public static BorderRadius bottom(float bottomLeft, float bottomRight) {
        return new BorderRadius(0, 0, bottomLeft, bottomRight);
    }

    public float uniform() {
        return Math.max(tl, Math.max(tr, Math.max(bl, br)));
    }
}
