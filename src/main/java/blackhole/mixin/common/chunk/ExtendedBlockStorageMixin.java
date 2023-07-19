package blackhole.mixin.common.chunk;

import blackhole.common.blockstate.ExtendedAbstractBlockState;
import blackhole.common.chunk.ExtendedExtendedBlockStorage;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExtendedBlockStorage.class)
public abstract class ExtendedBlockStorageMixin implements ExtendedExtendedBlockStorage {

    @Final
    @Shadow
    public ExtendedBlockStorage data;

    @Unique
    protected int transparentBlockCount;

    @Unique
    private final long[] knownBlockTransparencies = new long[16 * 16 * 16 * 2 / Long.SIZE]; // blocks * bits per block / bits per long

    @Unique
    private static long getKnownTransparency(final IBlockState state) {
        final int opacityIfCached = ((ExtendedAbstractBlockState)state).getOpacityIfCached();

        if (opacityIfCached == 0) {
            return ExtendedExtendedBlockStorage.BLOCK_IS_TRANSPARENT;
        }
        if (opacityIfCached == 15) {
            return ExtendedExtendedBlockStorage.BLOCK_IS_FULL_OPAQUE;
        }

        return opacityIfCached == -1 ? ExtendedExtendedBlockStorage.BLOCK_SPECIAL_TRANSPARENCY : ExtendedExtendedBlockStorage.BLOCK_UNKNOWN_TRANSPARENCY;
    }

    /* NOTE: Index is y | (x << 4) | (z << 8) */
    @Unique
    private void updateTransparencyInfo(final int blockIndex, final long transparency) {
        final int arrayIndex = (blockIndex >>> (6 - 1)); // blockIndex / (64/2)
        final int valueShift = (blockIndex & (Long.SIZE / 2 - 1)) << 1;

        long value = this.knownBlockTransparencies[arrayIndex];

        value &= ~(0b11L << valueShift);
        value |= (transparency << valueShift);

        this.knownBlockTransparencies[arrayIndex] = value;
    }

    @Unique
    private void initKnownTransparenciesData() {
        this.transparentBlockCount = 0;
        for (int y = 0; y <= 15; ++y) {
            for (int z = 0; z <= 15; ++z) {
                for (int x = 0; x <= 15; ++x) {
                    final long transparency = getKnownTransparency(this.data.get(x, y, z));
                    if (transparency == ExtendedExtendedBlockStorage.BLOCK_IS_TRANSPARENT) {
                        ++this.transparentBlockCount;
                    }
                    this.updateTransparencyInfo(y | (x << 4) | (z << 8), transparency);
                }
            }
        }
    }

    /**
     * Callback used to initialise the transparency data serverside. This only is for the server side since
     * calculateCounts is not called clientside.
     */
    @Inject(
            method = "recalculateRefCounts",
            at = @At("RETURN")
    )
    private void initKnownTransparenciesDataServerSide(final CallbackInfo ci) {
        this.initKnownTransparenciesData();
    }

    /**
     * Callback used to initialise the transparency data clientside. This is only for the client side as
     * calculateCounts is called server side, and fromPacket is only used clientside.
     */

    @SideOnly(Side.CLIENT)
    @Inject(
            method = "read",
            at = @At("RETURN")
    )
    private void initKnownTransparenciesDataClientSide(final CallbackInfo ci) {
        this.initKnownTransparenciesData();
    }

    /**
     * Callback used to update the transparency data on block update.
     */
    @Inject(
            method = "setBlockState(IIILnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;",
            at = @At("RETURN")
    )
    private void updateBlockCallback(final int x, final int y, final int z, final IBlockState state, final boolean lock,
                                     final CallbackInfoReturnable<IBlockState> cir) {
        final IBlockState oldState = cir.getReturnValue();
        final long oldTransparency = getKnownTransparency(oldState);
        final long newTransparency = getKnownTransparency(state);

        if (oldTransparency == ExtendedExtendedBlockStorage.BLOCK_IS_TRANSPARENT) {
            --this.transparentBlockCount;
        }
        if (newTransparency == ExtendedExtendedBlockStorage.BLOCK_IS_TRANSPARENT) {
            ++this.transparentBlockCount;
        }

        this.updateTransparencyInfo(y | (x << 4) | (z << 8), newTransparency);
    }

    @Override
    public final boolean hasOpaqueBlocks() {
        return this.transparentBlockCount != 4096;
    }

    @Override
    public final long getKnownTransparency(final int blockIndex) {
        // index = y | (x << 4) | (z << 8)
        final int arrayIndex = (blockIndex >>> (6 - 1)); // blockIndex / (64/2)
        final int valueShift = (blockIndex & (Long.SIZE / 2 - 1)) << 1;

        final long value = this.knownBlockTransparencies[arrayIndex];

        return (value >>> valueShift) & 0b11L;
    }


    @Override
    public final long getBitsetForColumn(final int columnX, final int columnZ) {
        // index = y | (x << 4) | (z << 8)
        final int columnIndex = (columnX << 4) | (columnZ << 8);
        final long value = this.knownBlockTransparencies[columnIndex >>> (6 - 1)]; // columnIndex / (64/2)

        final int startIndex = (columnIndex & (Long.SIZE / 2 - 1)) << 1;

        return (value >>> startIndex) & ((1L << (16 * 2)) - 1);
    }
}
