package rich.util.game;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EntityCache {
    private static final EntityCache INSTANCE = new EntityCache();
    private static final long UPDATE_INTERVAL_MS = 50;

    private volatile List<PlayerEntity> players = Collections.emptyList();
    private volatile List<Entity> entities = Collections.emptyList();
    private volatile long lastUpdate = 0;

    private EntityCache() {}

    public static EntityCache getInstance() {
        return INSTANCE;
    }

    public synchronized void update() {
        long now = System.currentTimeMillis();
        if (now - lastUpdate < UPDATE_INTERVAL_MS) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            players = Collections.emptyList();
            entities = Collections.emptyList();
            lastUpdate = now;
            return;
        }

        List<PlayerEntity> newPlayers = new ArrayList<>();
        List<Entity> newEntities = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;

            if (entity instanceof PlayerEntity player) {
                newPlayers.add(player);
            }
            newEntities.add(entity);
        }

        players = Collections.unmodifiableList(newPlayers);
        entities = Collections.unmodifiableList(newEntities);
        lastUpdate = now;
    }

    public List<PlayerEntity> getPlayers() {
        return players;
    }

    public List<Entity> getEntities() {
        return entities;
    }
}
