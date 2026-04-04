package mcjty.hazardous.client;

import mcjty.hazardous.items.model.GasmaskModel;
import mcjty.hazardous.setup.Registration;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

import java.lang.reflect.Field;
import java.util.List;

public class ClientRegistration {
    private static final Field LAYERS_FIELD = ObfuscationReflectionHelper.findField(LivingEntityRenderer.class, "f_115291_");

    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!ModList.get().isLoaded("curios")) {
            return;
        }
        event.enqueueWork(() -> CuriosRendererRegistry.register(Registration.GASMASK.get(), GasmaskCurioRenderer::new));
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GasmaskModel.LAYER_LOCATION, GasmaskModel::createBodyLayer);
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer == null) {
                continue;
            }
            replaceArmorLayer(renderer, event, "slim".equals(skin));
        }
    }

    @SuppressWarnings("unchecked")
    private static void replaceArmorLayer(PlayerRenderer renderer, EntityRenderersEvent.AddLayers event, boolean slimModel) {
        try {
            List<Object> layers = (List<Object>) LAYERS_FIELD.get(renderer);
            for (int i = 0; i < layers.size(); i++) {
                Object layer = layers.get(i);
                if (layer instanceof net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<?, ?, ?>) {
                    layers.set(i, new HazardousPlayerArmorLayer(
                            renderer,
                            new HumanoidArmorModel<>(event.getEntityModels().bakeLayer(slimModel ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
                            new HumanoidArmorModel<>(event.getEntityModels().bakeLayer(slimModel ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)),
                            event.getContext().getModelManager()
                    ));
                    return;
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to replace player armor layer", e);
        }
    }
}
