package com.holdmylua.source.global;

import java.util.HashMap;
import java.util.List;
import com.holdmylua.source.model.ModelPartAnimator;
import com.holdmylua.source.scripting.custom_api.DebugTextRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

public class GlobalsStorage {
    public static volatile boolean enabled = false;

    public static HashMap<String, Boolean> renderAsBlock = new HashMap<>();
    public static HashMap<String, Boolean> translateItem = new HashMap<>();
    public static HashMap<String, Boolean> applyBlockRotation = new HashMap<>();
    public static HashMap<String, Integer> itemSwingSpeed = new HashMap<>();
    public static HashMap<String, Object> useDuration = new HashMap<>();
    public static HashMap<String, Boolean> usingItem = new HashMap<>();
    public static final HashMap<String, Object> registry = new HashMap<>();
    public static final List<com.holdmylua.source.patricles.Particle> particles = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final ModelPartAnimator modelPartAnimator = new ModelPartAnimator();
    public static final DebugTextRenderer debugTextRenderer = new DebugTextRenderer();
    public static ItemStack mainHandItem;
    public static ItemStack offHandItem;
    public static ItemStack renderedStack;
}
