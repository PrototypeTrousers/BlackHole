package blackhole.common;

import blackhole.common.util.SectionPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.NibbleArray;

public interface IWorldLightListener {
    IWorldLightListener Dummy = new IWorldLightListener() {
        @Override
        public NibbleArray getData(SectionPos s) {
            return null;
        }

        @Override
        public int getLightFor(BlockPos blockPos) {
            return 0;
        }

        @Override
        public void updateSectionStatus(SectionPos pos, boolean notReady) {

        }
    };

    NibbleArray getData(SectionPos s);

    public int getLightFor(final BlockPos blockPos);

    public void updateSectionStatus(final SectionPos pos, final boolean notReady);
}
