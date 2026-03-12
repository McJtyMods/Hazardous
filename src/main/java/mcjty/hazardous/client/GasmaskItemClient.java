package mcjty.hazardous.client;

import mcjty.hazardous.items.model.GasmaskModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

public class GasmaskItemClient implements IClientItemExtensions {

    private GasmaskModel gasmaskModel;

    @Override
    public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
        if (equipmentSlot != EquipmentSlot.HEAD) {
            return original;
        }
        if (gasmaskModel == null) {
            gasmaskModel = new GasmaskModel(Minecraft.getInstance().getEntityModels().bakeLayer(GasmaskModel.LAYER_LOCATION));
        }
        return gasmaskModel;
    }
}
