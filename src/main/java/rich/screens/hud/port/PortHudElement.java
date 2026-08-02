package rich.screens.hud.port;

import net.minecraft.client.gui.DrawContext;
import rich.client.draggables.AbstractHudElement;

public abstract class PortHudElement extends AbstractHudElement {

    protected float px, py, pw, ph;

    public PortHudElement(String name, int x, int y, int width, int height, boolean draggable) {
        super(name, x, y, width, height, draggable);
        this.px = x;
        this.py = y;
        this.pw = width;
        this.ph = height;
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        renderPort(new CustomDrawContext(context), alpha);
    }

    public abstract void renderPort(CustomDrawContext ctx, int alpha);

    public float getPx() {
        return px;
    }

    public float getPy() {
        return py;
    }

    public float getPw() {
        return pw;
    }

    public float getPh() {
        return ph;
    }

    @Override
    public int getX() {
        return (int) px;
    }

    @Override
    public int getY() {
        return (int) py;
    }

    @Override
    public void setX(int x) {
        this.x = x;
        this.px = x;
    }

    @Override
    public void setY(int y) {
        this.y = y;
        this.py = y;
    }

    @Override
    public int getWidth() {
        return (int) pw;
    }

    @Override
    public int getHeight() {
        return (int) ph;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
        this.pw = width;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
        this.ph = height;
    }
}
