package mcjty.hazardous;

import mcjty.hazardous.client.RadiationOverlayRenderer;
import mcjty.hazardous.data.CustomRegistries;
import mcjty.hazardous.datagen.DataGenerators;
import mcjty.hazardous.setup.Config;
import mcjty.hazardous.setup.ModSetup;
import mcjty.hazardous.setup.Registration;
import mcjty.lib.datagen.DataGen;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
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
        Dist dist = FMLEnvironment.dist;
        instance = this;

        Config.register();
        Registration.register(bus);
        bus.addListener(setup::init);
        bus.addListener(this::onDataGen);
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(CustomRegistries::onDatapackSync);
        forgeBus.addListener(EventHandlers::registerCapabilities);
        forgeBus.addGenericListener(Entity.class, EventHandlers::onEntityConstructing);
        forgeBus.addListener(EventHandlers::onPlayerCloned);
        forgeBus.addListener(EventHandlers::onPlayerTickEvent);
        forgeBus.addListener(EventHandlers::commandRegister);

        if (dist.isClient()) {
            forgeBus.addListener(RadiationOverlayRenderer::onRender);
        }
    }

    private void onDataGen(GatherDataEvent event) {
        DataGen datagen = new DataGen(MODID, event);
        DataGenerators.datagen(datagen);
        datagen.generate();
    }
}
