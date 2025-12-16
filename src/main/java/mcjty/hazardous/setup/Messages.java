package mcjty.hazardous.setup;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.network.PacketRadiationAtPos;
import mcjty.lib.network.IPayloadRegistrar;
import mcjty.lib.network.Networking;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;

public class Messages {

    private static IPayloadRegistrar registrar;

    public static void registerMessages() {
        registrar = Networking.registrar(Hazardous.MODID)
                .versioned("1.0")
                .optional();

        registrar.play(PacketRadiationAtPos.class, PacketRadiationAtPos::create, handler -> handler.client(PacketRadiationAtPos::handle));
    }

    public static <T> void sendToPlayer(T packet, Player player) {
        registrar.getChannel().sendTo(packet, ((net.minecraft.server.level.ServerPlayer)player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static <T> void sendToServer(T packet) {
        registrar.getChannel().sendToServer(packet);
    }

    public static <T> void sendToAllPlayers(ResourceKey<Level> level, T packet) {
        registrar.getChannel().send(PacketDistributor.DIMENSION.with(() -> level), packet);
    }
}
