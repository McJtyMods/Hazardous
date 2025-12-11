package mcjty.hazardous;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Hazardous.MODID)
public class Hazardous {
    public static final String MODID = "hazardous";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public Hazardous() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        LOGGER.info("Hazardous mod initialized");
    }
}
