package mcjty.hazardous.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

final class RadiationPurifierShapes {

    private static final Map<Direction, VoxelShape> CONTROLLER_SHAPES = rotatedShapes(createControllerShape());
    private static final Map<Direction, VoxelShape> BASE_SHAPES = rotatedShapes(createBaseShape());

    private RadiationPurifierShapes() {
    }

    static VoxelShape controller(Direction facing) {
        return CONTROLLER_SHAPES.get(facing);
    }

    static VoxelShape base(Direction facing) {
        return BASE_SHAPES.get(facing);
    }

    /**
     * The commissioned model is authored facing east and spans x=-16 through x=16. These boxes are the
     * x=0 through x=16 portion rendered from the controller block.
     */
    private static VoxelShape createControllerShape() {
        return Shapes.or(
                Block.box(0.0, 0.0, 2.02358, 14.0, 13.0, 14.02358),
                Block.box(14.0, 4.0, 4.02358, 16.0, 12.0, 12.02358),
                Block.box(10.0, 3.0, 1.02358, 11.0, 9.0, 2.02358),
                Block.box(9.91964, 13.0, 3.97642, 10.91964, 16.0, 11.97642),
                Block.box(2.91964, 13.0, 3.97642, 3.91964, 16.0, 11.97642),
                Block.box(3.91964, 13.0, 10.97642, 9.91964, 16.0, 11.97642),
                Block.box(3.91964, 13.0, 3.97642, 9.91964, 16.0, 4.97642)
        ).optimize();
    }

    /**
     * The negative-x portion of the model, translated into the neighboring base block's local coordinates.
     */
    private static VoxelShape createBaseShape() {
        return Shapes.or(
                Block.box(2.0, 0.0, 2.02358, 16.0, 13.0, 14.02358),
                Block.box(6.08036, 13.0, 3.97642, 12.08036, 16.0, 4.97642),
                Block.box(12.08036, 13.0, 3.97642, 13.08036, 16.0, 11.97642),
                Block.box(5.08036, 13.0, 3.97642, 6.08036, 16.0, 11.97642),
                Block.box(6.08036, 13.0, 10.97642, 12.08036, 16.0, 11.97642),
                Block.box(1.0, 2.0, 11.0, 10.0, 3.0, 15.0),
                Block.box(1.0, 4.0, 11.0, 10.0, 5.0, 15.0),
                Block.box(1.0, 6.0, 11.0, 10.0, 7.0, 15.0)
        ).optimize();
    }

    private static Map<Direction, VoxelShape> rotatedShapes(VoxelShape eastShape) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            shapes.put(direction, rotateFromEast(eastShape, direction));
        }
        return shapes;
    }

    private static VoxelShape rotateFromEast(VoxelShape shape, Direction direction) {
        if (direction == Direction.EAST) {
            return shape;
        }

        VoxelShape rotated = Shapes.empty();
        for (AABB box : shape.toAabbs()) {
            VoxelShape rotatedBox = switch (direction) {
                case SOUTH -> Shapes.box(1.0 - box.maxZ, box.minY, box.minX,
                        1.0 - box.minZ, box.maxY, box.maxX);
                case WEST -> Shapes.box(1.0 - box.maxX, box.minY, 1.0 - box.maxZ,
                        1.0 - box.minX, box.maxY, 1.0 - box.minZ);
                case NORTH -> Shapes.box(box.minZ, box.minY, 1.0 - box.maxX,
                        box.maxZ, box.maxY, 1.0 - box.minX);
                default -> throw new IllegalArgumentException("Radiation Purifier cannot face " + direction);
            };
            rotated = Shapes.or(rotated, rotatedBox);
        }
        return rotated.optimize();
    }
}
