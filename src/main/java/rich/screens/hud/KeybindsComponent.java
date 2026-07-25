package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import rich.Initialization;
import rich.client.draggables.AbstractHudElement;
import rich.util.render.font.Fonts;
import rich.modules.module.ModuleStructure;
import rich.modules.impl.render.Hud;
import rich.modules.module.setting.Setting;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.util.ColorUtil;
import rich.util.render.Render2D;

import java.awt.Color;

public class KeybindsComponent extends AbstractHudElement {

    private float animatedWidth = 80;
    private float animatedXLine = 0;
    private float alphaAnim = 0;

    public KeybindsComponent() {
        super("Keybinds", 200, 100, 80, 23, true);
        stopAnimation();
    }

    @Override
    public void tick() {
        boolean found = false;
        for (ModuleStructure module : Initialization.getInstance().getManager().getModuleRepository().modules()) {
            if (module.getName().equals("Menu") || module.getName().equals("Hud")) continue;
            if (module.isState() && module.getKey() != -1) {
                found = true;
            }
            for (Setting setting : module.settings()) {
                if (setting instanceof BooleanSetting bs) {
                    if (bs.isValue() && bs.getKey() != -1) {
                        found = true;
                    }
                }
            }
        }
        float targetAlpha = (found || (mc.currentScreen instanceof ChatScreen)) ? 1.0f : 0.0f;
        alphaAnim += (targetAlpha - alphaAnim) * 0.1f;
        if (alphaAnim < 0.01f && targetAlpha == 0) {
            alphaAnim = 0;
        }
    }

    @Override
    public boolean visible() {
        return alphaAnim > 0.01f || !scaleAnimation.isFinished(rich.util.animations.Direction.BACKWARDS);
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (mc.player == null || alphaAnim < 0.01f) return;
        float a = (alpha / 255.0f) * alphaAnim;
        int themeColor = ColorUtil.astolfo(10000, 0, 0.7f, 0.8f, (int) (255 * a));
        int bgAlpha = (int) (64 * a);

        float x = getX();
        float y = getY();
        float defaultWidth = 53;
        float height = 14.5f;

        Render2D.blur(x, y, animatedWidth, 14.5f, 15, 2, new Color(255, 255, 255, (int) (255 * a)).getRGB());
        Render2D.rect(x, y, animatedWidth, 14.5f, ColorUtil.rgba(30, 30, 30, bgAlpha), 2);

        Fonts.HUD_ICONS.draw("C", x + 4.25f, y + 2, 10, themeColor);
        Fonts.BOLD.draw("|", x + 15, y + 2, 7, new Color(166, 166, 166, (int) (255 * a)).getRGB());
        Fonts.BOLD.draw("Hotkeys", x + 20.5f, y + 2, 7.5f, new Color(255, 255, 255, (int) (255 * a)).getRGB());

        y += 15.5f;
        float maxBindWidth = 0;
        for (ModuleStructure module : Initialization.getInstance().getManager().getModuleRepository().modules()) {
            if (module.getName().equals("Menu") || module.getName().equals("Hud")) continue;
            if (module.isState() && module.getKey() != -1) {
                String bind = InputUtil.fromKeyCode(new KeyInput(module.getKey(), 0, 0)).getLocalizedText().getString();
                float bindWidth = Fonts.REGULAR.getWidth(bind, 6.75f);
                if (bindWidth > maxBindWidth) maxBindWidth = bindWidth;
            }
            for (Setting setting : module.settings()) {
                if (setting instanceof BooleanSetting bs) {
                    if (bs.isValue() && bs.getKey() != -1) {
                        String bind = InputUtil.fromKeyCode(new KeyInput(bs.getKey(), 0, 0)).getLocalizedText().getString();
                        float bindWidth = Fonts.REGULAR.getWidth(bind, 6.75f);
                        if (bindWidth > maxBindWidth) maxBindWidth = bindWidth;
                    }
                }
            }
        }
        animatedXLine += (maxBindWidth + 10 - animatedXLine) * 0.1f;

        for (ModuleStructure module : Initialization.getInstance().getManager().getModuleRepository().modules()) {
            if (module.getName().equals("Menu") || module.getName().equals("Hud")) continue;
            if (module.isState() && module.getKey() != -1) {
                float animOut = module.getAnimation().getOutput().floatValue();
                if (animOut < 0.01f) continue;
                String bind = InputUtil.fromKeyCode(new KeyInput(module.getKey(), 0, 0)).getLocalizedText().getString();
                String moduleName = module.getName();
                float elemAlpha = animOut * a;
                float elemY = y + animOut * 3 - 3;

                height += 12 * animOut;
                Render2D.blur(x, elemY, animatedWidth, 11, 15, 2, new Color(255, 255, 255, (int) (255 * elemAlpha)).getRGB());
                Render2D.rect(x, elemY, animatedWidth, 11, ColorUtil.rgba(30, 30, 30, (int) (64 * elemAlpha)), 2);

                float sepX = x + animatedWidth - 6 - animatedXLine;
                Fonts.REGULAR.draw("|", sepX, elemY + 3, 6.5f, new Color(166, 166, 166, (int) (255 * elemAlpha)).getRGB());
                Fonts.BOLD.draw(moduleName, x + 5, elemY + 3, 7, new Color(255, 255, 255, (int) (255 * elemAlpha)).getRGB());
                Fonts.REGULAR.draw(bind, x + animatedWidth - 3 - animatedXLine / 2 - Fonts.REGULAR.getWidth(bind, 6.75f) / 2, elemY + 3, 6.5f, new Color(255, 255, 255, (int) (255 * elemAlpha)).getRGB());

                float elemWidth = Fonts.BOLD.getWidth(moduleName, 7) + Fonts.REGULAR.getWidth(bind, 6.75f) + 50;
                if (elemWidth > defaultWidth) defaultWidth = elemWidth;
                y += 12 * animOut;
            }
        }

        animatedWidth += (defaultWidth - animatedWidth) * 0.1f;
        setWidth((int) animatedWidth);
        setHeight((int) height);
    }
}
