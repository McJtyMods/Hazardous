package mcjty.hazardous.items.model;

import mcjty.hazardous.Hazardous;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class GasmaskModel extends HumanoidModel<LivingEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(Hazardous.MODID, "gasmask"), "main");

    public GasmaskModel(ModelPart root) {
        super(root, RenderType::entityCutoutNoCull);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();
        CubeDeformation shell = CubeDeformation.NONE;

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));

        PartDefinition mask = head.addOrReplaceChild("mask", CubeListBuilder.create()
                .texOffs(18, 16).addBox(-4.0F, -6.0F, -4.2F, 3.0F, 6.0F, 0.0F, shell)
                .texOffs(0, 0).addBox(1.0F, -6.0F, -4.2F, 3.0F, 6.0F, 0.0F, shell)
                .texOffs(30, 30).addBox(4.0F, -8.0F, -4.2F, 1.0F, 8.0F, 1.0F, shell)
                .texOffs(26, 30).addBox(-5.0F, -8.0F, -4.2F, 1.0F, 8.0F, 1.0F, shell)
                .texOffs(23, 4).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 1.0F, 1.0F, shell)
                .texOffs(0, 0).addBox(-4.0F, -9.0F, -3.0F, 8.0F, 1.0F, 7.0F, shell)
                .texOffs(18, 8).addBox(-4.0F, -8.0F, 4.0F, 8.0F, 7.0F, 1.0F, shell)
                .texOffs(7, 38).addBox(-5.0F, -8.0F, -3.2F, 1.0F, 7.0F, 7.2F, shell)
                .texOffs(0, 8).addBox(4.0F, -8.0F, -3.2F, 1.0F, 7.0F, 7.2F, shell)
                .texOffs(25, 26).addBox(-4.0F, -8.0F, -5.0F, 8.0F, 3.0F, 1.0F, shell)
                .texOffs(23, 0).addBox(-4.0F, -3.0F, -5.0F, 8.0F, 3.0F, 1.0F, shell)
                .texOffs(12, 29).addBox(-2.5F, -3.0F, -7.0F, 5.0F, 5.0F, 2.0F, shell)
                .texOffs(0, 22).addBox(-1.0F, -5.0F, -5.0F, 2.0F, 2.0F, 1.0F, shell), PartPose.ZERO);

        mask.addOrReplaceChild("left_filter", CubeListBuilder.create()
                .texOffs(0, 8).addBox(-8.0F, -3.0F, -5.0F, 1.0F, 4.0F, 2.0F, shell)
                .texOffs(0, 25).addBox(-7.0F, -4.0F, -6.0F, 2.0F, 6.0F, 4.0F, shell),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2094F, -0.384F, 0.0F));

        mask.addOrReplaceChild("right_filter", CubeListBuilder.create()
                .texOffs(9, 8).addBox(7.0F, -3.0F, -5.0F, 1.0F, 4.0F, 2.0F, shell)
                .texOffs(25, 16).addBox(5.0F, -4.0F, -6.0F, 2.0F, 6.0F, 4.0F, shell),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2182F, 0.3927F, 0.0F));

        return LayerDefinition.create(meshDefinition, 64, 64);
    }
}
