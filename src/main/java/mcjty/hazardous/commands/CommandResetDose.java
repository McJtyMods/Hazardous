package mcjty.hazardous.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mcjty.hazardous.data.PlayerDoseData;
import mcjty.hazardous.data.PlayerDoseDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CommandResetDose {

    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
        return Commands.literal("resetdose")
                .requires(cs -> cs.hasPermission(1))
                .executes(CommandResetDose::runSelf)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(CommandResetDose::runOther));
    }

    private static int runSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        resetDose(context.getSource(), player);
        return 0;
    }

    private static int runOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        resetDose(context.getSource(), player);
        return 0;
    }

    private static void resetDose(CommandSourceStack source, ServerPlayer player) {
        PlayerDoseDispatcher.getPlayerDose(player).ifPresent(PlayerDoseData::clear);
        if (source.getEntity() == player) {
            source.sendSuccess(() -> Component.literal("Hazard dose reset."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Hazard dose reset for " + player.getGameProfile().getName() + "."), false);
        }
    }
}
