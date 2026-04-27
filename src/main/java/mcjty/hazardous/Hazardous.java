package mcjty.hazardous;

import mcjty.hazardous.client.ItemEmissionTooltipHandler;
import mcjty.hazardous.client.RadiationOverlayRenderer;
import mcjty.hazardous.client.ClientFxManager;
import mcjty.hazardous.client.ClientRegistration;
import mcjty.hazardous.client.SoundController;
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
        bus.addListener(Config::onConfigLoading);
        bus.addListener(Config::onConfigReloading);
        Registration.register(bus);
        bus.addListener(setup::init);
        bus.addListener(this::onDataGen);
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(CustomRegistries::onDatapackSync);
        forgeBus.addListener(EventHandlers::registerCapabilities);
        forgeBus.addGenericListener(Entity.class, EventHandlers::onEntityConstructing);
        forgeBus.addListener(EventHandlers::onPlayerCloned);
        forgeBus.addListener(EventHandlers::onPlayerLoggedOut);
        forgeBus.addListener(EventHandlers::onPlayerTickEvent);
        forgeBus.addListener(EventHandlers::commandRegister);

        if (dist.isClient()) {
            bus.addListener(ClientRegistration::onClientSetup);
            bus.addListener(ClientRegistration::registerLayerDefinitions);
            bus.addListener(ClientRegistration::addLayers);
            forgeBus.addListener(ClientFxManager::onClientTick);
            forgeBus.addListener(ClientFxManager::onCameraAngles);
            forgeBus.addListener(ClientFxManager::onRenderOverlay);
            forgeBus.addListener(RadiationOverlayRenderer::onRender);
            forgeBus.addListener(SoundController::onClientTick);
            forgeBus.addListener(SoundController::onPlaySound);
            forgeBus.addListener(SoundController::onRightClickItem);
            forgeBus.addListener(ItemEmissionTooltipHandler::onItemTooltip);
        }
    }

    private void onDataGen(GatherDataEvent event) {
        DataGen datagen = new DataGen(MODID, event);
        DataGenerators.datagen(datagen);
        datagen.generate();
    }
}
