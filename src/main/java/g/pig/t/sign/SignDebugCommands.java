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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public final class SignDebugCommands {
    private static final String STATE_ARGUMENT = "state";
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
                                                        .executes(SignDebugCommands::setSignStateAtPosition))))))
        );
    }

    private static int getLookedAtSignState(CommandSourceStack source) throws CommandSyntaxException {
        TargetSign target = findLookedAtSign(source);
        if (target == null) {
            source.sendFailure(Component.literal("Look at a sign or pass a position."));
            return 0;
        }
        return sendState(source, target);
    }

    private static int getSignStateAtPosition(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, POS_ARGUMENT);
        TargetSign target = findSignAt(source.getLevel(), pos);
        if (target == null) {
            source.sendFailure(Component.literal("No sign found at " + pos.toShortString() + "."));
            return 0;
        }
        return sendState(source, target);
    }

    private static int setLookedAtSignState(CommandSourceStack source, String rawState) throws CommandSyntaxException {
        TargetSign target = findLookedAtSign(source);
        if (target == null) {
            source.sendFailure(Component.literal("Look at a sign or pass a position."));
            return 0;
        }
        return setState(source, target, rawState);
    }

    private static int setSignStateAtPosition(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, POS_ARGUMENT);
        TargetSign target = findSignAt(source.getLevel(), pos);
        if (target == null) {
            source.sendFailure(Component.literal("No sign found at " + pos.toShortString() + "."));
            return 0;
        }
        return setState(source, target, getString(context, STATE_ARGUMENT));
    }

    private static int sendState(CommandSourceStack source, TargetSign target) {
        SignState state = SignAttachments.getState(target.sign());
        GPigT.LOGGER.info("{} read sign state at {}: {}", source.getTextName(), target.pos().toShortString(), state.getSerializedName());
        source.sendSuccess(() -> Component.literal("Sign at " + target.pos().toShortString()
                + " is " + state.getSerializedName() + "."), false);
        return 1;
    }

    private static int setState(CommandSourceStack source, TargetSign target, String rawState) {
        SignState newState = SignState.fromSerializedName(rawState);
        if (newState == null) {
            source.sendFailure(Component.literal("Unknown sign state '" + rawState + "'. Use none, prompt, claimed, or response."));
            return 0;
        }

        SignState oldState = SignAttachments.getState(target.sign());
        SignAttachments.setState(target.sign(), newState);
        GPigT.LOGGER.info("{} changed sign state at {} from {} to {}",
                source.getTextName(), target.pos().toShortString(), oldState.getSerializedName(), newState.getSerializedName());
        source.sendSuccess(() -> Component.literal("Sign at " + target.pos().toShortString()
                + " changed from " + oldState.getSerializedName()
                + " to " + newState.getSerializedName() + "."), true);
        return 1;
    }

    private static TargetSign findLookedAtSign(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HitResult hit = player.pick(player.blockInteractionRange(), 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        return findSignAt(source.getLevel(), pos);
    }

    private static TargetSign findSignAt(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SignBlockEntity sign) {
            return new TargetSign(pos, sign);
        }
        return null;
    }

    private record TargetSign(BlockPos pos, SignBlockEntity sign) {
    }
}
