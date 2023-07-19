package blackhole.mixin.common;

import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkProviderClient.class)
public abstract class MixinChunkProviderClient implements IExtendedChunkProvider {

    @Shadow
    World world;

    @Override
    public World getWorld() {
        return world;
    }
}
