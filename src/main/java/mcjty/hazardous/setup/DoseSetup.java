package mcjty.hazardous.setup;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.data.PlayerDoseData;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class DoseSetup {

    public static final ResourceLocation PLAYER_DOSE_KEY = new ResourceLocation(Hazardous.MODID, "playerdosedata");
    public static Capability<PlayerDoseData> PLAYER_DOSE = CapabilityManager.get(new CapabilityToken<>(){});

}
