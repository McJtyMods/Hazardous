package mcjty.hazardous.client;

import mcjty.hazardous.data.HazardManager;
import mcjty.hazardous.network.PacketRequestItemEmissions;
import mcjty.hazardous.setup.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
            tooltip.add(Component.literal(String.format(Locale.ROOT, "  %s: %.2f", emission.hazardTypeId(), emission.intensity()))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
