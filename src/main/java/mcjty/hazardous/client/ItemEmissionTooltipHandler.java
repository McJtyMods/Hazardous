package mcjty.hazardous.client;

import mcjty.hazardous.data.HazardManager;
import mcjty.hazardous.items.TooltipNameHelper;
import mcjty.hazardous.network.PacketRequestItemEmissions;
import mcjty.hazardous.setup.Config;
import mcjty.hazardous.setup.HazardousTags;
import mcjty.hazardous.setup.Messages;
import mcjty.hazardous.setup.Registration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Locale;

public class ItemEmissionTooltipHandler {

    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (event.getEntity() == null || stack.isEmpty()) {
            return;
        }

        addProtectiveArmorTooltip(stack, event.getToolTip());

        List<HazardManager.TooltipEmission> emissions = ClientItemEmissionData.getEmissions(stack);
        if (emissions == null) {
            if (ClientItemEmissionData.markPending(stack)) {
                Messages.sendToServer(new PacketRequestItemEmissions(stack));
            }
            if (ClientItemEmissionData.isPending(stack)) {
                event.getToolTip().add(Component.literal("Checking carried emissions...")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            return;
        }
        if (emissions.isEmpty()) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        tooltip.add(Component.literal("Emissions when carried:").withStyle(ChatFormatting.YELLOW));
        for (HazardManager.TooltipEmission emission : emissions) {
            tooltip.add(Component.literal(String.format(Locale.ROOT, "  %s: %.2f",
                            TooltipNameHelper.getHazardTypeName(emission.hazardTypeId()),
                            emission.intensity()))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static void addProtectiveArmorTooltip(ItemStack stack, List<Component> tooltip) {
        if (!stack.is(HazardousTags.PROTECTIVE_ARMOR) || stack.is(Registration.GASMASK.get())) {
            return;
        }
        if (!(stack.getItem() instanceof ArmorItem)) {
            return;
        }

        tooltip.add(Component.literal("Hazard protection when worn:").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Protects: " + TooltipNameHelper.getHazardTypeName(Config.getGasmaskProtectedType().orElse(null)))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format(Locale.ROOT, "  Protection: %.0f%%", Mth.clamp(Config.GASMASK_PROTECTION_LEVEL.get(), 0.0, 1.0) * 100.0))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Uses 1 durability per protection application")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
