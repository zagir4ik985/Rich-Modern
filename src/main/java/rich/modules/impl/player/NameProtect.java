package rich.modules.impl.player;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.events.api.EventHandler;
import rich.events.api.events.render.TextFactoryEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.TextSetting;
import rich.util.Instance;
import rich.util.repository.friend.FriendUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NameProtect extends ModuleStructure {

    TextSetting nameSetting = new TextSetting("Имя", "Никнейм, который будет заменен на ваш").setText("Protected").setMax(32);
    BooleanSetting friendsSetting = new BooleanSetting("Друзья", "Скрывает никнеймы друзей").setValue(true);

    public NameProtect() {
        super("NameProtect","Name Protect", ModuleCategory.PLAYER);
        settings(friendsSetting);
    }

    public static boolean isEnabled() {
        NameProtect instance = Instance.get(NameProtect.class);
        return instance != null && instance.isState();
    }

    public static String getCustomName() {
        NameProtect instance = Instance.get(NameProtect.class);
        if (instance != null && instance.nameSetting != null) {
            return instance.nameSetting.getText();
        }
        return "Unknown";
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTextFactory(TextFactoryEvent e) {
        e.replaceText(mc.getSession().getUsername(), nameSetting.getText());
        if (friendsSetting.isValue()) {
            replaceFriendNames(e);
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void replaceFriendNames(TextFactoryEvent e) {
        FriendUtils.getFriends().forEach(friend -> e.replaceText(friend.getName(), nameSetting.getText()));
    }
}