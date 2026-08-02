package rich.screens.hud.port;

import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;

public class Keyboard {

    public static String getKeyName(int glfwKeyCode) {
        try {
            return InputUtil.fromKeyCode(new KeyInput(glfwKeyCode, 0, 0)).getLocalizedText().getString();
        } catch (Exception e) {
            return "None";
        }
    }
}
