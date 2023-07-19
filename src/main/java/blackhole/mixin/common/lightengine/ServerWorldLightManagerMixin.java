package blackhole.mixin.common.lightengine;

import blackhole.common.light.StarLightEngine;
import blackhole.common.light.StarLightInterface;
import blackhole.common.light.StarLightLightingProvider;
import blackhole.common.util.CoordinateUtils;
import blackhole.common.util.SectionPos;
import blackhole.mixin.common.IExtendedChunkProvider;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.*;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Mixin(ServerWorldLightManager.class)
public abstract class ServerWorldLightManagerMixin extends WorldLightManager implements StarLightLightingProvider {

    @Final
    @Shadow
    private ChunkManager chunkManager;

    @Final
    @Shadow
    private static Logger LOGGER;

    @Shadow
    public abstract void func_215588_z_(); // tryScheduleUpdate

    public ServerWorldLightManagerMixin(final IExtendedChunkProvider chunkProvider, final boolean hasBlockLight, final boolean hasSkyLight) {
        super(chunkProvider, hasBlockLight, hasSkyLight);
    }

    @Unique
    private final Long2IntOpenHashMap chunksBeingWorkedOn = new Long2IntOpenHashMap();

    @Unique
    private void queueTaskForSection(final int chunkX, final int chunkY, final int chunkZ, final Supplier<CompletableFuture<Void>> runnable) {
        final ServerWorld world = (ServerWorld)this.getLightEngine().getWorld();

        final Chunk center = this.getLightEngine().getAnyChunkNow(chunkX, chunkZ);
        if (center == null || !center.getStatus().isAtLeast(ChunkStatus.LIGHT)) {
            // do not accept updates in unlit chunks, unless we might be generating a chunk. thanks to the amazing
            // chunk scheduling, we could be lighting and generating a chunk at the same time
            return;
        }

        if (center.getStatus() != ChunkStatus.FULL) {
            // do not keep chunk loaded, we are probably in a gen thread
            // if we proceed to add a ticket the chunk will be loaded, which is not what we want (avoid cascading gen)
            runnable.get();
            return;
        }

        if (!world.getChunkProvider().chunkManager.mainThread.isOnExecutionThread()) {
            // ticket logic is not safe to run off-main, re-schedule
            world.getChunkProvider().chunkManager.mainThread.execute(() -> {
                this.queueTaskForSection(chunkX, chunkY, chunkZ, runnable);
            });
            return;
        }

        final long key = CoordinateUtils.getChunkKey(chunkX, chunkZ);

        final CompletableFuture<Void> updateFuture = runnable.get();

        if (updateFuture == null) {
            // not scheduled
            return;
        }

        final int references = this.chunksBeingWorkedOn.addTo(key, 1);
        if (references == 0) {
            final ChunkPos pos = new ChunkPos(chunkX, chunkZ);
            world.getChunkProvider().registerTicket(StarLightInterface.CHUNK_WORK_TICKET, pos, 0, pos);
        }

        // append future to this chunk and 1 radius neighbours chunk save futures
        // this prevents us from saving the world without first waiting for the light engine

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                ChunkHolder neighbour = world.getChunkProvider().chunkManager.func_219220_a(CoordinateUtils.getChunkKey(dx + chunkX, dz + chunkZ)); // getUpdatingChunkIfPresent
                if (neighbour != null) {
                    neighbour.field_219315_j = neighbour.field_219315_j.thenCombine(updateFuture, (final Chunk curr, final Void ignore) -> { // chunkToSave
                        return curr;
                    });
                }
            }
        }

        updateFuture.thenAcceptAsync((final Void ignore) -> {
            final int newReferences = this.chunksBeingWorkedOn.get(key);
            if (newReferences == 1) {
                this.chunksBeingWorkedOn.remove(key);
                final ChunkPos pos = new ChunkPos(chunkX, chunkZ);
                world.getChunkProvider().releaseTicket(StarLightInterface.CHUNK_WORK_TICKET, pos, 0, pos);
            } else {
                this.chunksBeingWorkedOn.put(key, newReferences - 1);
            }
        }, world.getChunkProvider().chunkManager.mainThread).whenComplete((final Void ignore, final Throwable thr) -> {
            if (thr != null) {
                LOGGER.fatal("Failed to remove ticket level for post chunk task " + new ChunkPos(chunkX, chunkZ), thr);
            }
        });
    }

    /**
     * @reason Redirect scheduling call away from the vanilla light engine, as well as enforce
     * that chunk neighbours are loaded before the processing can occur
     * @author Spottedleaf
     */
    @Overwrite
    public void checkBlock(final BlockPos pos) {
        final BlockPos posCopy = pos.toImmutable();
        this.queueTaskForSection(posCopy.getX() >> 4, posCopy.getY() >> 4, posCopy.getZ() >> 4, () -> {
            return this.getLightEngine().blockChange(posCopy);
        });
    }

    /**
     * @reason Avoid messing with the vanilla light engine state
     * @author Spottedleaf
     */
    @Overwrite
    public void updateChunkStatus(final ChunkPos pos) {}

    /**
     * @reason Redirect to schedule for our own logic, as well as ensure 1 radius neighbours
     * are loaded
     * Note: Our scheduling logic will discard this call if the chunk is not lit, unloaded, or not at LIGHT stage yet.
     * @author Spottedleaf
     */
    @Overwrite
    public void updateSectionStatus(final SectionPos pos, final boolean notReady) {
        this.queueTaskForSection(pos.getX(), pos.getY(), pos.getZ(), () -> {
            return this.getLightEngine().sectionChange(pos, notReady);
        });
    }

    /**
     * @reason Avoid messing with the vanilla light engine state
     * @author Spottedleaf
     */
    @Overwrite
    public void enableLightSources(final ChunkPos pos, final boolean lightEnabled) {
        // light impl does not need to do this
    }

    /**
     * @reason Light data is now attached to chunks, and this means we need to hook into chunk loading logic
     * to load the data rather than rely on this call. This call also would mess with the vanilla light engine state.
     * @author Spottedleaf
     */
    @Overwrite
    public void setData(final LightType lightType, final SectionPos pos, final NibbleArray nibbles,
                        final boolean trustEdges) {
        // load hooks inside ChunkSerializer
    }

    /**
     * @reason Avoid messing with the vanilla light engine state
     * @author Spottedleaf
     */
    @Overwrite
    public void retainData(final ChunkPos pos, final boolean retainData) {
        // light impl does not need to do this
    }

    /**
     * @reason Route to new logic to either light or just load the data
     * @author Spottedleaf
     */
    @Overwrite
    public CompletableFuture<Chunk> lightChunk(final Chunk chunk, final boolean lit) {
        final ChunkPos chunkPos = chunk.getPos();

        return CompletableFuture.supplyAsync(() -> {
            final Boolean[] emptySections = StarLightEngine.getEmptySectionsForChunk(chunk);
            if (!lit) {
                chunk.setLightPopulated(false);
                this.getLightEngine().lightChunk(chunk, emptySections);
                chunk.setLightPopulated(true);
            } else {
                this.getLightEngine().forceLoadInChunk(chunk, emptySections);
                // can't really force the chunk to be edged checked, as we need neighbouring chunks - but we don't have
                // them, so if it's not loaded then i guess we can't do edge checks. later loads of the chunk should
                // catch what we miss here.
                this.getLightEngine().checkChunkEdges(chunkPos.x, chunkPos.z);
            }

            this.chunkManager.func_219209_c(chunkPos); // releaseLightTicket
            return chunk;
        }, (runnable) -> {
            this.getLightEngine().scheduleChunkLight(chunkPos, runnable);
            this.func_215588_z_(); // tryScheduleUpdate
        }).whenComplete((final Chunk c, final Throwable throwable) -> {
            if (throwable != null) {
                LOGGER.fatal("Failed to light chunk " + chunkPos, throwable);
            }
        });
    }
}
