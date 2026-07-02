package g.pig.t.ponder;

import g.pig.t.GPigT;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

public final class PonderDebugCommands {
    private static final String QUESTION_ARGUMENT = "question";

    private PonderDebugCommands() {
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("gpigt")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("ponder")
                                .then(Commands.argument(QUESTION_ARGUMENT, greedyString())
                                        .executes(context -> ponder(
                                                context.getSource(),
                                                getString(context, QUESTION_ARGUMENT))))))
        );
    }

    private static int ponder(CommandSourceStack source, String question) {
        GPigT.LOGGER.info("{} started pondering: \"{}\"", source.getTextName(), question);
        source.sendSuccess(() -> Component.literal("Pondering \"" + question + "\"..."), false);
        PonderService.ponder(question).thenAccept(response ->
                // Callbacks run on the ponder thread — hop back to the server
                // thread before touching game state.
                source.getServer().execute(() -> {
                    GPigT.LOGGER.info("Ponder response for \"{}\": \"{}\"", question, response);
                    source.sendSuccess(() -> Component.literal("Ponder response: \"" + response + "\""), false);
                }));
        return 1;
    }
}
