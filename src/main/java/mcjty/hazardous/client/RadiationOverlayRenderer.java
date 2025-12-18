package mcjty.hazardous.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.util.Map;

public class RadiationOverlayRenderer {

    public static void onRender(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.BOSS_EVENT_PROGRESS.type()) {
            return;
        }
        Map<ResourceLocation, Double> values = ClientRadiationData.getValues();
        if (values == null || values.isEmpty()) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<ResourceLocation, Double> entry : values.entrySet()) {
            if (!first) {
                builder.append(" / ");
            }
            builder.append(entry.getKey().getPath()).append("=").append(String.format("%.2f", entry.getValue()));
            first = false;
        }
        Font fontRenderer = Minecraft.getInstance().font;
        event.getGuiGraphics().drawString(fontRenderer,
                builder.toString(),
                4, 4,
                0xFFFFFF);
    }
}
