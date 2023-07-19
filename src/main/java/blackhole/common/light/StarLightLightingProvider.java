package blackhole.common.light;

import blackhole.common.util.SectionPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;

import javax.annotation.Nullable;

public interface StarLightLightingProvider {

    public StarLightInterface getLightEngine();

    public void clientUpdateLight(final EnumSkyBlock lightType, SectionPos pos, final @Nullable NibbleArray nibble,
                                  final boolean trustEdges);

    public void clientRemoveLightData(final ChunkPos chunkPos);

    public void clientChunkLoad(final ChunkPos pos, final Chunk chunk);

}
