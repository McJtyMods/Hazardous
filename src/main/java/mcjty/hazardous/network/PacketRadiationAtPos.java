package mcjty.hazardous.network;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.client.ClientRadiationData;
import mcjty.lib.network.CustomPacketPayload;
import mcjty.lib.network.PlayPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record PacketRadiationAtPos(Map<ResourceLocation, Double> values) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(Hazardous.MODID, "radiationatpos");

    public static PacketRadiationAtPos create(FriendlyByteBuf buf) {
        Map<ResourceLocation, Double> values = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            values.put(buf.readResourceLocation(), buf.readDouble());
        }
        return new  PacketRadiationAtPos(values);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(values.size());
        for (Map.Entry<ResourceLocation, Double> entry : values.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeDouble(entry.getValue());
        }
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public void handle(PlayPayloadContext ctx) {
        ctx.workHandler().submitAsync(() -> ctx.player().ifPresent(player -> {
            ClientRadiationData.setValues(values);
        }));
    }
}
