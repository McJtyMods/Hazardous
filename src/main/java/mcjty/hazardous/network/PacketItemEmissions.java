package mcjty.hazardous.network;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.client.ClientItemEmissionData;
import mcjty.hazardous.data.HazardManager;
import mcjty.lib.network.CustomPacketPayload;
import mcjty.lib.network.PlayPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record PacketItemEmissions(ItemStack stack, List<HazardManager.TooltipEmission> emissions) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(Hazardous.MODID, "item_emissions");

    public static PacketItemEmissions create(FriendlyByteBuf buf) {
        ItemStack stack = buf.readItem();
        int size = buf.readInt();
        List<HazardManager.TooltipEmission> emissions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            emissions.add(new HazardManager.TooltipEmission(buf.readResourceLocation(), buf.readDouble()));
        }
        return new PacketItemEmissions(stack, emissions);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeItem(stack);
        buf.writeInt(emissions.size());
        for (HazardManager.TooltipEmission emission : emissions) {
            buf.writeResourceLocation(emission.hazardTypeId());
            buf.writeDouble(emission.intensity());
        }
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public void handle(PlayPayloadContext ctx) {
        ctx.workHandler().submitAsync(() -> ClientItemEmissionData.store(stack, emissions));
    }
}
