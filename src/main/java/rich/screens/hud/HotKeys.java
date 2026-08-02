package rich.screens.hud;

import net.minecraft.client.gui.screen.ChatScreen;
import rich.Initialization;
import rich.modules.module.ModuleStructure;
import rich.modules.module.setting.Setting;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.screens.hud.port.Animation;
import rich.screens.hud.port.BorderRadius;
import rich.screens.hud.port.ColorRGBA;
import rich.screens.hud.port.CustomDrawContext;
import rich.screens.hud.port.DrawUtil;
import rich.screens.hud.port.Easing;
import rich.screens.hud.port.Keyboard;
import rich.screens.hud.port.PortHudElement;
import rich.screens.hud.port.Theme;
import rich.util.modules.ModuleProvider;
import rich.util.render.font.Fonts;

import java.util.Iterator;
import java.util.List;

public class HotKeys extends PortHudElement {

    private final Animation widthAnimation;
    private final Animation xLine;
    private final Animation alpha;

    private static final float BLUR_STRENGTH = 15.0f;
    private static final float CORNER_RADIUS = 2.25f;

    public HotKeys() {
        super("HotKeys", 300, 40, 80, 23, true);
        this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
        this.xLine = new Animation(170L, Easing.SINE_OUT);
        this.alpha = new Animation(200L, Easing.CUBIC_OUT);
    }

    private void drawBlurBackground(CustomDrawContext ctx, float x, float y, float width, float height, Theme theme, float animation) {
        DrawUtil.drawBlur(
                ctx.getMatrices(), x, y, width, height,
                BLUR_STRENGTH,
                BorderRadius.all(CORNER_RADIUS),
                new ColorRGBA(255, 255, 255, (int) (animation * 255))
        );

        ColorRGBA themeColor = theme.getColor();
        ColorRGBA backgroundColor = new ColorRGBA(
                (int) (Math.min(255, Math.max(0, themeColor.getRed() * 0.15f))),
                (int) (Math.min(255, Math.max(0, themeColor.getGreen() * 0.15f))),
                (int) (Math.min(255, Math.max(0, themeColor.getBlue() * 0.15f))),
                (int) (64 * animation)
        );

        DrawUtil.drawRoundedRect(
                ctx.getMatrices(), x, y, width, height,
                BorderRadius.all(CORNER_RADIUS),
                backgroundColor
        );
    }

    private List<ModuleStructure> getModules() {
        if (Initialization.getInstance() == null
                || Initialization.getInstance().getManager() == null
                || Initialization.getInstance().getManager().getModuleProvider() == null) {
            return java.util.Collections.emptyList();
        }
        return Initialization.getInstance().getManager().getModuleProvider().getModuleStructures();
    }

    private boolean isSkipped(ModuleStructure module) {
        String name = module.getName();
        return name.equals("Hud");
    }

    @Override
    public void renderPort(CustomDrawContext ctx, int alpha) {
        float posX = this.getPx();
        float posY = this.getPy();
        float defaultWidth = 53.0F;
        float height = 14.5F;
        boolean isFound = false;
        List<ModuleStructure> modules = getModules();

        for (ModuleStructure module : modules) {
            if (isSkipped(module)) {
                continue;
            }
            if (module.isState() && module.getKey() != -1) {
                this.alpha.update(1.0F);
                isFound = true;
            }

            for (Setting setting : module.settings()) {
                if (setting instanceof BooleanSetting) {
                    BooleanSetting boolSetting = (BooleanSetting) setting;
                    if (boolSetting.isValue() && boolSetting.getKey() != -1) {
                        this.alpha.update(1.0F);
                        isFound = true;
                    }
                }
            }
        }

        if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
            this.alpha.update(0.0F);
        }

        if (mc.currentScreen instanceof ChatScreen) {
            this.alpha.update(1.0F);
        }

        if (this.alpha.getValue() < 0.01F) {
            this.pw = 0.0F;
            this.ph = 0.0F;
            this.width = 0;
            this.height = 0;
            return;
        }

        Theme theme = new Theme();

        drawBlurBackground(ctx, posX, posY, this.widthAnimation.getValue(), 14.5F, theme, this.alpha.getValue());

        ctx.drawText(Fonts.NURIKI, "C", posX + 4.25F, posY + 5.5F, 10.0F, theme.getColor().withAlpha(255.0F * this.alpha.getValue()));

        ctx.drawText(Fonts.SEMIBOLD, " :", posX + 15.0F, posY + 4.75F, 7.0F, new ColorRGBA(166, 166, 166, 255.0F * this.alpha.getValue()));

        ctx.drawText(Fonts.SEMIBOLD, "Hotkeys", posX + 20.5F, posY + 4.75F, 7.5F, (new ColorRGBA(-1)).withAlpha(255.0F * this.alpha.getValue()));

        posY += 14.5F + 1.0F;
        float bindWidth = 0.0F;

        for (ModuleStructure module : modules) {
            if (isSkipped(module)) {
                continue;
            }
            if (module.getAnimation().getOutput().floatValue() != 0.0F && module.getKey() != -1) {
                float localBindWidth = Fonts.SEMIBOLD.getWidth(Keyboard.getKeyName(module.getKey()), 6.75F);
                if (localBindWidth > bindWidth) {
                    bindWidth = localBindWidth;
                }
            }

            for (Setting setting : module.settings()) {
                if (setting instanceof BooleanSetting) {
                    BooleanSetting boolSetting = (BooleanSetting) setting;
                    if (boolSetting.isValue() && boolSetting.getKey() != -1) {
                        float localBindWidth = Fonts.SEMIBOLD.getWidth(Keyboard.getKeyName(boolSetting.getKey()), 6.75F);
                        if (localBindWidth > bindWidth) {
                            bindWidth = localBindWidth;
                        }
                    }
                }
            }
        }

        this.xLine.update(bindWidth + 10.0F);

        for (ModuleStructure module : modules) {
            if (isSkipped(module)) {
                continue;
            }
            if (module.getAnimation().getOutput().floatValue() != 0.0F && module.getKey() != -1) {
                height += 11.0F + 1.0F;
                String bind = Keyboard.getKeyName(module.getKey());
                String moduleName = module.getName();
                float elementsWidth = Fonts.SEMIBOLD.getWidth(moduleName, 7.0F) + Fonts.SEMIBOLD.getWidth(bind, 6.75F) + 50.0F;

                float moduleAnim = module.getAnimation().getOutput().floatValue();
                float elementAlpha = moduleAnim * this.alpha.getValue();
                float elementY = posY + moduleAnim * 3.0F - 3.0F;

                drawBlurBackground(ctx, posX, elementY, this.widthAnimation.getValue(), 11.0F, theme, elementAlpha);

                float separatorX = posX + this.widthAnimation.getValue() - 6.0F - this.xLine.getValue();
                ctx.drawText(Fonts.SEMIBOLD, ":", separatorX, elementY + 3.25F, 6.5F, new ColorRGBA(166, 166, 166, 255.0F * elementAlpha));

                ctx.drawText(Fonts.SEMIBOLD, moduleName, posX + 5.0F, elementY + 3.25F, 7.0F, (new ColorRGBA(-1)).withAlpha(elementAlpha * 255.0F));

                ctx.drawText(Fonts.SEMIBOLD, bind, posX + this.widthAnimation.getValue() - 3.0F - this.xLine.getValue() / 2.0F - Fonts.SEMIBOLD.getWidth(bind, 6.75F) / 2.0F, elementY + 3.25F, 6.5F, (new ColorRGBA(-1)).withAlpha(elementAlpha * 255.0F));

                if (elementsWidth > defaultWidth) {
                    defaultWidth = elementsWidth;
                }

                posY += (11.0F + 1.0F) * moduleAnim;
            }

            for (Setting setting : module.settings()) {
                if (setting instanceof BooleanSetting) {
                    BooleanSetting boolSetting = (BooleanSetting) setting;
                    if (boolSetting.isValue() && boolSetting.getKey() != -1) {
                        height += 11.0F + 1.0F;
                        String bind = Keyboard.getKeyName(boolSetting.getKey());
                        String settingName = boolSetting.getName();
                        float elementsWidth = Fonts.SEMIBOLD.getWidth(settingName, 7.0F) + Fonts.SEMIBOLD.getWidth(bind, 6.75F) + 50.0F;

                        float elementAlpha = this.alpha.getValue();
                        float elementY = posY - 3.0F;

                        drawBlurBackground(ctx, posX, elementY, this.widthAnimation.getValue(), 11.0F, theme, elementAlpha);

                        float separatorX = posX + this.widthAnimation.getValue() - 6.0F - this.xLine.getValue();
                        ctx.drawText(Fonts.SEMIBOLD, ":", separatorX, elementY + 3.25F, 6.5F, new ColorRGBA(166, 166, 166, 255.0F * elementAlpha));

                        ctx.drawText(Fonts.SEMIBOLD, settingName, posX + 5.0F, elementY + 3.25F, 7.0F, (new ColorRGBA(-1)).withAlpha(elementAlpha * 255.0F));

                        ctx.drawText(Fonts.SEMIBOLD, bind, posX + this.widthAnimation.getValue() - 3.0F - this.xLine.getValue() / 2.0F - Fonts.SEMIBOLD.getWidth(bind, 6.75F) / 2.0F, elementY + 3.25F, 6.5F, (new ColorRGBA(-1)).withAlpha(elementAlpha * 255.0F));

                        if (elementsWidth > defaultWidth) {
                            defaultWidth = elementsWidth;
                        }

                        posY += (11.0F + 1.0F);
                    }
                }
            }
        }

        this.widthAnimation.update(defaultWidth);
        this.pw = this.widthAnimation.getValue();
        this.ph = height;
        this.width = (int) this.pw;
        this.height = (int) this.ph;
    }
}
