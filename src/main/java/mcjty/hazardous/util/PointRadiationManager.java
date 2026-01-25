package mcjty.hazardous.util;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Cache for keeping track of radiation sources and blockers
 */
public class PointRadiationManager {

    private final ServerLevel level;
    private final Long2ObjectOpenHashMap<LongOpenHashSet> sourcesBySection = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<QuadTree> blockerTrees = new Long2ObjectOpenHashMap<>();
    private final Long2FloatOpenHashMap blockers = new Long2FloatOpenHashMap();
    private final Long2IntOpenHashMap blockerCounts = new Long2IntOpenHashMap();

    private boolean sourceBoundsDirty = false;
    private int minSourceSectionX;
    private int maxSourceSectionX;
    private int minSourceSectionY;
    private int maxSourceSectionY;
    private int minSourceSectionZ;
    private int maxSourceSectionZ;

    public PointRadiationManager(ServerLevel level) {
        this.level = level;
    }

    /**
     * Add a blocker. Radiation blocked by this is reduced with factor
     */
    public void addBlocker(BlockPos pos, double factor) {
        long key = pos.asLong();
        float factorValue = (float) factor;
        if (blockers.containsKey(key)) {
            float old = blockers.get(key);
            if (Math.abs(old - factorValue) < 0.0001f) {
                return;
            }
            blockers.put(key, factorValue);
            QuadTree tree = getOrCreateBlockerTree(pos);
            tree.addBlocker(pos, factorValue);
            return;
        }
        blockers.put(key, factorValue);
        QuadTree tree = getOrCreateBlockerTree(pos);
        tree.addBlocker(pos, factorValue);
        long sectionKey = SectionPos.asLong(SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getY()),
                SectionPos.blockToSectionCoord(pos.getZ()));
        blockerCounts.put(sectionKey, blockerCounts.get(sectionKey) + 1);
    }

    /**
     * Remove a blocker
     */
    public void removeBlocker(BlockPos pos) {
        long key = pos.asLong();
        if (!blockers.containsKey(key)) {
            return;
        }
        blockers.remove(key);
        long sectionKey = SectionPos.asLong(SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getY()),
                SectionPos.blockToSectionCoord(pos.getZ()));
        int count = blockerCounts.get(sectionKey) - 1;
        if (count <= 0) {
            blockerCounts.remove(sectionKey);
            blockerTrees.remove(sectionKey);
        } else {
            blockerCounts.put(sectionKey, count);
            QuadTree tree = blockerTrees.get(sectionKey);
            if (tree != null) {
                tree.addBlocker(pos, 1.0f);
            }
        }
    }

    /**
     * Add a source of radiation
     */
    public void addSource(BlockPos source) {
        long sourceKey = source.asLong();
        int sx = SectionPos.blockToSectionCoord(source.getX());
        int sy = SectionPos.blockToSectionCoord(source.getY());
        int sz = SectionPos.blockToSectionCoord(source.getZ());
        long sectionKey = SectionPos.asLong(sx, sy, sz);
        LongOpenHashSet sources = sourcesBySection.get(sectionKey);
        if (sources == null) {
            sources = new LongOpenHashSet();
            sourcesBySection.put(sectionKey, sources);
        }
        if (!sources.add(sourceKey)) {
            return;
        }
        if (sourcesBySection.size() == 1 && sources.size() == 1) {
            minSourceSectionX = maxSourceSectionX = sx;
            minSourceSectionY = maxSourceSectionY = sy;
            minSourceSectionZ = maxSourceSectionZ = sz;
            sourceBoundsDirty = false;
        } else {
            if (!sourceBoundsDirty) {
                minSourceSectionX = Math.min(minSourceSectionX, sx);
                maxSourceSectionX = Math.max(maxSourceSectionX, sx);
                minSourceSectionY = Math.min(minSourceSectionY, sy);
                maxSourceSectionY = Math.max(maxSourceSectionY, sy);
                minSourceSectionZ = Math.min(minSourceSectionZ, sz);
                maxSourceSectionZ = Math.max(maxSourceSectionZ, sz);
            }
        }
    }

    /**
     * Remove a source of radiation
     */
    public void removeSource(BlockPos source) {
        int sx = SectionPos.blockToSectionCoord(source.getX());
        int sy = SectionPos.blockToSectionCoord(source.getY());
        int sz = SectionPos.blockToSectionCoord(source.getZ());
        long sectionKey = SectionPos.asLong(sx, sy, sz);
        LongOpenHashSet sources = sourcesBySection.get(sectionKey);
        if (sources == null) {
            return;
        }
        if (!sources.remove(source.asLong())) {
            return;
        }
        if (sources.isEmpty()) {
            sourcesBySection.remove(sectionKey);
            if (!sourceBoundsDirty) {
                if (sx == minSourceSectionX || sx == maxSourceSectionX
                        || sy == minSourceSectionY || sy == maxSourceSectionY
                        || sz == minSourceSectionZ || sz == maxSourceSectionZ) {
                    sourceBoundsDirty = true;
                }
            }
        }
    }

    /**
     * Get the distance between the closest radiation source and the given position reduced
     * by blockers
     */
    public double getRadiationValue(BlockPos pos) {
        if (sourcesBySection.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        if (!ensureSourceBounds()) {
            return Double.POSITIVE_INFINITY;
        }

        int sx = SectionPos.blockToSectionCoord(pos.getX());
        int sy = SectionPos.blockToSectionCoord(pos.getY());
        int sz = SectionPos.blockToSectionCoord(pos.getZ());
        int maxRadius = Math.max(
                Math.max(Math.abs(sx - minSourceSectionX), Math.abs(sx - maxSourceSectionX)),
                Math.max(
                        Math.max(Math.abs(sy - minSourceSectionY), Math.abs(sy - maxSourceSectionY)),
                        Math.max(Math.abs(sz - minSourceSectionZ), Math.abs(sz - maxSourceSectionZ))
                )
        );

        int localX = pos.getX() & 15;
        int localY = pos.getY() & 15;
        int localZ = pos.getZ() & 15;

        double bestSq = Double.POSITIVE_INFINITY;
        long bestSource = Long.MIN_VALUE;

        for (int r = 0; r <= maxRadius; r++) {
            if (r == 0) {
                bestSource = scanSourcesInSection(pos, sx, sy, sz, bestSq);
                if (bestSource != Long.MIN_VALUE) {
                    bestSq = distanceSq(pos, bestSource);
                }
            } else {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dy = -r; dy <= r; dy++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (Math.abs(dx) != r && Math.abs(dy) != r && Math.abs(dz) != r) {
                                continue;
                            }
                            long found = scanSourcesInSection(pos, sx + dx, sy + dy, sz + dz, bestSq);
                            if (found != Long.MIN_VALUE) {
                                double distSq = distanceSq(pos, found);
                                if (distSq < bestSq) {
                                    bestSq = distSq;
                                    bestSource = found;
                                }
                            }
                        }
                    }
                }
            }

            if (bestSq == 0.0) {
                break;
            }
            if (bestSq != Double.POSITIVE_INFINITY) {
                int distToOutsideX = Math.min(r * 16 + localX + 1, (r + 1) * 16 - localX);
                int distToOutsideY = Math.min(r * 16 + localY + 1, (r + 1) * 16 - localY);
                int distToOutsideZ = Math.min(r * 16 + localZ + 1, (r + 1) * 16 - localZ);
                int minOutside = Math.min(distToOutsideX, Math.min(distToOutsideY, distToOutsideZ));
                if (bestSq <= (double) minOutside * (double) minOutside) {
                    break;
                }
            }
        }

        if (bestSource == Long.MIN_VALUE) {
            return Double.POSITIVE_INFINITY;
        }

        double distance = Math.sqrt(bestSq);
        double factor = getBlockerFactor(BlockPos.of(bestSource), pos);
        if (factor <= 0.000001) {
            return Double.POSITIVE_INFINITY;
        }
        return distance / factor;
    }

    private QuadTree getOrCreateBlockerTree(BlockPos pos) {
        int sx = SectionPos.blockToSectionCoord(pos.getX());
        int sy = SectionPos.blockToSectionCoord(pos.getY());
        int sz = SectionPos.blockToSectionCoord(pos.getZ());
        long sectionKey = SectionPos.asLong(sx, sy, sz);
        QuadTree tree = blockerTrees.get(sectionKey);
        if (tree == null) {
            int minX = sx << 4;
            int minY = sy << 4;
            int minZ = sz << 4;
            tree = new QuadTree(minX, minY, minZ, minX + 16, minY + 16, minZ + 16);
            blockerTrees.put(sectionKey, tree);
        }
        return tree;
    }

    private boolean ensureSourceBounds() {
        if (sourcesBySection.isEmpty()) {
            return false;
        }
        if (!sourceBoundsDirty) {
            return true;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (long sectionKey : sourcesBySection.keySet()) {
            int sx = SectionPos.x(sectionKey);
            int sy = SectionPos.y(sectionKey);
            int sz = SectionPos.z(sectionKey);
            minX = Math.min(minX, sx);
            maxX = Math.max(maxX, sx);
            minY = Math.min(minY, sy);
            maxY = Math.max(maxY, sy);
            minZ = Math.min(minZ, sz);
            maxZ = Math.max(maxZ, sz);
        }
        minSourceSectionX = minX;
        maxSourceSectionX = maxX;
        minSourceSectionY = minY;
        maxSourceSectionY = maxY;
        minSourceSectionZ = minZ;
        maxSourceSectionZ = maxZ;
        sourceBoundsDirty = false;
        return true;
    }

    private long scanSourcesInSection(BlockPos pos, int sx, int sy, int sz, double bestSq) {
        long sectionKey = SectionPos.asLong(sx, sy, sz);
        LongOpenHashSet sources = sourcesBySection.get(sectionKey);
        if (sources == null || sources.isEmpty()) {
            return Long.MIN_VALUE;
        }
        long bestSource = Long.MIN_VALUE;
        for (long sourceKey : sources) {
            double distSq = distanceSq(pos, sourceKey);
            if (distSq < bestSq) {
                bestSq = distSq;
                bestSource = sourceKey;
            }
        }
        return bestSource;
    }

    private double distanceSq(BlockPos pos, long sourceKey) {
        int sx = BlockPos.getX(sourceKey);
        int sy = BlockPos.getY(sourceKey);
        int sz = BlockPos.getZ(sourceKey);
        double dx = sx - pos.getX();
        double dy = sy - pos.getY();
        double dz = sz - pos.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private double getBlockerFactor(BlockPos source, BlockPos target) {
        if (blockerTrees.isEmpty()) {
            return 1.0;
        }
        Vec3 start = new Vec3(source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5);
        Vec3 end = new Vec3(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        Vec3 endHead = new Vec3(target.getX() + 0.5, target.getY() + 1.1, target.getZ() + 0.5);

        double factorBody = factorForRay(start, end);
        if (factorBody <= 0.000001) {
            return 0.0;
        }
        double factorHead = factorForRay(start, endHead);
        return Math.max(factorBody, factorHead);
    }

    private double factorForRay(Vec3 start, Vec3 end) {
        double sx = start.x / 16.0;
        double sy = start.y / 16.0;
        double sz = start.z / 16.0;
        double ex = end.x / 16.0;
        double ey = end.y / 16.0;
        double ez = end.z / 16.0;

        int x = Mth.floor(sx);
        int y = Mth.floor(sy);
        int z = Mth.floor(sz);
        int endX = Mth.floor(ex);
        int endY = Mth.floor(ey);
        int endZ = Mth.floor(ez);

        double dx = ex - sx;
        double dy = ey - sy;
        double dz = ez - sz;

        int stepX = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
        int stepY = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
        int stepZ = dz > 0 ? 1 : (dz < 0 ? -1 : 0);

        double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dx);
        double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dy);
        double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dz);

        double tMaxX = stepX == 0 ? Double.POSITIVE_INFINITY
                : (stepX > 0 ? (Math.floor(sx) + 1.0 - sx) : (sx - Math.floor(sx))) * tDeltaX;
        double tMaxY = stepY == 0 ? Double.POSITIVE_INFINITY
                : (stepY > 0 ? (Math.floor(sy) + 1.0 - sy) : (sy - Math.floor(sy))) * tDeltaY;
        double tMaxZ = stepZ == 0 ? Double.POSITIVE_INFINITY
                : (stepZ > 0 ? (Math.floor(sz) + 1.0 - sz) : (sz - Math.floor(sz))) * tDeltaZ;

        double factor = 1.0;
        int maxSteps = 1 + Math.abs(endX - x) + Math.abs(endY - y) + Math.abs(endZ - z);
        for (int i = 0; i <= maxSteps; i++) {
            QuadTree tree = blockerTrees.get(SectionPos.asLong(x, y, z));
            if (tree != null) {
                factor *= tree.factor(start.x, start.y, start.z, end.x, end.y, end.z);
                if (factor <= 0.000001) {
                    return 0.0;
                }
            }
            if (x == endX && y == endY && z == endZ) {
                break;
            }
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    tMaxX += tDeltaX;
                } else {
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    tMaxY += tDeltaY;
                } else {
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                }
            }
        }
        return factor;
    }
}
