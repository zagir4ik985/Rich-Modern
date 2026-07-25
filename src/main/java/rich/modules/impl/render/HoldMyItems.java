package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.Initialization;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HoldMyItems extends ModuleStructure {

    private static boolean enabled = false;

    public static boolean isEnabled() {
        return enabled;
    }

    public static HoldMyItems getInstance() {
        try {
            var repo = Initialization.getInstance().getManager().getModuleRepository();
            for (var m : repo.allModules()) {
                if (m instanceof HoldMyItems hmi) return hmi;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public HoldMyItems() {
        super("HoldMyItems", "3D item models, hand poses, particles via Lua", ModuleCategory.RENDER);
        settings();
    }

    @Override
    public void activate() {
        enabled = true;
    }

    @Override
    public void deactivate() {
        enabled = false;
    }

    @EventHandler
    public void onTick(TickEvent e) {
    }
}
