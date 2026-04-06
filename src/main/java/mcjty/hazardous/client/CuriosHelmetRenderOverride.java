package mcjty.hazardous.client;

import mcjty.hazardous.compat.CuriosCompat;
import mcjty.hazardous.setup.Config;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

public class CuriosHelmetRenderOverride {

    public static boolean shouldHideDefaultHelmet(LivingEntity livingEntity, EquipmentSlot slot) {
        if (slot != EquipmentSlot.HEAD || !Config.CURIOS_HEAD_OVERRIDE_HELMET_RENDER.get() || !ModList.get().isLoaded("curios")) {
            return false;
        }
        return CuriosCompat.findFirstHeadCurio(livingEntity, CuriosHelmetRenderOverride::shouldOverrideHelmetRender)
                .filter(slotResult -> slotResult.slotContext().visible())
                .isPresent();
    }

    private static boolean shouldOverrideHelmetRender(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem armorItem) || armorItem.getEquipmentSlot() != EquipmentSlot.HEAD) {
            return false;
        }
        return CuriosRendererRegistry.getRenderer(stack.getItem()).isPresent();
    }
}
