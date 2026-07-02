package g.pig.t.sign;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import g.pig.t.GPigT;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public final class SignDebugCommands {
    private static final String STATE_ARGUMENT = "state";
    private static final String TEXT_ARGUMENT = "text";
    private static final String POS_ARGUMENT = "pos";

    private SignDebugCommands() {
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("gpigt")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("sign-state")
                                .then(Commands.literal("get")
                                        .executes(context -> getLookedAtSignState(context.getSource()))
                                        .then(Commands.argument(POS_ARGUMENT, BlockPosArgument.blockPos())
                                                .executes(SignDebugCommands::getSignStateAtPosition)))
                                .then(Commands.literal("set")
                                        .then(Commands.argument(STATE_ARGUMENT, word())
                                                .executes(context -> setLookedAtSignState(
                                                        context.getSource(),
                                                        getString(context, STATE_ARGUMENT))))
                                        .then(Commands.argument(POS_ARGUMENT, BlockPosArgument.blockPos())
                                                .then(Commands.argument(STATE_ARGUMENT, word())
                                                        .executes(SignDebugCommands::setSignStateAtPosition)))))
                        .then(Commands.literal("sign-text")
                                .then(Commands.literal("get")
                                        .executes(context -> getLookedAtSignText(context.getSource()))
                                        .then(Commands.argument(POS_ARGUMENT, BlockPosArgument.blockPos())
                                                .executes(SignDebugCommands::getSignTextAtPosition)))
                                .then(Commands.literal("set")
                                        .then(Commands.argument(TEXT_ARGUMENT, greedyString())
                                                .executes(context -> setLookedAtSignText(
                                                        context.getSource(),
                                                        getString(context, TEXT_ARGUMENT))))
                                        .then(Commands.argument(POS_ARGUMENT, BlockPosArgument.blockPos())
                                                .then(Commands.argument(TEXT_ARGUMENT, greedyString())
                                                        .executes(SignDebugCommands::setSignTextAtPosition))))))
        );
    }

    private static int getLookedAtSignState(CommandSourceStack source) throws CommandSyntaxException {
        BlockPos pos = findLookedAtSign(source);
        if (pos == null) {
            source.sendFailure(Component.literal("Look at a sign or pass a position."));
            return 0;
        }
        return sendState(source, pos);
    }

    private static int getSignStateAtPosition(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, POS_ARGUMENT);
        if (!isSign(source.getLevel(), pos)) {
            source.sendFailure(Component.literal("No sign found at " + pos.toShortString() + "."));
            return 0;
        }
        return sendState(source, pos);
    }

    private static int setLookedAtSignState(CommandSourceStack source, String rawState) throws CommandSyntaxException {
        BlockPos pos = findLookedAtSign(source);
        if (pos == null) {
            source.sendFailure(Component.literal("Look at a sign or pass a position."));
            return 0;
        }
        return setState(source, pos, rawState);
    }

    private static int setSignStateAtPosition(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, POS_ARGUMENT);
        if (!isSign(source.getLevel(), pos)) {
            source.sendFailure(Component.literal("No sign found at " + pos.toShortString() + "."));
            return 0;
        }
        return setState(source, pos, getString(context, STATE_ARGUMENT));
    }

    private static int getLookedAtSignText(CommandSourceStack source) throws CommandSyntaxException {
        BlockPos pos = findLookedAtSign(source);
        if (pos == null) {
            source.sendFailure(Component.literal("Look at a sign or pass a position."));
            return 0;
        }
        return sendText(source, pos);
    }

    private static int getSignTextAtPosition(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, POS_ARGUMENT);
        if (!isSign(source.getLevel(), pos)) {
            source.sendFailure(Component.literal("No sign found at " + pos.toShortString() + "."));
            return 0;
        }
        return sendText(source, pos);
    }

    private static int setLookedAtSignText(CommandSourceStack source, String rawText) throws CommandSyntaxException {
        BlockPos pos = findLookedAtSign(source);
        if (pos == null) {
            source.sendFailure(Component.literal("Look at a sign or pass a position."));
            return 0;
        }
        return setText(source, pos, rawText);
    }

    private static int setSignTextAtPosition(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, POS_ARGUMENT);
        if (!isSign(source.getLevel(), pos)) {
            source.sendFailure(Component.literal("No sign found at " + pos.toShortString() + "."));
            return 0;
        }
        return setText(source, pos, getString(context, TEXT_ARGUMENT));
    }

    private static int sendState(CommandSourceStack source, BlockPos pos) {
        SignState state = SignIO.getState(source.getLevel(), pos);
        GPigT.LOGGER.info("{} read sign state at {}: {}", source.getTextName(), pos.toShortString(), state.getSerializedName());
        source.sendSuccess(() -> Component.literal("Sign at " + pos.toShortString()
                + " is " + state.getSerializedName() + "."), false);
        return 1;
    }

    private static int setState(CommandSourceStack source, BlockPos pos, String rawState) {
        SignState newState = SignState.fromSerializedName(rawState);
        if (newState == null) {
            source.sendFailure(Component.literal("Unknown sign state '" + rawState + "'. Use none, prompt, claimed, or response."));
            return 0;
        }

        SignState oldState = SignIO.getState(source.getLevel(), pos);
        SignIO.setState(source.getLevel(), pos, newState);
        GPigT.LOGGER.info("{} changed sign state at {} from {} to {}",
                source.getTextName(), pos.toShortString(), oldState.getSerializedName(), newState.getSerializedName());
        source.sendSuccess(() -> Component.literal("Sign at " + pos.toShortString()
                + " changed from " + oldState.getSerializedName()
                + " to " + newState.getSerializedName() + "."), true);
        return 1;
    }

    private static int sendText(CommandSourceStack source, BlockPos pos) {
        String text = SignIO.readFrontText(source.getLevel(), pos);
        GPigT.LOGGER.info("{} read sign text at {}: \"{}\"", source.getTextName(), pos.toShortString(), text);
        source.sendSuccess(() -> Component.literal("Sign at " + pos.toShortString()
                + " front reads: \"" + text + "\""), false);
        return 1;
    }

    private static int setText(CommandSourceStack source, BlockPos pos, String rawText) {
        List<Component> lines = new ArrayList<>();
        String[] parts = rawText.split("\\|");
        for (int i = 0; i < parts.length && i < SignText.LINES; i++) {
            lines.add(Component.literal(parts[i]));
        }
        SignIO.writeFrontText(source.getLevel(), pos, lines);
        GPigT.LOGGER.info("{} wrote sign text at {}: \"{}\"", source.getTextName(), pos.toShortString(), rawText);
        source.sendSuccess(() -> Component.literal("Wrote front face of sign at " + pos.toShortString() + "."), true);
        return 1;
    }

    private static BlockPos findLookedAtSign(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HitResult hit = player.pick(player.blockInteractionRange(), 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        return isSign(source.getLevel(), pos) ? pos : null;
    }

    private static boolean isSign(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof SignBlockEntity;
    }
}
