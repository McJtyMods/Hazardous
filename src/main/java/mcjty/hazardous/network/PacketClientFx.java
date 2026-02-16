package mcjty.hazardous.network;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.client.ClientFxManager;
import mcjty.lib.network.CustomPacketPayload;
import mcjty.lib.network.PlayPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PacketClientFx(String fxId, double intensity, int durationTicks) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(Hazardous.MODID, "clientfx");

    public static PacketClientFx create(FriendlyByteBuf buf) {
        return new PacketClientFx(buf.readUtf(64), buf.readDouble(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(fxId, 64);
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
