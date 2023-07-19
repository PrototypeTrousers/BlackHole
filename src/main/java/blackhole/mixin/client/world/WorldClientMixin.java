package blackhole.mixin.client.world;

import blackhole.common.light.VariableBlockLightHandler;
import blackhole.common.world.ExtendedWorld;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Supplier;

@Mixin(WorldClient.class)
public abstract class WorldClientMixin extends World implements ExtendedWorld {

    @Shadow
    public abstract IChunkProvider getChunkProvider();

    @Unique
    private VariableBlockLightHandler customBlockLightHandler;

    protected WorldClientMixin(final ISpawnWorldInfo worldInfo, final RegistryKey<World> dimension, final DimensionType dimensionType,
                               final Supplier<IProfiler> profiler, final boolean isRemote, final boolean isDebug, final long seed) {
        super(worldInfo, dimension, dimensionType, profiler, isRemote, isDebug, seed);
    }

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
        return this.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
    }

    @Override
    public final Chunk getAnyChunkImmediately(int chunkX, int chunkZ) {
        return this.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
    }
}
