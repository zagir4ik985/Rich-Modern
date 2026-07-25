package rich.modules.impl.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
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
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.string.chat.ChatMessage;
import rich.util.Instance;
import rich.util.network.NetworkUtility;

public class BaseFinder extends ModuleStructure {

    public static BaseFinder getInstance() {
        return Instance.get(BaseFinder.class);
    }

    private final SelectSetting direction = new SelectSetting("Направление", "Направление поиска")
            .value("Север (+Z)", "Юг (-Z)", "Восток (+X)", "Запад (-X)", "Рандом")
            .selected("Север (+Z)");

    private final SliderSettings targetY = new SliderSettings("Целевой Y", "Y координата для поиска")
            .range(5, 60)
            .setValue(12);

    private final SliderSettings tunnelLength = new SliderSettings("Длина туннеля", "Длина туннеля")
            .range(5, 50)
            .setValue(20);

    private final SliderSettings checkInterval = new SliderSettings("Интервал проверки", "Интервал проверки привата")
            .range(3, 20)
            .setValue(8);

    private final BooleanSetting autoBridge = new BooleanSetting("Автомост", "Автоматическая постройка мостов")
            .setValue(true);

    private final BooleanSetting avoidLava = new BooleanSetting("Избегать лавы", "Избегать лавы")
            .setValue(true);

    private final BooleanSetting avoidVoid = new BooleanSetting("Избегать пустоты", "Избегать пустоты")
            .setValue(true);

    private final BooleanSetting runAway = new BooleanSetting("Убегать от мобов", "Убегать от мобов")
            .setValue(true);

    private enum State { IDLE, DESCEND, BRIDGE, MINE, CHECK_PRIVAT, RETURN }

    private State state = State.IDLE;
    private int ticks;
    private int blocksMined;
    private int oresChecked;
    private int basesFound;
    private int currentDirX;
    private int currentDirZ;
    private BlockPos startPos;
    private BlockPos currentPos;
    private BlockPos checkOrePos = null;
    private int checkOreTicks = 0;
    private final Random randomGen = new Random();
    private final List<BlockPos> foundBases = new ArrayList<>();

    public BaseFinder() {
        super("BaseFinder", "Поиск баз на фракциях", ModuleCategory.MISC);
        settings(direction, targetY, tunnelLength, checkInterval, autoBridge, avoidLava, avoidVoid, runAway);
    }

    @Override
    public void activate() {
        if (mc.player == null) return;
        state = State.IDLE;
        ticks = 0;
        blocksMined = 0;
        oresChecked = 1;
        checkOrePos = null;
        checkOreTicks = 0;
        startPos = mc.player.getBlockPos();
        currentPos = startPos;
        selectDirection();
        ChatMessage.brandmessage("[BaseFinder] Поиск баз начат. Направление: " + direction.getSelected());
    }

    @Override
    public void deactivate() {
        state = State.IDLE;
        ChatMessage.brandmessage("[BaseFinder] Остановлен. Проверено руд: " + oresChecked + ", найдено баз: " + basesFound);
    }

    private void selectDirection() {
        String sel = direction.getSelected();
        if (sel.equals("Север (+Z)")) { currentDirX = 0; currentDirZ = 1; }
        else if (sel.equals("Юг (-Z)")) { currentDirX = 0; currentDirZ = -1; }
        else if (sel.equals("Восток (+X)")) { currentDirX = 1; currentDirZ = 0; }
        else if (sel.equals("Запад (-X)")) { currentDirX = -1; currentDirZ = 0; }
        else {
            int dir = randomGen.nextInt(4);
            switch (dir) {
                case 0 -> { currentDirX = 0; currentDirZ = 1; }
                case 1 -> { currentDirX = 0; currentDirZ = -1; }
                case 2 -> { currentDirX = 1; currentDirZ = 0; }
                case 3 -> { currentDirX = -1; currentDirZ = 0; }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        ticks++;

        if (checkOrePos != null) {
            checkOreTicks++;
            if (checkOreTicks >= 40) {
                BlockState stateAt = mc.world.getBlockState(checkOrePos);
                if (!stateAt.isOf(Blocks.COAL_ORE) && !stateAt.isOf(Blocks.IRON_ORE)
                        && !stateAt.isOf(Blocks.GOLD_ORE) && !stateAt.isOf(Blocks.DIAMOND_ORE)
                        && !stateAt.isOf(Blocks.REDSTONE_ORE) && !stateAt.isOf(Blocks.LAPIS_ORE)
                        && !stateAt.isOf(Blocks.EMERALD_ORE)) {

                    if (!foundBases.contains(checkOrePos)) {
                        basesFound++;
                        foundBases.add(checkOrePos);
                        ChatMessage.brandmessage("[BaseFinder] База найдена! X:" + checkOrePos.getX()
                                + " Y:" + checkOrePos.getY() + " Z:" + checkOrePos.getZ()
                                + " (всего: " + basesFound + ")");
                    }
                }
                checkOrePos = null;
                checkOreTicks = 0;
            }
        }

        if (runAway.isValue() && hasDangerousMobs()) {
            ChatMessage.brandmessage("[BaseFinder] Опасность! Убегаем от мобов!");
            state = State.RETURN;
        }

        switch (state) {
            case IDLE -> handleIdle();
            case DESCEND -> handleDescend();
            case BRIDGE -> handleBridge();
            case MINE -> handleMine();
            case CHECK_PRIVAT -> handleCheckPrivat();
            case RETURN -> handleReturn();
        }
    }

    private void handleIdle() {
        if (mc.player.getY() > targetY.getValue()) {
            state = State.DESCEND;
        } else {
            state = State.BRIDGE;
        }
    }

    private void handleDescend() {
        if (mc.player.getY() <= targetY.getValue() + 1) {
            state = State.BRIDGE;
            return;
        }

        if (ticks % 3 == 0) {
            mc.getNetworkHandler().sendPacket(
                    new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                            mc.player.getBlockPos().down(),
                            Direction.DOWN
                    )
            );
            mc.getNetworkHandler().sendPacket(
                    new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                            mc.player.getBlockPos().down(),
                            Direction.DOWN
                    )
            );
        }
    }

    private void handleBridge() {
        if (!autoBridge.isValue()) {
            state = State.MINE;
            return;
        }

        faceDirection();
        NetworkUtility.sendInputPacket(true, false, false, false, false, false, true);

        BlockPos forward = currentPos.offset(getDirectionFromDir(currentDirX, currentDirZ));
        if (isAir(forward) && isAir(forward.up())) {
            placeBlock(forward.down());
            currentPos = forward;
        } else if (!isAir(forward) && !isAir(forward.up())) {
            state = State.MINE;
        } else {
            currentPos = forward;
        }

        blocksMined++;

        if (blocksMined >= tunnelLength.getValue()) {
            blocksMined = 0;
            state = State.MINE;
        }
    }

    private void handleMine() {
        BlockPos forward = currentPos.offset(getDirectionFromDir(currentDirX, currentDirZ));

        if (avoidLava.isValue() && isLava(forward)) {
            ChatMessage.brandmessage("[BaseFinder] Лава обнаружена! Меняем позицию.");
            currentPos = currentPos.up();
            return;
        }

        if (avoidVoid.isValue() && forward.getY() < 5) {
            ChatMessage.brandmessage("[BaseFinder] Пустота! Возвращаемся.");
            state = State.RETURN;
            return;
        }

        faceDirection();
        NetworkUtility.sendInputPacket(true, false, false, false, false, false, true);

        if (!isAir(forward)) {
            if (checkOrePos == null || !forward.equals(checkOrePos)) {
                mc.getNetworkHandler().sendPacket(
                        new PlayerActionC2SPacket(
                                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                                forward,
                                getDirectionFromDir(currentDirX, currentDirZ)
                        )
                );
                mc.getNetworkHandler().sendPacket(
                        new PlayerActionC2SPacket(
                                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                                forward,
                                getDirectionFromDir(currentDirX, currentDirZ)
                        )
                );
                blocksMined++;
            }
        }

        currentPos = forward;

        if (blocksMined >= tunnelLength.getValue()) {
            blocksMined = 0;
            if (oresChecked % (int) checkInterval.getValue() == 0) {
                state = State.CHECK_PRIVAT;
            } else {
                currentPos = currentPos.up();
                state = State.BRIDGE;
            }
        }
    }

    private void handleCheckPrivat() {
        if (checkOrePos == null) {
            BlockPos checkPos = currentPos.offset(getDirectionFromDir(currentDirX, currentDirZ));
            if (placeOreForCheck(checkPos)) {
                checkOrePos = checkPos;
                checkOreTicks = 0;
                oresChecked++;
            }
        }
        state = State.MINE;
    }

    private void handleReturn() {
        if (startPos == null) {
            state = State.IDLE;
            return;
        }

        double dist = mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(startPos));
        if (dist < 3) {
            state = State.IDLE;
            mc.player.setSprinting(false);
            return;
        }

        double dx = startPos.getX() - mc.player.getX();
        double dz = startPos.getZ() - mc.player.getZ();
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));

        mc.player.setYaw((float) yaw);
        mc.player.setHeadYaw((float) yaw);
        mc.player.setSprinting(true);
        NetworkUtility.sendInputPacket(true, false, false, false, false, false, true);
    }

    private boolean placeOreForCheck(BlockPos pos) {
        int slot = findItem(Items.COAL_ORE);
        if (slot == -1) slot = findItem(Items.IRON_ORE);
        if (slot == -1) slot = findItem(Items.GOLD_ORE);
        if (slot == -1) slot = findItem(Items.DIAMOND_ORE);
        if (slot == -1) return false;

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);

        BlockHitResult hitResult = new BlockHitResult(
                Vec3d.ofCenter(pos), Direction.UP, pos, false
        );
        NetworkUtility.sendUse(Hand.MAIN_HAND, hitResult);

        mc.player.getInventory().setSelectedSlot(oldSlot);
        return true;
    }

    private boolean placeBlock(BlockPos pos) {
        int slot = findItem(Items.COBBLESTONE);
        if (slot == -1) slot = findItem(Items.STONE);
        if (slot == -1) return false;

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);

        BlockHitResult hitResult = new BlockHitResult(
                Vec3d.ofCenter(pos), Direction.UP, pos, false
        );
        NetworkUtility.sendUse(Hand.MAIN_HAND, hitResult);

        mc.player.getInventory().setSelectedSlot(oldSlot);
        return true;
    }

    private boolean isAir(BlockPos pos) {
        return mc.world.getBlockState(pos).isAir();
    }

    private boolean isLava(BlockPos pos) {
        return mc.world.getBlockState(pos).isOf(Blocks.LAVA);
    }

    private boolean hasDangerousMobs() {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof MobEntity mob) {
                if (mob.distanceTo(mc.player) < 16) {
                    return true;
                }
            }
        }
        return false;
    }

    private int findItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private Direction getDirectionFromDir(int dirX, int dirZ) {
        if (dirX == 0 && dirZ == 1) return Direction.SOUTH;
        if (dirX == 0 && dirZ == -1) return Direction.NORTH;
        if (dirX == 1 && dirZ == 0) return Direction.EAST;
        if (dirX == -1 && dirZ == 0) return Direction.WEST;
        return Direction.NORTH;
    }

    private void faceDirection() {
        float yaw;
        if (currentDirX == 0 && currentDirZ == 1) yaw = 180.0f;
        else if (currentDirX == 0 && currentDirZ == -1) yaw = 0.0f;
        else if (currentDirX == 1 && currentDirZ == 0) yaw = 90.0f;
        else if (currentDirX == -1 && currentDirZ == 0) yaw = -90.0f;
        else yaw = 0.0f;
        mc.player.setYaw(yaw);
        mc.player.setHeadYaw(yaw);
    }
}
