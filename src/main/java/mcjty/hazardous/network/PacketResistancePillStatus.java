package mcjty.hazardous.network;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.client.ClientData;
import mcjty.hazardous.data.PlayerHazardData;
import mcjty.lib.network.CustomPacketPayload;
import mcjty.lib.network.PlayPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record PacketResistancePillStatus(Map<ResourceLocation, PlayerHazardData.ResistancePillStatus> values) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(Hazardous.MODID, "resistancepillstatus");

    public static PacketResistancePillStatus create(FriendlyByteBuf buf) {
        Map<ResourceLocation, PlayerHazardData.ResistancePillStatus> values = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            values.put(buf.readResourceLocation(), new PlayerHazardData.ResistancePillStatus(buf.readDouble(), buf.readInt(), buf.readLong()));
        }
        return new PacketResistancePillStatus(values);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(values.size());
        for (Map.Entry<ResourceLocation, PlayerHazardData.ResistancePillStatus> entry : values.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeDouble(entry.getValue().amount());
            buf.writeInt(entry.getValue().stacks());
            buf.writeLong(entry.getValue().expiresAt());
        }
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public void handle(PlayPayloadContext ctx) {
        ctx.workHandler().submitAsync(() -> ctx.player().ifPresent(player -> ClientData.setResistancePillStatuses(values)));
    }
}
