package mcjty.hazardous.network;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.data.HazardManager;
import mcjty.hazardous.setup.Messages;
import mcjty.lib.network.CustomPacketPayload;
import mcjty.lib.network.PlayPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record PacketRequestItemEmissions(ItemStack stack) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(Hazardous.MODID, "request_item_emissions");

    public static PacketRequestItemEmissions create(FriendlyByteBuf buf) {
        return new PacketRequestItemEmissions(buf.readItem());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeItem(stack);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public void handle(PlayPayloadContext ctx) {
        ctx.workHandler().submitAsync(() -> ctx.player().ifPresent(player -> {
            if (!(player instanceof ServerPlayer serverPlayer) || stack.isEmpty()) {
                return;
            }
            Messages.sendToPlayer(new PacketItemEmissions(stack, HazardManager.getItemEmissions(stack, serverPlayer)), serverPlayer);
        }));
    }
}
