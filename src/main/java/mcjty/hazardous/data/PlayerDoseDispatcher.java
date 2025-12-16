package mcjty.hazardous.data;

import mcjty.hazardous.setup.DoseSetup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayerDoseDispatcher implements ICapabilityProvider, INBTSerializable<Tag> {

    public static LazyOptional<PlayerDoseData> getPlayerDose(Player player) {
        return player.getCapability(DoseSetup.PLAYER_DOSE);
    }

    private final PlayerDoseData data = new PlayerDoseData();
    private final LazyOptional<PlayerDoseData> cap = LazyOptional.of(() -> data);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability) {
        if (capability == DoseSetup.PLAYER_DOSE) {
            return cap.cast();
        }
        return LazyOptional.empty();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side) {
        return getCapability(capability);
    }

    @Override
    public Tag serializeNBT() {
        return data.saveNBTData();
    }

    @Override
    public void deserializeNBT(Tag nbt) {
        data.loadNBTData(nbt);
    }
}
