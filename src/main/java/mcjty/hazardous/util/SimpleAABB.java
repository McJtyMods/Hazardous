package mcjty.hazardous.util;

import net.minecraft.core.BlockPos;

/**
 * Created by McJty
 * <p>
 * Simple AABB for the QuadTree, minimal bloat
 */
public record SimpleAABB(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public static SimpleAABB get(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return new SimpleAABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Returns if the supplied Coordinate is completely inside the bounding box
     */
    public boolean isVecInside(BlockPos c) {
        return c.getX() >= this.minX && c.getX() < this.maxX && (c.getY() >= this.minY && c.getY() < this.maxY && c.getZ() >= this.minZ && c.getZ() < this.maxZ);
    }

    /**
     * Returns a copy of the bounding box.
     */
    public SimpleAABB copy() {
        return get(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }


    @Override
    public String toString() {
        return "box[" + this.minX + ", " + this.minY + ", " + this.minZ + " -> " + this.maxX + ", " + this.maxY + ", " + this.maxZ + "]";
    }

}
