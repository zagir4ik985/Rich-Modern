package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.client.draggables.HudManager;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.ModuleStructure;
import rich.util.Instance;
import rich.Initialization;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.modules.module.setting.implement.BooleanSetting;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Hud extends ModuleStructure {

    public static Hud getInstance() {
        return Instance.get(Hud.class);
    }

    public MultiSelectSetting interfaceSettings = new MultiSelectSetting("Elements", "Watermark")
        .value("Watermark", "HotBar", "Information", "TargetHud", "Keybinds", "Staff", "Potions", "Notifications", "Cooldowns", "Inventory")
        .selected("Watermark", "Information", "Keybinds", "Potions", "Notifications", "Cooldowns", "Inventory");

    public BooleanSetting showBps = new BooleanSetting("Show BPS", "Show BPS counter").setValue(true);
    public BooleanSetting showTps = new BooleanSetting("Show TPS", "Show TPS counter").setValue(true);

    public Hud() {
        super("Hud", ModuleCategory.RENDER);
        settings(interfaceSettings, showBps, showTps);
        Initialization.getInstance().getManager().getHudManager().initElements();
    }
}
