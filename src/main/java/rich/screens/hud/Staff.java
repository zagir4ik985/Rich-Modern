package rich.screens.hud;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import rich.screens.hud.port.Animation;
import rich.screens.hud.port.BorderRadius;
import rich.screens.hud.port.ColorRGBA;
import rich.screens.hud.port.CustomDrawContext;
import rich.screens.hud.port.DrawUtil;
import rich.screens.hud.port.Easing;
import rich.screens.hud.port.PortHudElement;
import rich.screens.hud.port.Theme;
import rich.util.render.font.Fonts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class Staff extends PortHudElement {

    private final Map<String, StaffModule> modules = new LinkedHashMap<>();
    private final Set<String> staffPrefix = Set.of("helper", "ᴀдмин", "moder", "staff", "admin", "curator", "стажёр", "сотрудник", "помощник", "админ", "модер", "ꔗ", "ꔥ", "ꔡ", "ꔳ");
    private final Map<String, Identifier> skinTextureCache = new HashMap<>();
    private long lastStaffUpdate = 0L;
    private long lastSkinCacheClear = 0L;
    private final Set<String> currentStaffKeys = new HashSet<>();
    private final Animation widthAnimation;
    private final Animation alpha;

    private static final float blurStrength = 15.0f;
    private static final float cornerRadius = 2.25f;

    public Staff() {
        super("Staff", 300, 150, 80, 23, true);
        this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
        this.alpha = new Animation(200L, Easing.CUBIC_OUT);
    }

    private void drawBlurBackground(CustomDrawContext ctx, float x, float y, float width, float height, Theme theme, float animation) {
        DrawUtil.drawBlur(
                ctx.getMatrices(), x, y, width, height,
                blurStrength,
                BorderRadius.all(cornerRadius),
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
                BorderRadius.all(cornerRadius),
                backgroundColor
        );
    }

    @Override
    public void renderPort(CustomDrawContext ctx, int alpha) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastStaffUpdate > 50L && mc.getNetworkHandler() != null) {
            this.updateStaffList();
            this.lastStaffUpdate = currentTime;
        }

        if (currentTime - this.lastSkinCacheClear > 30000L) {
            this.skinTextureCache.clear();
            this.lastSkinCacheClear = currentTime;
        }

        this.modules.entrySet().removeIf((entry) -> entry.getValue().isDelete());
        float posX = this.getPx();
        float posY = this.getPy();
        float defaultWidth = 61.5F;
        float height = 14.5F;
        boolean isFound = false;
        Iterator<Entry<String, StaffModule>> var9 = this.modules.entrySet().iterator();

        while (var9.hasNext()) {
            Entry<String, StaffModule> module = var9.next();
            module.getValue().animation.update(this.currentStaffKeys.contains(module.getKey()) ? 1.0F : 0.0F);
            if (module.getValue().animation.getValue() != 0.0F) {
                this.alpha.update(1.0F);
                isFound = true;
            }
        }

        if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
            this.alpha.update(0.0F);
        }

        if (mc.currentScreen instanceof ChatScreen) {
            this.alpha.update(1.0F);
        }

        Theme theme = new Theme();

        drawBlurBackground(ctx, posX, posY, this.widthAnimation.getValue(), 14.5F, theme, this.alpha.getValue());

        ctx.drawText(Fonts.NURIKI, "O", posX + 4F, posY + 5.5F, 9F, theme.getColor().withAlpha(255.0F * this.alpha.getValue()));

        ctx.drawText(Fonts.SEMIBOLD, ":", posX + 15.0F, posY + 4.75F, 7.0F, new ColorRGBA(166, 166, 166, 255.0F * this.alpha.getValue()));

        ctx.drawText(Fonts.SEMIBOLD, "Staff online", posX + 18.5F, posY + 4.75F, 7.5F, (new ColorRGBA(-1)).withAlpha(255.0F * this.alpha.getValue()));
        posY += 14.5F + 1.0F;
        Iterator<Entry<String, StaffModule>> var16 = this.modules.entrySet().iterator();

        while (var16.hasNext()) {
            Entry<String, StaffModule> module = var16.next();
            if (module.getValue().animation.getValue() != 0.0F) {
                height += 11.0F + 1.0F;
                Identifier skinTexture = this.skinTextureCache.get(module.getValue().name);
                if (skinTexture == null && mc.getNetworkHandler() != null) {
                    PlayerListEntry player = mc.getNetworkHandler().getPlayerList().stream().filter((p) ->
                            p.getProfile() != null && module.getValue().name.equals(p.getProfile().name())).findFirst().orElse(null);
                    if (player != null && player.getSkinTextures() != null) {
                        skinTexture = player.getSkinTextures().body().texturePath();
                        this.skinTextureCache.put(module.getValue().name, skinTexture);
                    }
                }

                if (skinTexture == null) {
                    skinTexture = DefaultSkinHelper.getSteve().body().texturePath();
                }

                Text prefix = module.getValue().displayNameText;
                float elementsWidth = Fonts.SEMIBOLD.getWidth(prefix.getString(), 7.0F) + 28.0F;

                float elementAlpha = module.getValue().animation.getValue() * this.alpha.getValue();

                float elementY = posY + module.getValue().animation.getValue() * 3.0F - 3.0F;
                drawBlurBackground(ctx, posX, elementY, this.widthAnimation.getValue(), 11.0F, theme, elementAlpha);

                ctx.drawText(Fonts.SEMIBOLD, ":", posX + this.widthAnimation.getValue() - 10.5F, elementY + 3.25F, 6.5F, new ColorRGBA(166, 166, 166, 255.0F * elementAlpha));

                ColorRGBA statusColor;
                if (module.getValue().status == Status.SPEC) {
                    statusColor = new ColorRGBA(255, 32, 32, 255.0F * elementAlpha);
                } else if (module.getValue().status == Status.VANISHED) {
                    statusColor = new ColorRGBA(255, 220, 0, 255.0F * elementAlpha);
                } else {
                    statusColor = new ColorRGBA(32, 255, 32, 255.0F * elementAlpha);
                }
                DrawUtil.drawRoundedRect(ctx.getMatrices(), posX + this.widthAnimation.getValue() - 7.5F, elementY + 3.5F, 4.0F, 4.0F, BorderRadius.all(2.0F), statusColor);

                DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), skinTexture, posX + 2.5F, elementY + 2.25F, 6.5F, BorderRadius.all(2.0F), ColorRGBA.WHITE.withAlpha(module.getValue().animation.getValue() * 255.0F));

                ctx.drawText(Fonts.SEMIBOLD, prefix.getString(), posX + 11.5F, elementY + 3.25F, 7.0F, module.getValue().animation.getValue() * 255.0F * this.alpha.getValue());
                if (elementsWidth > defaultWidth) {
                    defaultWidth = elementsWidth;
                }

                posY += (11.0F + 1.0F) * module.getValue().animation.getValue();
            }
        }

        this.widthAnimation.update(defaultWidth);
        this.pw = this.widthAnimation.getValue();
        this.ph = height;
        this.width = (int) this.pw;
        this.height = (int) this.ph;
    }

    private void updateStaffList() {
        if (mc.getNetworkHandler() != null) {
            this.currentStaffKeys.clear();
            Iterator<PlayerListEntry> var1 = mc.getNetworkHandler().getPlayerList().iterator();

            while (true) {
                PlayerListEntry entry;
                Text displayName;
                String display;
                String name;
                String prefix;
                do {
                    do {
                        do {
                            GameProfile profile;
                            do {
                                do {
                                    if (!var1.hasNext()) {
                                        return;
                                    }

                                    entry = var1.next();
                                    profile = entry.getProfile();
                                    displayName = entry.getDisplayName();
                                } while (displayName == null);
                            } while (profile == null);

                            display = displayName.getString();
                            name = profile.name();
                            prefix = display.replace(name, "").trim();
                            String var10000 = prefix.replaceAll("ꔗ", String.valueOf(Formatting.BLUE) + "MODER").replaceAll("ꔥ", String.valueOf(Formatting.BLUE) + "ST.MODER").replaceAll("ꔡ", String.valueOf(Formatting.LIGHT_PURPLE) + "MODER+").replaceAll("ꔀ", String.valueOf(Formatting.GRAY) + "PLAYER").replaceAll("ꔉ", String.valueOf(Formatting.YELLOW) + "HELPER").replaceAll("◆", "@").replaceAll("┃", "|").replaceAll("ꔳ", String.valueOf(Formatting.AQUA) + "ML.ADMIN");
                            String var10002 = String.valueOf(Formatting.RED);
                            prefix = var10000.replaceAll("ꔅ", var10002 + "Y" + String.valueOf(Formatting.WHITE) + "T").replaceAll("ꔂ", String.valueOf(Formatting.BLUE) + "D.MODER").replaceAll("ꕠ", String.valueOf(Formatting.YELLOW) + "D.HELPER").replaceAll("ꕄ", String.valueOf(Formatting.RED) + "DRACULA").replaceAll("ꔖ", String.valueOf(Formatting.AQUA) + "OVERLORD").replaceAll("ꕈ", String.valueOf(Formatting.GREEN) + "COBRA").replaceAll("ꔨ", String.valueOf(Formatting.LIGHT_PURPLE) + "DRAGON").replaceAll("ꔤ", String.valueOf(Formatting.RED) + "IMPERATOR").replaceAll("ꔠ", String.valueOf(Formatting.GOLD) + "MAGISTER").replaceAll("ꔄ", String.valueOf(Formatting.BLUE) + "HERO").replaceAll("ꔒ", String.valueOf(Formatting.GREEN) + "AVENGER").replaceAll("ꕒ", String.valueOf(Formatting.WHITE) + "RABBIT").replaceAll("ꔈ", String.valueOf(Formatting.YELLOW) + "TITAN").replaceAll("ꕀ", String.valueOf(Formatting.DARK_GREEN) + "HYDRA").replaceAll("ꔶ", String.valueOf(Formatting.GOLD) + "TIGER").replaceAll("ꔲ", String.valueOf(Formatting.DARK_PURPLE) + "BULL").replaceAll("ꕖ", String.valueOf(Formatting.BLACK) + "BUNNY").replaceAll("ꕗꕘ", String.valueOf(Formatting.YELLOW) + "SPONSOR").replaceAll("\ud83d\udd25", "@").replaceAll("ᴀ", "A").replaceAll("ʙ", "B").replaceAll("ᴄ", "C").replaceAll("ᴅ", "D").replaceAll("ᴇ", "E").replaceAll("ғ", "F").replaceAll("ɢ", "G").replaceAll("ʜ", "H").replaceAll("ɪ", "I").replaceAll("ᴊ", "J").replaceAll("ᴋ", "K").replaceAll("ʟ", "L").replaceAll("ᴍ", "M").replaceAll("ɴ", "N").replaceAll("ꜱ", "S").replaceAll("ᴏ", "O").replaceAll("ᴘ", "P").replaceAll("ǫ", "Q").replaceAll("ʀ", "R").replaceAll("ᴛ", "T").replaceAll("ᴜ", "U").replaceAll("ᴠ", "V").replaceAll("ᴡ", "W").replaceAll("ꜰ", "F").replaceAll("ʏ", "Y").replaceAll("ᴢ", "Z");
                        } while (prefix.length() < 2);
                    } while (!this.containsAnyKeyword(prefix));
                } while (prefix.contains("D.ADMIN") || prefix.contains("sTAFF"));

                Status status = entry.getGameMode() == GameMode.SPECTATOR ? Status.VANISHED : Status.NONE;
                final Text finalDisplayName = displayName;
                final String finalDisplay = display;
                final String finalName = name;
                final Status finalStatus = status;
                this.modules.computeIfAbsent(display, (k) ->
                        new StaffModule(finalDisplayName, finalDisplay, finalName, finalStatus));
                this.currentStaffKeys.add(display);
            }
        }
    }

    public boolean containsAnyKeyword(String text) {
        String lower = text.toLowerCase(Locale.US);
        Iterator<String> var3 = this.staffPrefix.iterator();

        String keyword;
        do {
            if (!var3.hasNext()) {
                return false;
            }

            keyword = var3.next();
        } while (!lower.contains(keyword));

        return true;
    }

    private static class StaffModule {
        private final Animation animation;
        private final Text displayNameText;
        private final String key;
        private final String name;
        private final Status status;
        private final long appearTime;

        public StaffModule(Text displayNameText, String key, String name, Status status) {
            this.animation = new Animation(250L, 0.01F, Easing.CUBIC_OUT);
            this.displayNameText = displayNameText;
            this.key = key;
            this.name = name;
            this.status = status;
            this.appearTime = System.currentTimeMillis();
        }

        public boolean isDelete() {
            return this.animation.getValue() == 0.0F;
        }
    }

    public enum Status {
        NONE,
        VANISHED,
        SPEC
    }
}
