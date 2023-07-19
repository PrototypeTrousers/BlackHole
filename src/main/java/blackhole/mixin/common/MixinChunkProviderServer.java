package blackhole.mixin.common;

import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServer implements IExtendedChunkProvider {

    @Shadow
    World world;

    @Override
    public World getWorld() {
        return world;
    }
}
