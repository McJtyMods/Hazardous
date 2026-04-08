package mcjty.hazardous.network;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.client.ClientFxManager;
import mcjty.hazardous.data.objects.ClientFxId;
import mcjty.lib.network.CustomPacketPayload;
import mcjty.lib.network.PlayPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PacketClientFx(ClientFxId fxId, double intensity, int durationTicks) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(Hazardous.MODID, "clientfx");

    public static PacketClientFx create(FriendlyByteBuf buf) {
        return new PacketClientFx(buf.readEnum(ClientFxId.class), buf.readDouble(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(fxId);
        buf.writeDouble(intensity);
        buf.writeInt(durationTicks);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public void handle(PlayPayloadContext ctx) {
        ctx.workHandler().submitAsync(() -> ClientFxManager.activate(fxId, intensity, durationTicks));
    }
}
