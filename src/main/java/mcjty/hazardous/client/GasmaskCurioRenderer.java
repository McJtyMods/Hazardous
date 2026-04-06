package mcjty.hazardous.client;

import com.mojang.blaze3d.vertex.PoseStack;
import mcjty.hazardous.items.model.GasmaskModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class GasmaskCurioRenderer implements ICurioRenderer {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack,
                                                                          RenderLayerParent<T, M> renderLayerParent, MultiBufferSource renderTypeBuffer,
                                                                          int light, float limbSwing, float limbSwingAmount, float partialTicks,
                                                                          float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity wearer = slotContext.entity();
        GasmaskModel model = GasmaskRenderHelper.getModel();

        model.setAllVisible(false);
        model.head.visible = true;
        model.hat.visible = true;

        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            ((HumanoidModel<LivingEntity>) humanoidModel).copyPropertiesTo(model);
        } else {
            ICurioRenderer.followHeadRotations(wearer, model.head, model.hat);
        }

        model.renderToBuffer(
                poseStack,
                ItemRenderer.getArmorFoilBuffer(renderTypeBuffer, RenderType.armorCutoutNoCull(GasmaskRenderHelper.TEXTURE), false, stack.hasFoil()),
                light,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );
    }
}
