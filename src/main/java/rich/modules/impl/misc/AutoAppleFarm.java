package rich.modules.impl.misc;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.string.chat.ChatMessage;



public class AutoAppleFarm extends ModuleStructure {

    private final SliderSettings delay = new SliderSettings("Задержка (мс)", "Задержка между тиками")
            .range(50, 200)
            .setValue(50);

    private enum State {
        PLACING_SAPLING,
        GROWING_TREE,
        BREAKING_LEAVES,
        BREAKING_LOG,
        WAITING
    }

    private State currentState = State.WAITING;
    private BlockPos treePos = null;
    private boolean running = false;
    private boolean isBreaking = false;
    private BlockPos currentBreakingPos = null;
    private int tickCounter = 0;

    public AutoAppleFarm() {
        super("AutoAppleFarm", "Автоматическая ферма яблок", ModuleCategory.MISC);
        settings(delay);
    }

    @Override
    public void activate() {
        if (running) return;
        currentState = State.PLACING_SAPLING;
        treePos = null;
        running = true;
        isBreaking = false;
        currentBreakingPos = null;
        tickCounter = 0;
        ChatMessage.brandmessage("[AutoAppleFarm] Включен");
    }

    @Override
    public void deactivate() {
        isBreaking = false;
        running = false;
        currentBreakingPos = null;
        treePos = null;
        currentState = State.WAITING;
        ChatMessage.brandmessage("[AutoAppleFarm] Выключен");
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (!running) return;

        tickCounter++;
        int delayTicks = (int) (delay.getValue() / 50);
        if (delayTicks < 1) delayTicks = 1;
        if (tickCounter % delayTicks != 0) return;

        if (!checkInventory()) {
            switchState();
            return;
        }

        autoSelectTool();

        switch (currentState) {
            case PLACING_SAPLING -> placeSapling();
            case GROWING_TREE -> growTree();
            case BREAKING_LEAVES -> breakLeaves();
            case BREAKING_LOG -> breakLog();
            case WAITING -> {}
        }
    }

    private void autoSelectTool() {
        if (mc.crosshairTarget instanceof BlockHitResult hitResult) {
            BlockPos targetPos = hitResult.getBlockPos();
            if (targetPos != null) {
                var targetBlock = mc.world.getBlockState(targetPos).getBlock();

                if (targetBlock == Blocks.OAK_LOG || targetBlock == Blocks.BIRCH_LOG ||
                        targetBlock == Blocks.SPRUCE_LOG || targetBlock == Blocks.JUNGLE_LOG ||
                        targetBlock == Blocks.ACACIA_LOG || targetBlock == Blocks.DARK_OAK_LOG ||
                        targetBlock == Blocks.CHERRY_LOG || targetBlock == Blocks.MANGROVE_LOG) {
                    int axeSlot = findToolSlot("axe");
                    if (axeSlot != -1) {
                        mc.player.getInventory().setSelectedSlot(axeSlot);
                    }
                } else if (targetBlock == Blocks.OAK_LEAVES || targetBlock == Blocks.BIRCH_LEAVES ||
                        targetBlock == Blocks.SPRUCE_LEAVES || targetBlock == Blocks.JUNGLE_LEAVES ||
                        targetBlock == Blocks.ACACIA_LEAVES || targetBlock == Blocks.DARK_OAK_LEAVES ||
                        targetBlock == Blocks.CHERRY_LEAVES || targetBlock == Blocks.MANGROVE_LEAVES) {
                    int hoeSlot = findToolSlot("hoe");
                    if (hoeSlot != -1) {
                        mc.player.getInventory().setSelectedSlot(hoeSlot);
                    }
                }
            }
        }
    }

    private boolean checkInventory() {
        boolean hasSapling = false;
        boolean hasBoneMeal = false;
        boolean hasAxe = false;
        boolean hasHoe = false;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() == Items.OAK_SAPLING || stack.getItem() == Items.BIRCH_SAPLING
                        || stack.getItem() == Items.SPRUCE_SAPLING || stack.getItem() == Items.JUNGLE_SAPLING
                        || stack.getItem() == Items.ACACIA_SAPLING || stack.getItem() == Items.DARK_OAK_SAPLING
                        || stack.getItem() == Items.CHERRY_SAPLING) hasSapling = true;
                if (stack.getItem() == Items.BONE_MEAL) hasBoneMeal = true;

                String name = stack.getItem().toString().toLowerCase();
                if (name.contains("axe") && !name.contains("pick")) hasAxe = true;
                if (name.contains("hoe")) hasHoe = true;
            }
        }

        if (!hasSapling) {
            ChatMessage.brandmessage("[AutoAppleFarm] Нет саженца");
            return false;
        }
        if (!hasBoneMeal) {
            ChatMessage.brandmessage("[AutoAppleFarm] Нет костной муки");
            return false;
        }
        if (!hasAxe) {
            ChatMessage.brandmessage("[AutoAppleFarm] Нет топора");
            return false;
        }
        if (!hasHoe) {
            ChatMessage.brandmessage("[AutoAppleFarm] Нет мотыги");
            return false;
        }
        return true;
    }

    private void placeSapling() {
        if (isBreaking) stopBreaking();

        BlockPos groundPos = findGroundPos();
        if (groundPos == null) return;

        BlockPos airPos = groundPos.up();
        var existingBlock = mc.world.getBlockState(airPos).getBlock();
        if (existingBlock == Blocks.OAK_LOG || existingBlock == Blocks.OAK_LEAVES
                || existingBlock == Blocks.BIRCH_LOG || existingBlock == Blocks.BIRCH_LEAVES
                || existingBlock == Blocks.CHERRY_LOG || existingBlock == Blocks.CHERRY_LEAVES
                || existingBlock == Blocks.SPRUCE_LOG || existingBlock == Blocks.SPRUCE_LEAVES
                || existingBlock == Blocks.JUNGLE_LOG || existingBlock == Blocks.JUNGLE_LEAVES
                || existingBlock == Blocks.ACACIA_LOG || existingBlock == Blocks.ACACIA_LEAVES
                || existingBlock == Blocks.DARK_OAK_LOG || existingBlock == Blocks.DARK_OAK_LEAVES) {
            currentState = State.BREAKING_LEAVES;
            return;
        }

        if (mc.world.getBlockState(airPos).isAir()) {
            int saplingSlot = findItemSlot(Items.OAK_SAPLING);
            if (saplingSlot == -1) saplingSlot = findItemSlot(Items.BIRCH_SAPLING);
            if (saplingSlot == -1) saplingSlot = findItemSlot(Items.SPRUCE_SAPLING);
            if (saplingSlot == -1) saplingSlot = findItemSlot(Items.JUNGLE_SAPLING);
            if (saplingSlot == -1) saplingSlot = findItemSlot(Items.ACACIA_SAPLING);
            if (saplingSlot == -1) saplingSlot = findItemSlot(Items.DARK_OAK_SAPLING);
            if (saplingSlot == -1) saplingSlot = findItemSlot(Items.CHERRY_SAPLING);

            if (saplingSlot != -1) {
                mc.player.getInventory().setSelectedSlot(saplingSlot);
                lookAt(airPos);

                BlockHitResult hit = new BlockHitResult(
                        Vec3d.ofCenter(groundPos), Direction.UP, groundPos, false
                );
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);

                treePos = airPos;
                currentState = State.GROWING_TREE;
                ChatMessage.brandmessage("[AutoAppleFarm] Саженец поставлен");
            }
        } else if (mc.world.getBlockState(airPos).getBlock() == Blocks.OAK_SAPLING
                || mc.world.getBlockState(airPos).getBlock() == Blocks.CHERRY_SAPLING
                || mc.world.getBlockState(airPos).getBlock() == Blocks.BIRCH_SAPLING
                || mc.world.getBlockState(airPos).getBlock() == Blocks.SPRUCE_SAPLING
                || mc.world.getBlockState(airPos).getBlock() == Blocks.JUNGLE_SAPLING
                || mc.world.getBlockState(airPos).getBlock() == Blocks.ACACIA_SAPLING
                || mc.world.getBlockState(airPos).getBlock() == Blocks.DARK_OAK_SAPLING) {
            treePos = airPos;
            currentState = State.GROWING_TREE;
        }
    }

    private void growTree() {
        if (treePos == null) {
            currentState = State.PLACING_SAPLING;
            return;
        }

        if (mc.world.getBlockState(treePos).getBlock() == Blocks.OAK_SAPLING
                || mc.world.getBlockState(treePos).getBlock() == Blocks.BIRCH_SAPLING
                || mc.world.getBlockState(treePos).getBlock() == Blocks.SPRUCE_SAPLING
                || mc.world.getBlockState(treePos).getBlock() == Blocks.JUNGLE_SAPLING
                || mc.world.getBlockState(treePos).getBlock() == Blocks.ACACIA_SAPLING
                || mc.world.getBlockState(treePos).getBlock() == Blocks.DARK_OAK_SAPLING
                || mc.world.getBlockState(treePos).getBlock() == Blocks.CHERRY_SAPLING) {
            int boneMealSlot = findItemSlot(Items.BONE_MEAL);
            if (boneMealSlot != -1) {
                mc.player.getInventory().setSelectedSlot(boneMealSlot);
                lookAt(treePos);

                BlockHitResult hit = new BlockHitResult(
                        Vec3d.ofCenter(treePos), Direction.UP, treePos, false
                );
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        } else {
            currentState = State.BREAKING_LEAVES;
            ChatMessage.brandmessage("[AutoAppleFarm] Дерево выросло");
        }
    }

    private void breakLeaves() {
        BlockPos leafPos = findNearestBlock(Blocks.OAK_LEAVES);
        if (leafPos == null) leafPos = findNearestBlock(Blocks.BIRCH_LEAVES);
        if (leafPos == null) leafPos = findNearestBlock(Blocks.CHERRY_LEAVES);
        if (leafPos == null) leafPos = findNearestBlock(Blocks.SPRUCE_LEAVES);
        if (leafPos == null) leafPos = findNearestBlock(Blocks.JUNGLE_LEAVES);
        if (leafPos == null) leafPos = findNearestBlock(Blocks.ACACIA_LEAVES);
        if (leafPos == null) leafPos = findNearestBlock(Blocks.DARK_OAK_LEAVES);

        if (leafPos != null) {
            if (currentBreakingPos == null || !currentBreakingPos.equals(leafPos)) {
                if (isBreaking) stopBreaking();
                currentBreakingPos = leafPos;
                isBreaking = true;
            }
            lookAt(leafPos);
            mc.getNetworkHandler().sendPacket(
                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, leafPos, Direction.DOWN));
            mc.getNetworkHandler().sendPacket(
                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, leafPos, Direction.DOWN));
        } else {
            if (isBreaking) stopBreaking();
            currentState = State.BREAKING_LOG;
        }
    }

    private void breakLog() {
        BlockPos logPos = findNearestBlock(Blocks.OAK_LOG);
        if (logPos == null) logPos = findNearestBlock(Blocks.BIRCH_LOG);
        if (logPos == null) logPos = findNearestBlock(Blocks.CHERRY_LOG);
        if (logPos == null) logPos = findNearestBlock(Blocks.SPRUCE_LOG);
        if (logPos == null) logPos = findNearestBlock(Blocks.JUNGLE_LOG);
        if (logPos == null) logPos = findNearestBlock(Blocks.ACACIA_LOG);
        if (logPos == null) logPos = findNearestBlock(Blocks.DARK_OAK_LOG);

        if (logPos != null) {
            if (currentBreakingPos == null || !currentBreakingPos.equals(logPos)) {
                if (isBreaking) stopBreaking();
                currentBreakingPos = logPos;
                isBreaking = true;
            }
            lookAt(logPos);
            mc.getNetworkHandler().sendPacket(
                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, logPos, Direction.DOWN));
            mc.getNetworkHandler().sendPacket(
                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, logPos, Direction.DOWN));
        } else {
            if (isBreaking) stopBreaking();
            currentState = State.PLACING_SAPLING;
            treePos = null;
            ChatMessage.brandmessage("[AutoAppleFarm] Дерево срублено");
        }
    }

    private void stopBreaking() {
        isBreaking = false;
        currentBreakingPos = null;
    }

    private BlockPos findGroundPos() {
        Direction facing = mc.player.getHorizontalFacing();
        BlockPos frontPos = mc.player.getBlockPos().offset(facing);

        for (int yOffset = 0; yOffset >= -2; yOffset--) {
            BlockPos checkPos = frontPos.down(Math.abs(yOffset));
            var block = mc.world.getBlockState(checkPos).getBlock();
            if (block == Blocks.GRASS_BLOCK || block == Blocks.DIRT ||
                    block == Blocks.COARSE_DIRT || block == Blocks.ROOTED_DIRT) {
                return checkPos;
            }
        }
        return null;
    }

    private BlockPos findNearestBlock(net.minecraft.block.Block block) {
        BlockPos nearest = null;
        double nearestDist = 8.0;

        int range = 5;
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = new BlockPos(
                            (int) Math.floor(mc.player.getX()) + x,
                            (int) Math.floor(mc.player.getY()) + y,
                            (int) Math.floor(mc.player.getZ()) + z
                    );
                    if (mc.world.getBlockState(pos).getBlock() == block) {
                        double dist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));
                        if (dist < nearestDist && dist <= 5.0) {
                            nearestDist = dist;
                            nearest = pos;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    private int findItemSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findToolSlot(String tool) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                String name = stack.getItem().toString().toLowerCase();
                if (tool.equals("axe") && name.contains("axe") && !name.contains("pick")) {
                    return i;
                }
                if (tool.equals("hoe") && name.contains("hoe")) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void lookAt(BlockPos pos) {
        Vec3d target = Vec3d.ofCenter(pos);
        Vec3d eyePos = mc.player.getEyePos();

        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;

        double distance = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, distance));
        pitch = Math.max(-90, Math.min(90, pitch));

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }
}
