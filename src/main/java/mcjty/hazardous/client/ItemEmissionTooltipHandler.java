package mcjty.hazardous.client;

import mcjty.hazardous.data.HazardManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Locale;

public class ItemEmissionTooltipHandler {

    public static void onItemTooltip(ItemTooltipEvent event) {
        List<HazardManager.TooltipEmission> emissions = HazardManager.getItemEmissions(event.getItemStack(), event.getEntity());
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
