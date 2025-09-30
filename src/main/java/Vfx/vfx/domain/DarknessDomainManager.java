package Vfx.vfx.domain;

import Vfx.vfx.Vfx;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Vfx.MODID)
public class DarknessDomainManager {
    private static final Map<ResourceKey<Level>, DarknessDomainManager> MANAGERS = new HashMap<>();

    public static DarknessDomainManager get(ServerLevel level) {
        return MANAGERS.compute(level.dimension(), (key, existing) -> {
            if (existing == null || existing.level != level) {
                return new DarknessDomainManager(level);
            }
            return existing;
        });
    }

    private final ServerLevel level;
    private final List<DarknessDomain> activeDomains = new ArrayList<>();

    private DarknessDomainManager(ServerLevel level) {
        this.level = level;
    }

    public void activateDomain(BlockPos center, int size, int durationTicks) {
        DarknessDomain domain = new DarknessDomain(level, center, size, durationTicks);
        domain.apply();
        activeDomains.add(domain);
    }

    private void tick() {
        Iterator<DarknessDomain> iterator = activeDomains.iterator();
        while (iterator.hasNext()) {
            DarknessDomain domain = iterator.next();
            if (domain.isExpired()) {
                domain.revert();
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void handleServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        for (Iterator<Map.Entry<ResourceKey<Level>, DarknessDomainManager>> iterator = MANAGERS.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<ResourceKey<Level>, DarknessDomainManager> entry = iterator.next();
            ServerLevel serverLevel = event.getServer().getLevel(entry.getKey());
            if (serverLevel == null) {
                iterator.remove();
                continue;
            }
            entry.getValue().tick();
        }
    }

    private static class DarknessDomain {
        private final ServerLevel level;
        private final Map<BlockPos, BlockState> originalBlocks = new HashMap<>();
        private final long expiryGameTime;
        private final int size;
        private final BlockPos center;

        private DarknessDomain(ServerLevel level, BlockPos center, int size, int durationTicks) {
            this.level = level;
            this.center = center;
            this.size = size;
            this.expiryGameTime = level.getGameTime() + durationTicks;
        }

        private void apply() {
            int half = size / 2;
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            int minY = Math.max(level.getMinBuildHeight(), center.getY() - half);
            int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + half);

            for (int x = center.getX() - half; x <= center.getX() + half; x++) {
                for (int z = center.getZ() - half; z <= center.getZ() + half; z++) {
                    ensureChunksLoaded(x, z);
                    for (int y = minY; y <= maxY; y++) {
                        mutable.set(x, y, z);
                        BlockPos immutablePos = mutable.immutable();
                        BlockState currentState = level.getBlockState(immutablePos);
                        if (!currentState.is(Blocks.BLACK_CONCRETE)) {
                            originalBlocks.putIfAbsent(immutablePos, currentState);
                            level.setBlock(immutablePos, Blocks.BLACK_CONCRETE.defaultBlockState(), 3);
                        }
                    }
                }
            }

            spawnDarknessCloud(half);
        }

        private void ensureChunksLoaded(int x, int z) {
            BlockPos targetPos = new BlockPos(x, center.getY(), z);
            level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, new ChunkPos(targetPos), 1, center);
        }

        private void spawnDarknessCloud(int half) {
            AreaEffectCloud cloud = new AreaEffectCloud(level, center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5);
            cloud.setRadius(Math.max(half, 1));
            cloud.setDuration((int) (expiryGameTime - level.getGameTime()));
            cloud.setWaitTime(0);
            cloud.setRadiusPerTick(0);
            cloud.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200));
            level.addFreshEntity(cloud);
        }

        private boolean isExpired() {
            return level.getGameTime() >= expiryGameTime;
        }

        private void revert() {
            originalBlocks.forEach((pos, state) -> {
                if (level.isLoaded(pos)) {
                    level.setBlock(pos, state, 3);
                }
            });
            originalBlocks.clear();
        }
    }
}