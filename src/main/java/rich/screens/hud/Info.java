package rich.screens.hud;

import net.minecraft.client.gui.screen.ChatScreen;
import rich.screens.hud.port.Animation;
import rich.screens.hud.port.BorderRadius;
import rich.screens.hud.port.ColorRGBA;
import rich.screens.hud.port.CustomDrawContext;
import rich.screens.hud.port.DrawUtil;
import rich.screens.hud.port.Easing;
import rich.screens.hud.port.PortHudElement;
import rich.screens.hud.port.Theme;
import rich.util.render.font.Fonts;

import java.util.Locale;

public class Info extends PortHudElement {

    private final Animation yAnimation;

    public Info() {
        super("Info", 4, 30, 100, 14, false);
        this.yAnimation = new Animation(200L, Easing.CUBIC_OUT);
    }

    @Override
    public void renderPort(CustomDrawContext ctx, int alpha) {
        Theme theme = new Theme();
        if (mc.currentScreen instanceof ChatScreen) {
            this.yAnimation.update((float) (mc.getWindow().getScaledHeight() - 15));
        } else {
            this.yAnimation.update((float) mc.getWindow().getScaledHeight());
        }

        int px = (int) Math.floor(mc.player.getX());
        int py = (int) Math.floor(mc.player.getY());
        int pz = (int) Math.floor(mc.player.getZ());
        double speed = Math.hypot(mc.player.getX() - mc.player.lastX, mc.player.getZ() - mc.player.lastZ);
        String coordsText = String.format(Locale.US, "%d %d %d", px, py, pz);
        String speedText = String.format("%.2f", speed * 20.0D).replace(",", ".");

        float value = this.yAnimation.getValue();

        DrawUtil.drawBlur(ctx.getMatrices(), 4.0F, value - 17.0F,
                Fonts.MEDIUM.getWidth(coordsText, 7.75F) + Fonts.MEDIUM.getWidth(speedText, 7.75F) + 49.5F,
                14.0F, 11.0F, BorderRadius.all(2.0F), new ColorRGBA(80, 80, 80, 255));

        ctx.drawText(Fonts.ICONSTYPETHO, "n", 7.75F, value - 12.5F, 7.5F, theme.getColor());
        ctx.drawText(Fonts.MEDIUM, coordsText, 18.0F, value - 12.5F, 7.5F, ColorRGBA.WHITE);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), 18.5F + Fonts.MEDIUM.getWidth(coordsText, 7.75F) + 3.0F, value - 11.0F, 2.0F, 2.0F, BorderRadius.all(0.5F), theme.getColor());
        ctx.drawText(Fonts.ICONSTYPETHO, "l", 24.0F + Fonts.MEDIUM.getWidth(coordsText, 7.75F) + 3.5F, value - 12.5F, 7.5F, theme.getColor());
        ctx.drawText(Fonts.MEDIUM, speedText + " b/s", 24.0F + Fonts.MEDIUM.getWidth(coordsText, 7.75F) + 12.0F, value - 12.5F, 7.5F, ColorRGBA.WHITE);
    }
}
