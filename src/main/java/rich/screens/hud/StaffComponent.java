package rich.screens.hud;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import rich.client.draggables.AbstractHudElement;
import rich.util.render.font.Fonts;
import rich.modules.impl.render.Hud;
import rich.util.ColorUtil;
import rich.util.render.Render2D;

import java.awt.Color;
import java.util.*;

public class StaffComponent extends AbstractHudElement {

    private final Map<String, StaffModule> modules = new LinkedHashMap<>();
    private final Set<String> staffPrefixes = Set.of(
        "helper", "admin", "moder", "staff", "curator",
        "помощник", "админ", "модер", "стажёр", "сотрудник"
    );
    private final Map<String, Identifier> skinCache = new HashMap<>();
    private long lastStaffUpdate = 0;
    private long lastSkinClear = 0;
    private final Set<String> currentKeys = new HashSet<>();
    private float animatedWidth = 61.5f;
    private float alphaAnim = 0;

    public StaffComponent() {
        super("Staff", 10, 80, 80, 23, true);
        stopAnimation();
    }

    @Override
    public void tick() {
        long now = System.currentTimeMillis();
        if (now - lastStaffUpdate > 50 && mc.getNetworkHandler() != null) {
            updateStaffList();
            lastStaffUpdate = now;
        }
        if (now - lastSkinClear > 30000) {
            skinCache.clear();
            lastSkinClear = now;
        }

        modules.entrySet().removeIf(e -> e.getValue().animVal <= 0);

        boolean isFound = false;
        for (StaffModule m : modules.values()) {
            float target = currentKeys.contains(m.displayName) ? 1 : 0;
            m.animVal += (target - m.animVal) * 0.1f;
            if (m.animVal > 0.01f) isFound = true;
        }

        float targetAlpha = (isFound || mc.currentScreen instanceof ChatScreen) ? 1.0f : 0.0f;
        alphaAnim += (targetAlpha - alphaAnim) * 0.1f;
    }

    @Override
    public boolean visible() {
        return alphaAnim > 0.01f || !scaleAnimation.isFinished(rich.util.animations.Direction.BACKWARDS);
    }

    @Override
    public void drawDraggable(DrawContext ctx, int alpha) {
        float a = (alpha / 255.0f) * alphaAnim;
        int themeColor = ColorUtil.astolfo(10000, 0, 0.7f, 0.8f, (int) (255 * a));
        int bgAlpha = (int) (64 * a);

        float x = getX();
        float y = getY();
        float defaultWidth = 61.5f;
        float height = 14.5f;

        Render2D.blur(x, y, animatedWidth, 14.5f, 15, 2, new Color(255, 255, 255, (int) (255 * a)).getRGB());
        Render2D.rect(x, y, animatedWidth, 14.5f, ColorUtil.rgba(30, 30, 30, bgAlpha), 2);

        Fonts.HUD_ICONS.draw("O", x + 4, y + 2, 9, themeColor);
        Fonts.BOLD.draw("|", x + 15, y + 2, 7, new Color(166, 166, 166, (int) (255 * a)).getRGB());
        Fonts.BOLD.draw("Staff online", x + 18.5f, y + 2, 7.5f, new Color(255, 255, 255, (int) (255 * a)).getRGB());
        y += 15.5f;

        for (StaffModule m : modules.values()) {
            if (m.animVal < 0.01f) continue;
            float elemAlpha = m.animVal * a;
            float elemY = y + m.animVal * 3 - 3;
            height += 12 * m.animVal;

            Render2D.blur(x, elemY, animatedWidth, 11, 15, 2, new Color(255, 255, 255, (int) (255 * elemAlpha)).getRGB());
            Render2D.rect(x, elemY, animatedWidth, 11, ColorUtil.rgba(30, 30, 30, (int) (64 * elemAlpha)), 2);

            Fonts.BOLD.draw("|", x + animatedWidth - 10.5f, elemY + 3, 6.5f, new Color(166, 166, 166, (int) (255 * elemAlpha)).getRGB());

            int statusColor;
            if (m.status == 2) statusColor = ColorUtil.rgba(255, 32, 32, (int) (255 * elemAlpha));
            else if (m.status == 1) statusColor = ColorUtil.rgba(255, 220, 0, (int) (255 * elemAlpha));
            else statusColor = ColorUtil.rgba(32, 255, 32, (int) (255 * elemAlpha));

            Render2D.rect(x + animatedWidth - 7.5f, elemY + 3.5f, 4, 4, statusColor, 2);

            Identifier skin = skinCache.getOrDefault(m.name, DefaultSkinHelper.getSteve().body().texturePath());
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, skin, (int) (x + 2.5f), (int) (elemY + 2.25f), 0, 0, 6, 6, 64, 64);

            Fonts.BOLD.draw(m.displayName, x + 11.5f, elemY + 3, 7, new Color(255, 255, 255, (int) (255 * elemAlpha)).getRGB());

            float elemWidth = Fonts.BOLD.getWidth(m.displayName, 7) + 28;
            if (elemWidth > defaultWidth) defaultWidth = elemWidth;
            y += 12 * m.animVal;
        }

        animatedWidth += (defaultWidth - animatedWidth) * 0.1f;
        setWidth((int) animatedWidth);
        setHeight((int) height);
    }

    private void updateStaffList() {
        if (mc.getNetworkHandler() == null) return;
        currentKeys.clear();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            GameProfile profile = entry.getProfile();
            Text displayName = entry.getDisplayName();
            if (displayName == null || profile == null) continue;

            String display = displayName.getString();
            String name = profile.name();
            String prefix = display.replace(name, "").trim();
            if (prefix.length() < 2) continue;
            if (!containsAnyKeyword(prefix)) continue;

            int status = entry.getGameMode() == GameMode.SPECTATOR ? 2 : 0;
            modules.computeIfAbsent(display, k -> new StaffModule(display, name, status));
            currentKeys.add(display);
        }
    }

    private boolean containsAnyKeyword(String text) {
        String lower = text.toLowerCase(Locale.US);
        for (String keyword : staffPrefixes) {
            if (lower.contains(keyword)) return true;
        }
        return false;
    }

    private static class StaffModule {
        String displayName;
        String name;
        int status;
        float animVal = 0;
        StaffModule(String displayName, String name, int status) {
            this.displayName = displayName;
            this.name = name;
            this.status = status;
        }
    }
}
