package mcjty.hazardous.blocks;

import mcjty.hazardous.setup.Registration;
import mcjty.lib.blocks.BaseBlock;
import mcjty.lib.blocks.RotationType;
import mcjty.lib.builder.BlockBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RadiationPurifierControllerBlock extends BaseBlock {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

    public RadiationPurifierControllerBlock() {
        super(new BlockBuilder()
                .properties(Properties.copy(Blocks.IRON_BLOCK).noOcclusion())
                .tileEntitySupplier(RadiationPurifierControllerBlockEntity::new));
        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(FORMED, false)
                .setValue(ACTIVE, false));
    }

    @Override
    public RotationType getRotationType() {
        return RotationType.HORIZROTATION;
    }

    @Override
    protected Property<?>[] getProperties() {
        return new Property<?>[]{BlockStateProperties.HORIZONTAL_FACING, FORMED, ACTIVE};
    }

    @Nonnull
    @Override
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos,
                               @Nonnull CollisionContext context) {
        return state.getValue(FORMED)
                ? RadiationPurifierShapes.controller(state.getValue(BlockStateProperties.HORIZONTAL_FACING))
                : super.getShape(state, level, pos, context);
    }

    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                            @Nullable LivingEntity placer, @Nonnull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        updateFormation(level, pos);
    }

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                @Nonnull Block neighborBlock, @Nonnull BlockPos neighborPos, boolean moving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, moving);
        updateFormation(level, pos);
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                         @Nonnull BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            unformOwnedBase(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    public static BlockPos getBasePos(BlockPos controllerPos, BlockState controllerState) {
        return controllerPos.relative(controllerState.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
    }

    public static void updateFormation(Level level, BlockPos controllerPos) {
        if (level.isClientSide) {
            return;
        }
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!controllerState.is(Registration.RADIATION_PURIFIER_CONTROLLER.get())) {
            return;
        }

        Direction baseDirection = findBaseDirection(level, controllerPos, controllerState);
        boolean formed = baseDirection != null;
        Direction facing = formed
                ? baseDirection.getOpposite()
                : controllerState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos expectedBasePos = formed ? controllerPos.relative(baseDirection) : null;

        // Claim the base first so neighbor updates cannot let a second controller claim it reentrantly.
        if (formed) {
            BlockState baseState = level.getBlockState(expectedBasePos);
            BlockState wantedBase = baseState
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                    .setValue(RadiationPurifierBaseBlock.FORMED, true);
            if (wantedBase != baseState) {
                level.setBlock(expectedBasePos, wantedBase, Block.UPDATE_ALL);
            }
        }

        controllerState = level.getBlockState(controllerPos);
        BlockState wantedController = controllerState
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(FORMED, formed)
                .setValue(ACTIVE, formed && controllerState.getValue(ACTIVE));
        if (wantedController != controllerState) {
            level.setBlock(controllerPos, wantedController, Block.UPDATE_ALL);
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidatePos = controllerPos.relative(direction);
            BlockState candidate = level.getBlockState(candidatePos);
            if (!candidate.is(Registration.RADIATION_PURIFIER_BASE.get()) || !candidate.getValue(RadiationPurifierBaseBlock.FORMED)) {
                continue;
            }
            Direction candidateFacing = candidate.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (candidatePos.relative(candidateFacing).equals(controllerPos)
                    && (expectedBasePos == null || !candidatePos.equals(expectedBasePos))) {
                level.setBlock(candidatePos, candidate.setValue(RadiationPurifierBaseBlock.FORMED, false), Block.UPDATE_ALL);
            }
        }
    }

    @Nullable
    private static Direction findBaseDirection(Level level, BlockPos controllerPos, BlockState controllerState) {
        // Keep an existing claim stable, even if something attempted to rotate the controller independently.
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidatePos = controllerPos.relative(direction);
            BlockState candidate = level.getBlockState(candidatePos);
            if (candidate.is(Registration.RADIATION_PURIFIER_BASE.get())
                    && candidate.getValue(RadiationPurifierBaseBlock.FORMED)
                    && candidatePos.relative(candidate.getValue(BlockStateProperties.HORIZONTAL_FACING)).equals(controllerPos)) {
                return direction;
            }
        }

        // Prefer the side implied by the placement orientation when it contains an available base.
        Direction preferredDirection = controllerState.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        if (isAvailableBase(level, controllerPos, preferredDirection)) {
            return preferredDirection;
        }

        // Otherwise accept a base on any horizontal side and orient both halves to match.
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction != preferredDirection && isAvailableBase(level, controllerPos, direction)) {
                return direction;
            }
        }
        return null;
    }

    private static boolean isAvailableBase(Level level, BlockPos controllerPos, Direction direction) {
        BlockPos basePos = controllerPos.relative(direction);
        BlockState baseState = level.getBlockState(basePos);
        return baseState.is(Registration.RADIATION_PURIFIER_BASE.get())
                && (!baseState.getValue(RadiationPurifierBaseBlock.FORMED)
                || basePos.relative(baseState.getValue(BlockStateProperties.HORIZONTAL_FACING)).equals(controllerPos)
                || !hasValidControllerClaim(level, basePos, baseState));
    }

    private static void unformOwnedBase(Level level, BlockPos controllerPos, BlockState controllerState) {
        if (level.isClientSide || !controllerState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return;
        }
        BlockPos basePos = getBasePos(controllerPos, controllerState);
        BlockState baseState = level.getBlockState(basePos);
        if (baseState.is(Registration.RADIATION_PURIFIER_BASE.get())
                && baseState.getValue(RadiationPurifierBaseBlock.FORMED)
                && basePos.relative(baseState.getValue(BlockStateProperties.HORIZONTAL_FACING)).equals(controllerPos)) {
            level.setBlock(basePos, baseState.setValue(RadiationPurifierBaseBlock.FORMED, false), Block.UPDATE_ALL);
        }
    }

    private static boolean hasValidControllerClaim(Level level, BlockPos basePos, BlockState baseState) {
        BlockPos claimedControllerPos = basePos.relative(baseState.getValue(BlockStateProperties.HORIZONTAL_FACING));
        BlockState claimedController = level.getBlockState(claimedControllerPos);
        return claimedController.is(Registration.RADIATION_PURIFIER_CONTROLLER.get())
                && (!claimedController.getValue(FORMED)
                || getBasePos(claimedControllerPos, claimedController).equals(basePos));
    }
}
