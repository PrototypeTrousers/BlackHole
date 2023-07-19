package blackhole.common.world;

import blackhole.common.light.VariableBlockLightHandler;
import net.minecraft.world.chunk.Chunk;

public interface ExtendedWorld {

    // rets full chunk without blocking
    public Chunk getChunkAtImmediately(final int chunkX, final int chunkZ);

    // rets chunk at any stage, if it exists, immediately
    public Chunk getAnyChunkImmediately(final int chunkX, final int chunkZ);

    public VariableBlockLightHandler getCustomLightHandler();

    public void setCustomLightHandler(final VariableBlockLightHandler handler);

}
