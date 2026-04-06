package mcjty.hazardous.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;

public class HazardousPlayerArmorLayer extends HumanoidArmorLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>, HumanoidModel<AbstractClientPlayer>> {

    private final HumanoidModel<AbstractClientPlayer> innerModel;
    private final HumanoidModel<AbstractClientPlayer> outerModel;
    private final TextureAtlas armorTrimAtlas;

    public HazardousPlayerArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer,
                                     HumanoidModel<AbstractClientPlayer> innerModel,
                                     HumanoidModel<AbstractClientPlayer> outerModel,
                                     ModelManager modelManager) {
        super(renderer, innerModel, outerModel, modelManager);
        this.innerModel = innerModel;
        this.outerModel = outerModel;
        this.armorTrimAtlas = modelManager.getAtlas(Sheets.ARMOR_TRIMS_SHEET);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        renderArmorPiece(poseStack, buffer, player, EquipmentSlot.CHEST, packedLight, getArmorModel(EquipmentSlot.CHEST));
        renderArmorPiece(poseStack, buffer, player, EquipmentSlot.LEGS, packedLight, getArmorModel(EquipmentSlot.LEGS));
        renderArmorPiece(poseStack, buffer, player, EquipmentSlot.FEET, packedLight, getArmorModel(EquipmentSlot.FEET));
        if (!CuriosHelmetRenderOverride.shouldHideDefaultHelmet(player, EquipmentSlot.HEAD)) {
            renderArmorPiece(poseStack, buffer, player, EquipmentSlot.HEAD, packedLight, getArmorModel(EquipmentSlot.HEAD));
        }
    }

    private void renderArmorPiece(PoseStack poseStack, MultiBufferSource buffer, AbstractClientPlayer player, EquipmentSlot slot,
                                  int packedLight, HumanoidModel<AbstractClientPlayer> model) {
        ItemStack itemStack = player.getItemBySlot(slot);
        Item item = itemStack.getItem();
        if (!(item instanceof ArmorItem armorItem) || armorItem.getEquipmentSlot() != slot) {
            return;
        }

        this.getParentModel().copyPropertiesTo(model);
        this.setPartVisibility(model, slot);
        Model armorModel = this.getArmorModelHook(player, itemStack, slot, model);
        boolean innerTexture = usesInnerModel(slot);

        if (armorItem instanceof net.minecraft.world.item.DyeableLeatherItem dyeableLeatherItem) {
            int color = dyeableLeatherItem.getColor(itemStack);
            float red = (float) (color >> 16 & 255) / 255.0F;
            float green = (float) (color >> 8 & 255) / 255.0F;
            float blue = (float) (color & 255) / 255.0F;
            renderModel(poseStack, buffer, packedLight, armorModel, itemStack, innerTexture, red, green, blue, this.getArmorResource(player, itemStack, slot, null));
            renderModel(poseStack, buffer, packedLight, armorModel, itemStack, innerTexture, 1.0F, 1.0F, 1.0F, this.getArmorResource(player, itemStack, slot, "overlay"));
        } else {
            renderModel(poseStack, buffer, packedLight, armorModel, itemStack, innerTexture, 1.0F, 1.0F, 1.0F, this.getArmorResource(player, itemStack, slot, null));
        }

        ArmorTrim.getTrim(player.level().registryAccess(), itemStack).ifPresent(trim ->
                renderTrim(armorItem, poseStack, buffer, packedLight, trim, armorModel, innerTexture));
    }

    private void renderModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Model model, ItemStack itemStack,
                             boolean noEntity, float red, float green, float blue, net.minecraft.resources.ResourceLocation texture) {
        VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(texture), noEntity, itemStack.hasFoil());
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1.0F);
    }

    private void renderTrim(ArmorItem armorItem, PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                            ArmorTrim trim, Model model, boolean innerTexture) {
        TextureAtlasSprite texture = this.armorTrimAtlas.getSprite(innerTexture ? trim.innerTexture(armorItem.getMaterial()) : trim.outerTexture(armorItem.getMaterial()));
        VertexConsumer vertexConsumer = texture.wrap(buffer.getBuffer(Sheets.armorTrimsSheet()));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private HumanoidModel<AbstractClientPlayer> getArmorModel(EquipmentSlot slot) {
        return usesInnerModel(slot) ? this.innerModel : this.outerModel;
    }

    private boolean usesInnerModel(EquipmentSlot slot) {
        return slot == EquipmentSlot.LEGS;
    }
}
