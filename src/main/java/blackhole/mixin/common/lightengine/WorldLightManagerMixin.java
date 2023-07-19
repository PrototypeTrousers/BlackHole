package blackhole.mixin.common.lightengine;

import blackhole.common.IWorldLightListener;
import blackhole.common.chunk.ExtendedChunk;
import blackhole.common.light.SWMRNibbleArray;
import blackhole.common.light.StarLightEngine;
import blackhole.common.light.StarLightInterface;
import blackhole.common.light.StarLightLightingProvider;
import blackhole.common.util.CoordinateUtils;
import blackhole.common.util.SectionPos;
import blackhole.common.util.WorldUtil;
import blackhole.mixin.common.IExtendedChunkProvider;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

import static net.minecraft.world.EnumSkyBlock.BLOCK;
import static net.minecraft.world.EnumSkyBlock.SKY;

@Mixin(WorldLightManager.class)
public abstract class WorldLightManagerMixin implements ILightListener, StarLightLightingProvider {

    @Shadow
    @Nullable
    private LightEngine<?, ?> blockLight;

    @Shadow
    @Nullable
    private LightEngine<?, ?> skyLight;

    @Unique
    protected StarLightInterface lightEngine;

    @Override
    public final StarLightInterface getLightEngine() {
        return this.lightEngine;
    }

    /**
     *
     * TODO since this is a constructor inject, check on update for new constructors
     */
    @Inject(
            method = "<init>", at = @At("TAIL")
    )
    public void construct(final IExtendedChunkProvider provider, final boolean hasBlockLight, final boolean hasSkyLight,
                          final CallbackInfo ci) {
        this.lightEngine = new StarLightInterface(provider, hasSkyLight, hasBlockLight);
        // intentionally destroy mods hooking into old light engine state
        this.blockLight = null;
        this.skyLight = null;
    }

    /**
     * @reason Route to new light engine
     * @author Spottedleaf
     */
    @Overwrite
    public void checkBlock(final BlockPos pos) {
        this.lightEngine.blockChange(pos.toImmutable());
    }

    /**
     * @reason Avoid messing with vanilla light engine state
     * @author Spottedleaf
     */
    @Overwrite
    public void onBlockEmissionIncrease(final BlockPos pos, final int level) {
        // this light engine only reads levels from blocks, so this is a no-op
    }

    /**
     * @reason Route to new light engine
     * @author Spottedleaf
     */
    @Overwrite
    public boolean hasLightWork() {
        // route to new light engine
        return this.lightEngine.hasUpdates();
    }

    /**
     * @reason Hook into new light engine for light updates
     * @author Spottedleaf
     */
    @Overwrite
    public int tick(final int maxUpdateCount, final boolean doSkylight, final boolean skipEdgeLightPropagation) {
        // replace impl
        final boolean hadUpdates = this.hasLightWork();
        this.lightEngine.propagateChanges();
        return hadUpdates ? 1 : 0;
    }

    /**
     * @reason New light engine hook for handling empty section changes
     * @author Spottedleaf
     */
    @Overwrite
    public void updateSectionStatus(final SectionPos pos, final boolean notReady) {
        this.lightEngine.sectionChange(pos, notReady);
    }

    /**
     * @reason Avoid messing with the vanilla light engine state
     * @author Spottedleaf
     */
    @Overwrite
    public void enableLightSources(final ChunkPos pos, final boolean lightEnabled) {

    }

    /**
     * @reason Replace light views with our own that hook into the new light engine instead of vanilla's
     * @author Spottedleaf
     */
    @Overwrite
    public IWorldLightListener getLightEngine(final LightType lightType) {
        return lightType == LightType.BLOCK ? this.lightEngine.getBlockReader() : this.lightEngine.getSkyReader();
    }

    /**
     * @reason Avoid messing with the vanilla light engine state
     * @author Spottedleaf
     */
    @Overwrite
    public void setData(final LightType lightType, final SectionPos pos, final NibbleArray nibble,
                        final boolean trustEdges) {
    }

    /**
     * @reason Avoid messing with the vanilla light engine state
     * @author Spottedleaf
     */
    @Overwrite
    public void retainData(final ChunkPos pos, final boolean retainData) {
        // not used by new light impl
    }

    /**
     * @reason Need to use our own hooks for retrieving light data
     * @author Spottedleaf
     */
    @Overwrite
    public int getLightSubtracted(final BlockPos pos, final int ambientDarkness) {
        // need to use new light hooks for this
        final int sky = this.lightEngine.getSkyReader().getLightFor(pos) - ambientDarkness;
        final int block = this.lightEngine.getBlockReader().getLightFor(pos);
        return Math.max(sky, block);
    }

    @Unique
    protected final Long2ObjectOpenHashMap<SWMRNibbleArray[]> blockLightMap = new Long2ObjectOpenHashMap<>();

    @Unique
    protected final Long2ObjectOpenHashMap<SWMRNibbleArray[]> skyLightMap = new Long2ObjectOpenHashMap<>();

    @Override
    public void clientUpdateLight(final LightType lightType, SectionPos pos, final @Nullable NibbleArray nibble,
                                  final boolean trustEdges) {
        if (((Object)this).getClass() != WorldLightManager.class) {
            throw new IllegalStateException("This hook is for the CLIENT ONLY");
        }
        // data storage changed with new light impl
        final Chunk chunk = this.getLightEngine().getAnyChunkNow(pos.getX(), pos.getZ());
        switch (lightType) {
            case BLOCK: {
                final SWMRNibbleArray[] blockNibbles = this.blockLightMap.computeIfAbsent(CoordinateUtils.getChunkKey(pos), (final long keyInMap) -> {
                    return StarLightEngine.getFilledEmptyLight(this.lightEngine.getWorld());
                });

                blockNibbles[pos.getY() - WorldUtil.getMinLightSection(this.lightEngine.getWorld())] = SWMRNibbleArray.fromVanilla(nibble);

                if (chunk != null) {
                    ((ExtendedChunk)chunk).setBlockNibbles(blockNibbles);
                    this.lightEngine.getLightAccess().markLightChanged(LightType.BLOCK, pos);
                }
                break;
            }
            case SKY: {
                final SWMRNibbleArray[] skyNibbles = this.skyLightMap.computeIfAbsent(CoordinateUtils.getChunkKey(pos), (final long keyInMap) -> {
                    return StarLightEngine.getFilledEmptyLight(this.lightEngine.getWorld());
                });

                skyNibbles[pos.getY() - WorldUtil.getMinLightSection(this.lightEngine.getWorld())] = SWMRNibbleArray.fromVanilla(nibble);

                if (chunk != null) {
                    ((ExtendedChunk)chunk).setSkyNibbles(skyNibbles);
                    this.lightEngine.getLightAccess().markLightChanged(LightType.SKY, pos);
                }
                break;
            }
        }
    }

    @Override
    public void clientRemoveLightData(final ChunkPos chunkPos) {
        if (((Object)this).getClass() != WorldLightManager.class) {
            throw new IllegalStateException("This hook is for the CLIENT ONLY");
        }
        this.blockLightMap.remove(CoordinateUtils.getChunkKey(chunkPos));
        this.skyLightMap.remove(CoordinateUtils.getChunkKey(chunkPos));
    }

    @Override
    public void clientChunkLoad(final ChunkPos pos, final Chunk chunk) {
        if (((Object)this).getClass() != WorldLightManager.class) {
            throw new IllegalStateException("This hook is for the CLIENT ONLY");
        }
        final long key = CoordinateUtils.getChunkKey(pos);
        final SWMRNibbleArray[] blockNibbles = this.blockLightMap.get(key);
        final SWMRNibbleArray[] skyNibbles = this.skyLightMap.get(key);
        if (blockNibbles != null) {
            ((ExtendedChunk)chunk).setBlockNibbles(blockNibbles);
        }
        if (skyNibbles != null) {
            ((ExtendedChunk)chunk).setSkyNibbles(skyNibbles);
        }
    }
}
