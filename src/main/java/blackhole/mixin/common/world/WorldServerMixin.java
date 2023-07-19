package blackhole.mixin.common.world;

import blackhole.common.light.VariableBlockLightHandler;
import blackhole.common.util.CoordinateUtils;
import blackhole.common.world.ExtendedWorld;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(WorldServer.class)
public abstract class WorldServerMixin extends World implements ISeedReader, ExtendedWorld {

    @Unique
    private VariableBlockLightHandler customBlockLightHandler;

    protected WorldServerMixin(final ISpawnWorldInfo worldInfo, final RegistryKey<World> dimension, final DimensionType dimensionType,
                               final Supplier<IProfiler> profiler, final boolean isRemote, final boolean isDebug, final long seed) {
        super(worldInfo, dimension, dimensionType, profiler, isRemote, isDebug, seed);
    }

    @Shadow
    public abstract IChunkProvider getChunkProvider();

    @Override
    public final VariableBlockLightHandler getCustomLightHandler() {
        return this.customBlockLightHandler;
    }

    @Override
    public final void setCustomLightHandler(final VariableBlockLightHandler handler) {
        this.customBlockLightHandler = handler;
    }

    @Override
    public final Chunk getChunkAtImmediately(final int chunkX, final int chunkZ) {
        final ChunkManager storage = this.getChunkProvider().chunkManager;
        final ChunkHolder holder = storage.func_219219_b(CoordinateUtils.getChunkKey(chunkX, chunkZ)); // getChunkHolderOffThread

        if (holder == null) {
            return null;
        }

        final Either<IChunk, ChunkHolder.IChunkLoadingError> either = holder.func_219301_a(ChunkStatus.FULL).getNow(null); // getChunkFuture

        return either == null ? null : (Chunk)either.left().orElse(null);
    }

    @Override
    public final Chunk getAnyChunkImmediately(final int chunkX, final int chunkZ) {
        final ChunkManager storage = this.getChunkProvider().chunkManager;
        final ChunkHolder holder = storage.func_219219_b(CoordinateUtils.getChunkKey(chunkX, chunkZ)); // getChunkHolderOffThread

        return holder == null ? null : holder.func_219287_e(); // getCurrentChunk
    }
}
