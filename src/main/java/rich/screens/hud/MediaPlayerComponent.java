package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import rich.client.draggables.AbstractHudElement;
import rich.util.render.font.Fonts;
import rich.modules.impl.render.Hud;
import rich.util.ColorUtil;
import rich.util.render.Render2D;

import java.awt.Color;

public class MediaPlayerComponent extends AbstractHudElement {

    private float alphaAnim = 0;
    private boolean hasMedia = false;

    public MediaPlayerComponent() {
        super("MediaPlayer", 10, 300, 100, 45, true);
        stopAnimation();
    }

    @Override
    public void tick() {
        float target = hasMedia ? 1 : 0;
        alphaAnim += (target - alphaAnim) * 0.1f;
    }

    @Override
    public boolean visible() {
        return alphaAnim > 0.01f || !scaleAnimation.isFinished(rich.util.animations.Direction.BACKWARDS);
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alphaAnim < 0.01f) return;
        float a = (alpha / 255.0f) * alphaAnim;
        int themeColor = ColorUtil.astolfo(10000, 0, 0.7f, 0.8f, (int) (255 * a));

        float x = getX();
        float y = getY();
        float width = 100;
        float height = 45;

        Render2D.blur(x, y, width, height, 0, 3, new Color(0, 0, 0, (int) (255 * a)).getRGB());
        Render2D.rect(x, y, width, height, ColorUtil.rgba(20, 20, 20, (int) (19 * a)), 3);
        Render2D.outline(x, y, width, height, 0.5f, ColorUtil.rgba(0, 0, 0, (int) (100 * a)), 3);

        Fonts.BOLD.draw("No media", x + 28.5f, y + 6, 7.5f, new Color(255, 255, 255, (int) (255 * a)).getRGB());

        setWidth((int) width);
        setHeight((int) height);
    }
}
