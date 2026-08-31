package mcjty.hazardous.blocks;

import mcjty.hazardous.setup.Config;
import mcjty.hazardous.setup.Registration;
import mcjty.lib.api.container.DefaultContainerProvider;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.tileentity.Cap;
import mcjty.lib.tileentity.CapType;
import mcjty.lib.tileentity.GenericEnergyStorage;
import mcjty.lib.tileentity.TickingTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static mcjty.lib.api.container.DefaultContainerProvider.empty;

public class RadiationPurifierControllerBlockEntity extends TickingTileEntity {

    @Cap(type = CapType.ENERGY)
    private final GenericEnergyStorage energyStorage = new GenericEnergyStorage(this, true,
            Config.RADIATION_PURIFIER_MAX_ENERGY.get(), Config.RADIATION_PURIFIER_MAX_ENERGY.get());

    @Cap(type = CapType.CONTAINER)
    private final Lazy<MenuProvider> screenHandler = Lazy.of(() -> new DefaultContainerProvider<GenericContainer>("Radiation Purifier")
            .containerSupplier(empty(Registration.RADIATION_PURIFIER_MENU, this))
            .energyHandler(() -> energyStorage)
            .setupSync(this));

    public RadiationPurifierControllerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.RADIATION_PURIFIER_CONTROLLER_BLOCK_ENTITY.get(), pos, state);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY) {
            Direction outward = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            // Unsided access is used for persistence, GUIs, and read-only integrations such as The One Probe.
            // Physical neighbors still receive the capability only from the controller's outward face.
            if (side != null && side != outward) {
                return LazyOptional.empty();
            }
        }
        return super.getCapability(capability, side);
    }

    public GenericEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    protected void tickServer() {
        if (level == null) {
            return;
        }

        if (Math.floorMod(level.getGameTime() + worldPosition.hashCode(), 20L) == 0L) {
            RadiationPurifierControllerBlock.updateFormation(level, worldPosition);
        }

        BlockState state = level.getBlockState(worldPosition);
        if (!state.is(Registration.RADIATION_PURIFIER_CONTROLLER.get())) {
            return;
        }

        boolean formed = state.getValue(RadiationPurifierControllerBlock.FORMED);
        long energyPerTick = Config.RADIATION_PURIFIER_ENERGY_PER_TICK.get();
        boolean active = formed && energyStorage.getEnergy() >= energyPerTick;
        if (active && energyPerTick > 0) {
            energyStorage.consumeEnergy(energyPerTick);
        }
        setActive(state, active);

        if (active) {
            int interval = Config.RADIATION_PURIFIER_PLAYER_SCAN_INTERVAL.get();
            if (Math.floorMod(level.getGameTime() + worldPosition.hashCode(), (long) interval) == 0L) {
                grantImmunity(state, interval);
            }
        }
    }

    private void setActive(BlockState state, boolean active) {
        if (state.getValue(RadiationPurifierControllerBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(RadiationPurifierControllerBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    private void grantImmunity(BlockState state, int interval) {
        int radius = Config.RADIATION_PURIFIER_RADIUS.get();
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Vec3 center = Vec3.atCenterOf(worldPosition).relative(facing.getOpposite(), 0.5);
        AABB searchBox = new AABB(center, center).inflate(radius);
        double radiusSquared = (double) radius * radius;
        // Keep the effect above Minecraft's ten-second HUD warning range between scans so its icon does not flash.
        int effectDuration = Math.max(240, interval + 220);

        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, searchBox,
                player -> player.distanceToSqr(center) <= radiusSquared)) {
            player.addEffect(new MobEffectInstance(Registration.HAZARD_IMMUNITY.get(), effectDuration,
                    0, false, false, true));
        }
    }
}
