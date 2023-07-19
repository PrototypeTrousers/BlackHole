package blackhole.mixin.common;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

public interface IExtendedChunkProvider extends IChunkProvider {
    default World getWorld() {
        return null;
    }
}
