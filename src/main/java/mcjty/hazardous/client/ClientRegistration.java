package mcjty.hazardous.client;

import mcjty.hazardous.items.model.GasmaskModel;
import net.minecraftforge.client.event.EntityRenderersEvent;

public class ClientRegistration {

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GasmaskModel.LAYER_LOCATION, GasmaskModel::createBodyLayer);
    }
}
