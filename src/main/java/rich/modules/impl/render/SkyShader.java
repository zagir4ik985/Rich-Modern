package rich.modules.impl.render;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import rich.events.api.EventHandler;
import rich.events.impl.WorldRenderEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.render.Render3D;

import java.awt.*;

public class SkyShader extends ModuleStructure {

    private final SelectSetting mode = new SelectSetting("Режим", "Тип эффекта")
            .value("Облако", "Каустка")
            .selected("Облако");

    private final SliderSettings speed = new SliderSettings("Скорость", "Скорость анимации")
            .range(0.1f, 5.0f)
            .setValue(1.0f);

    private final SliderSettings scale = new SliderSettings("Масштаб", "Масштаб эффекта")
            .range(1.0f, 20.0f)
            .setValue(5.0f);

    private final SliderSettings intensity = new SliderSettings("Интенсивность", "Интенсивность эффекта")
            .range(0.001f, 0.05f)
            .setValue(0.01f);

    private final SliderSettings alpha = new SliderSettings("Прозрачность", "Прозрачность эффекта")
            .range(0.3f, 1.0f)
            .setValue(1.0f);

    private final ColorSetting color = new ColorSetting("Цвет", "Цвет эффекта")
            .setColor(new Color(0, 120, 255, 255).getRGB());

    private final BooleanSetting cancelClouds = new BooleanSetting("Отключить облака", "Отключить облака")
            .setValue(false);

    private long startMillis = -1;

    public SkyShader() {
        super("SkyShader", "Визуальные эффекты неба", ModuleCategory.RENDER);
        settings(mode, speed, scale, intensity, alpha, color, cancelClouds);
    }

    @Override
    public void activate() {
        startMillis = -1;
    }

    @Override
    public void deactivate() {
        startMillis = -1;
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null) return;
        renderEffect();
    }

    private void renderEffect() {
        if (!isState()) return;
        if (startMillis < 0) startMillis = System.currentTimeMillis();

        float time = (System.currentTimeMillis() - startMillis) / 1000.0F;
        int colorInt = color.getColor();
        int a = (int) (alpha.getValue() * 255);
        int fillColor = (a << 24) | (colorInt & 0x00FFFFFF);

        Vec3d playerPos = mc.player.getEntityPos();
        double radius = scale.getValue() * 10;
        int segments = 12;
        double angleStep = Math.PI * 2 / segments;

        for (int i = 0; i < segments; i++) {
            double angle1 = angleStep * i + time * speed.getValue();
            double angle2 = angleStep * (i + 1) + time * speed.getValue();

            double x1 = playerPos.x + Math.cos(angle1) * radius;
            double z1 = playerPos.z + Math.sin(angle1) * radius;
            double x2 = playerPos.x + Math.cos(angle2) * radius;
            double z2 = playerPos.z + Math.sin(angle2) * radius;

            double height = playerPos.y + 60 + Math.sin(time * speed.getValue() + i) * 5;

            Vec3d p1 = new Vec3d(x1, height, z1);
            Vec3d p2 = new Vec3d(x2, height, z2);
            Vec3d p3 = new Vec3d(x2, height - 2, z2);
            Vec3d p4 = new Vec3d(x1, height - 2, z1);

            Render3D.drawQuad(p1, p2, p3, p4, fillColor, true);
        }
    }

    public boolean shouldCancelSky() {
        return isState();
    }

    public boolean shouldCancelClouds() {
        return isState() && cancelClouds.isValue();
    }
}
