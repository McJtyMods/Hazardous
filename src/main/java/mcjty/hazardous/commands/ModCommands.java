package mcjty.hazardous.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import mcjty.hazardous.Hazardous;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> commands = dispatcher.register(
                Commands.literal(Hazardous.MODID)
                        .then(CommandRadiationHere.register(dispatcher))
                        .then(CommandDose.register(dispatcher))
                        .then(CommandResetDose.register(dispatcher))
        );

        dispatcher.register(Commands.literal("haz").redirect(commands));
    }
}
