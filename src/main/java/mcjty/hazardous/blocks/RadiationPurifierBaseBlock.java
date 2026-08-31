package mcjty.hazardous.blocks;

import mcjty.hazardous.setup.Registration;
import mcjty.lib.blocks.BaseBlock;
import mcjty.lib.blocks.RotationType;
import mcjty.lib.builder.BlockBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RadiationPurifierBaseBlock extends BaseBlock {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public RadiationPurifierBaseBlock() {
        super(new BlockBuilder().properties(Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));
        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(FORMED, false));
    }

    @Override
    public RotationType getRotationType() {
        return RotationType.HORIZROTATION;
    }

    @Override
    protected Property<?>[] getProperties() {
        return new Property<?>[]{BlockStateProperties.HORIZONTAL_FACING, FORMED};
    }

    @Nonnull
    @Override
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos,
                               @Nonnull CollisionContext context) {
        return state.getValue(FORMED)
                ? RadiationPurifierShapes.base(state.getValue(BlockStateProperties.HORIZONTAL_FACING))
                : super.getShape(state, level, pos, context);
    }

    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                            @Nullable LivingEntity placer, @Nonnull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        updateAdjacentControllers(level, pos);
    }

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                @Nonnull Block neighborBlock, @Nonnull BlockPos neighborPos, boolean moving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, moving);
        updateAdjacentControllers(level, pos);
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                         @Nonnull BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (!state.is(newState.getBlock())) {
            updateAdjacentControllers(level, pos);
        }
    }

    @Nonnull
    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                 @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        BlockPos controllerPos = findController(level, pos);
        if (controllerPos == null) {
            return super.use(state, level, pos, player, hand, hit);
        }
        BlockState controllerState = level.getBlockState(controllerPos);
        BlockHitResult controllerHit = new BlockHitResult(hit.getLocation(), hit.getDirection(), controllerPos, hit.isInside());
        return controllerState.use(level, player, hand, controllerHit);
    }

    @Nullable
    private static BlockPos findController(Level level, BlockPos basePos) {
        BlockState baseState = level.getBlockState(basePos);
        if (!baseState.is(Registration.RADIATION_PURIFIER_BASE.get()) || !baseState.getValue(FORMED)) {
            return null;
        }
        BlockPos controllerPos = basePos.relative(baseState.getValue(BlockStateProperties.HORIZONTAL_FACING));
        BlockState controller = level.getBlockState(controllerPos);
        return controller.is(Registration.RADIATION_PURIFIER_CONTROLLER.get())
                && controller.getValue(RadiationPurifierControllerBlock.FORMED)
                && RadiationPurifierControllerBlock.getBasePos(controllerPos, controller).equals(basePos)
                ? controllerPos : null;
    }

    private static void updateAdjacentControllers(Level level, BlockPos basePos) {
        if (level.isClientSide) {
            return;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos controllerPos = basePos.relative(direction);
            if (level.getBlockState(controllerPos).is(Registration.RADIATION_PURIFIER_CONTROLLER.get())) {
                RadiationPurifierControllerBlock.updateFormation(level, controllerPos);
            }
        }
    }
}
