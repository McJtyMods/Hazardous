package mcjty.hazardous;

import mcjty.hazardous.data.CustomRegistries;
import mcjty.hazardous.datagen.DataGenerators;
import mcjty.hazardous.setup.ModSetup;
import mcjty.hazardous.setup.Registration;
import mcjty.lib.datagen.DataGen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Hazardous.MODID)
public class Hazardous {
    public static final String MODID = "hazardous";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static ModSetup setup = new ModSetup();
    public static Hazardous instance;

    public Hazardous() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        instance = this;

        Registration.register(bus);
        bus.addListener(setup::init);
        bus.addListener(this::onDataGen);
        MinecraftForge.EVENT_BUS.addListener(CustomRegistries::onDatapackSync);
        MinecraftForge.EVENT_BUS.addListener(EventHandlers::onPlayerTickEvent);
    }

    private void onDataGen(GatherDataEvent event) {
        DataGen datagen = new DataGen(MODID, event);
        DataGenerators.datagen(datagen);
        datagen.generate();
    }
}
