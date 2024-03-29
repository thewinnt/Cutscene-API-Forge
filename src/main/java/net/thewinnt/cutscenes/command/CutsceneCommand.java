package net.thewinnt.cutscenes.command;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.thewinnt.cutscenes.CutsceneManager;
import net.thewinnt.cutscenes.CutsceneType;
import net.thewinnt.cutscenes.networking.CutsceneNetworkHandler;
import net.thewinnt.cutscenes.networking.packets.StartCutscenePacket;
import net.thewinnt.cutscenes.networking.packets.StopCutscenePacket;
import net.thewinnt.cutscenes.util.ServerPlayerExt;

@Mod.EventBusSubscriber
public class CutsceneCommand {
    public static final DynamicCommandExceptionType PLAYER_ALREADY_IN_CUTSCENE = new DynamicCommandExceptionType(obj -> Component.translatable("commands.cutscene.error.player_already_in_cutscene", obj));
    public static final DynamicCommandExceptionType PLAYER_NOT_IN_CUTSCENE = new DynamicCommandExceptionType(obj -> Component.translatable("commands.cutscene.error.player_not_in_cutscene", obj));
    public static final SimpleCommandExceptionType MISSING_RUNNER = new SimpleCommandExceptionType(Component.translatable("commands.cutscene.error.no_runner"));
    public static final SimpleCommandExceptionType NO_PREVIEW = new SimpleCommandExceptionType(Component.translatable("commands.cutscene.error.no_preview"));
    public static final DynamicCommandExceptionType NO_CUTSCENE = new DynamicCommandExceptionType(obj -> Component.translatable("commands.cutscene.error.no_such_cutscene", obj));
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_CUTSCENES = (stack, builder) -> {
        return SharedSuggestionProvider.suggestResource(CutsceneManager.REGISTRY.keySet(), builder);
    };

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("cutscene")
            .requires((s) -> s.hasPermission(2))
            .then(literal("start")
            .then(argument("player", EntityArgument.player())
            .then(argument("type", ResourceLocationArgument.id())
                .suggests(SUGGEST_CUTSCENES)
            .executes((arg) -> {
                CommandSourceStack source = arg.getSource();
                ResourceLocation type = ResourceLocationArgument.getId(arg, "type");
                ServerPlayer player = EntityArgument.getPlayer(arg, "player");
                return showCutscene(source, type, player, player.getPosition(1), Vec3.ZERO, Vec3.ZERO);
            })
            .then(literal("at_preview")
            .executes((arg) -> {
                CommandSourceStack source = arg.getSource();
                ResourceLocation type = ResourceLocationArgument.getId(arg, "type");
                ServerPlayer player = EntityArgument.getPlayer(arg, "player");
                if (CutsceneManager.getPreviewedCutscene() != null && !type.equals(CutsceneManager.REGISTRY.inverse().get(CutsceneManager.getPreviewedCutscene()))) {
                    arg.getSource().sendSuccess(() -> Component.translatable("commands.cutscene.warning.cutscene_mismatch").withStyle(ChatFormatting.GOLD), false);
                }
                return showCutscene(source, type, player, new Vec3(CutsceneManager.getOffset()), Vec3.ZERO, new Vec3(CutsceneManager.previewPathYaw, CutsceneManager.previewPathPitch, CutsceneManager.previewPathRoll));
            })
            .then(argument("camera_rotation", new Vec3Argument(false))
            .executes((arg) -> {
                CommandSourceStack source = arg.getSource();
                ResourceLocation type = ResourceLocationArgument.getId(arg, "type");
                ServerPlayer player = EntityArgument.getPlayer(arg, "player");
                Vec3 rot = Vec3Argument.getVec3(arg, "camera_rotation");
                if (CutsceneManager.getPreviewedCutscene() != null && type != CutsceneManager.REGISTRY.inverse().get(CutsceneManager.getPreviewedCutscene())) {
                    arg.getSource().sendSuccess(() -> Component.translatable("commands.cutscene.warning.cutscene_mismatch").withStyle(ChatFormatting.GOLD), false);
                }
                return showCutscene(source, type, player, new Vec3(CutsceneManager.getOffset()), rot, new Vec3(CutsceneManager.previewPathYaw, CutsceneManager.previewPathPitch, CutsceneManager.previewPathPitch));
            })))
            .then(argument("start_pos", Vec3Argument.vec3())
            .executes((arg) -> {
                CommandSourceStack source = arg.getSource();
                ResourceLocation type = ResourceLocationArgument.getId(arg, "type");
                ServerPlayer player = EntityArgument.getPlayer(arg, "player");
                Vec3 pos = Vec3Argument.getVec3(arg, "start_pos");
                if (CutsceneManager.getPreviewedCutscene() != null && type != CutsceneManager.REGISTRY.inverse().get(CutsceneManager.getPreviewedCutscene())) {
                    arg.getSource().sendSuccess(() -> Component.translatable("commands.cutscene.warning.cutscene_mismatch").withStyle(ChatFormatting.GOLD), false);
                }
                return showCutscene(source, type, player, pos, Vec3.ZERO, Vec3.ZERO);
            })
            .then(argument("camera_rotation", Vec3Argument.vec3(false))
            .executes((arg) -> {
                CommandSourceStack source = arg.getSource();
                ResourceLocation type = ResourceLocationArgument.getId(arg, "type");
                ServerPlayer player = EntityArgument.getPlayer(arg, "player");
                Vec3 pos = Vec3Argument.getVec3(arg, "start_pos");
                Vec3 rot = Vec3Argument.getVec3(arg, "camera_rotation");
                double xRot = rot.x < -180 || rot.x > 180 ? Double.NaN : rot.x;
                double yRot = rot.y < -90 || rot.y > 90 ? Double.NaN : rot.y;
                double zRot = rot.z < -180 || rot.z > 180 ? Double.NaN : rot.z;
                if (CutsceneManager.getPreviewedCutscene() != null && type != CutsceneManager.REGISTRY.inverse().get(CutsceneManager.getPreviewedCutscene())) {
                    arg.getSource().sendSuccess(() -> Component.translatable("commands.cutscene.warning.cutscene_mismatch").withStyle(ChatFormatting.GOLD), false);
                }
                return showCutscene(source, type, player, pos, new Vec3(xRot, yRot, zRot), Vec3.ZERO);
            })
            .then(argument("path_rotation", Vec3Argument.vec3(false))
            .executes((arg) -> {
                CommandSourceStack source = arg.getSource();
                ResourceLocation type = ResourceLocationArgument.getId(arg, "type");
                ServerPlayer player = EntityArgument.getPlayer(arg, "player");
                Vec3 pos = Vec3Argument.getVec3(arg, "start_pos");
                Vec3 camRot = Vec3Argument.getVec3(arg, "camera_rotation");
                double xRot = camRot.x < -180 || camRot.x > 180 ? Double.NaN : camRot.x;
                double yRot = camRot.y < -90 || camRot.y > 90 ? Double.NaN : camRot.y;
                double zRot = camRot.z < -180 || camRot.z > 180 ? Double.NaN : camRot.z;
                Vec3 pathRot = Vec3Argument.getVec3(arg, "path_rotation");
                if (CutsceneManager.getPreviewedCutscene() != null && type != CutsceneManager.REGISTRY.inverse().get(CutsceneManager.getPreviewedCutscene())) {
                    arg.getSource().sendSuccess(() -> Component.translatable("commands.cutscene.warning.cutscene_mismatch").withStyle(ChatFormatting.GOLD), false);
                }
                return showCutscene(source, type, player, pos, new Vec3(xRot, yRot, zRot), pathRot);
            })))))))

            .then(literal("stop")
            .then(argument("player", EntityArgument.player())
            .executes((arg) -> {
                CommandSourceStack source = arg.getSource();
                ServerPlayer player = EntityArgument.getPlayer(arg, "player");
                ((ServerPlayerExt)player).setCutsceneTicks(0);
                CutsceneNetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new StopCutscenePacket());
                source.sendSuccess(() -> Component.translatable("commands.cutscene.stopped", player.getDisplayName()), true);
                return 1;
            })))

            .then(literal("preview")
            .then(literal("set")
            .then(argument("cutscene", ResourceLocationArgument.id())
                .suggests(SUGGEST_CUTSCENES)
            .executes((arg) -> {
                CommandSourceStack source = arg.getSource();
                ResourceLocation id = ResourceLocationArgument.getId(arg, "cutscene");
                CutsceneType type = CutsceneManager.REGISTRY.get(id);
                if (type == null) {
                    throw NO_CUTSCENE.create(id);
                }
                source.sendSuccess(() -> Component.translatable("commands.cutscene.preview.from_block", id), true);
                CutsceneManager.setPreviewedCutscene(type, new Vec3(0, 150, 0), 0, 0, 0);
                return 1;
            })
            .then(argument("start_pos", Vec3Argument.vec3())
            .executes((arg) -> {
                CommandSourceStack source = arg.getSource();
                ResourceLocation id = ResourceLocationArgument.getId(arg, "cutscene");
                CutsceneType type = CutsceneManager.REGISTRY.get(id);
                if (type == null) {
                    throw NO_CUTSCENE.create(id);
                }
                Vec3 pos = Vec3Argument.getVec3(arg, "start_pos");
                source.sendSuccess(() -> Component.translatable("commands.cutscene.preview.from_block", id), true);
                CutsceneManager.setPreviewedCutscene(type, pos, 0, 0, 0);
                return 1;
            })
            .then(argument("path_rotation", Vec3Argument.vec3(false))
            .executes((arg) -> {
                CommandSourceStack source = arg.getSource();
                ResourceLocation id = ResourceLocationArgument.getId(arg, "cutscene");
                CutsceneType type = CutsceneManager.REGISTRY.get(id);
                if (type == null) {
                    throw NO_CUTSCENE.create(id);
                }
                Vec3 pos = Vec3Argument.getVec3(arg, "start_pos");
                Vec3 rot = Vec3Argument.getVec3(arg, "path_rotation");
                source.sendSuccess(() -> Component.translatable("commands.cutscene.preview.from_block", id), true);
                CutsceneManager.setPreviewedCutscene(type, pos, (float)rot.x, (float)rot.y, (float)rot.z);
                return 1;
            })))))
            .then(literal("hide")
            .executes((arg) -> {
                CommandSourceStack source = arg.getSource();
                CutsceneManager.setPreviewedCutscene(null, Vec3.ZERO, 0, 0, 0);
                source.sendSuccess(() -> Component.translatable("commands.cutscene.preview.hide"), true);
                return 1;
            }))));

            
    }

    private static int showCutscene(CommandSourceStack source, ResourceLocation id, ServerPlayer player, Vec3 pos, Vec3 camRot, Vec3 pathRot) throws CommandSyntaxException {
        if (!CutsceneManager.REGISTRY.containsKey(id)) {
            throw NO_CUTSCENE.create(id);
        }
        CutsceneType type = CutsceneManager.REGISTRY.get(id);
        CutsceneNetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new StartCutscenePacket(id, pos, (float)camRot.x, (float)camRot.y, (float)camRot.z, (float)pathRot.x, (float)pathRot.y, (float)pathRot.z));
        ((ServerPlayerExt)player).setCutsceneTicks(type.length);
        source.sendSuccess(() -> Component.translatable("commands.cutscene.showing", id, player.getDisplayName()), true);
        return 1;
    }
}
