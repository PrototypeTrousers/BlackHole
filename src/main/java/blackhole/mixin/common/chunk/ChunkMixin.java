package blackhole.mixin.common.chunk;

import blackhole.common.chunk.ExtendedChunk;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Chunk.class)
public interface ChunkMixin extends ExtendedChunk {}