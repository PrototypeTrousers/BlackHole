package blackhole.common.util;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;

public class SectionPos extends Vec3i {
    SectionPos(int p_123162_, int p_123163_, int p_123164_) {
        super(p_123162_, p_123163_, p_123164_);
    }

    public static SectionPos of(int p_123174_, int p_123175_, int p_123176_) {
        return new SectionPos(p_123174_, p_123175_, p_123176_);
    }

    public static int x(long p_123214_) {
        return (int)(p_123214_ << 0 >> 42);
    }

    public static int y(long p_123226_) {
        return (int)(p_123226_ << 44 >> 44);
    }

    public static int z(long p_123231_) {
        return (int)(p_123231_ << 22 >> 42);
    }

    public int x() {
        return this.getX();
    }

    public int y() {
        return this.getY();
    }

    public int z() {
        return this.getZ();
    }

    public ChunkPos chunk() {
        return new ChunkPos(this.x(), this.z());
    }
}
