package mcjty.hazardous.client;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.items.model.GasmaskModel;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class GasmaskRenderHelper {

    public static final ResourceLocation TEXTURE = new ResourceLocation(Hazardous.MODID, "textures/models/armor/gasmask_layer_1.png");

    private static GasmaskModel gasmaskModel;

    public static GasmaskModel getModel() {
        if (gasmaskModel == null) {
            gasmaskModel = new GasmaskModel(Minecraft.getInstance().getEntityModels().bakeLayer(GasmaskModel.LAYER_LOCATION));
        }
        return gasmaskModel;
    }
}
