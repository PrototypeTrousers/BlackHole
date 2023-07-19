package blackhole.mixin.common;

import net.minecraft.world.chunk.IChunkProvider;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IChunkProvider.class)
public abstract class MixinIChunkProvider implements IExtendedChunkProvider {
}
