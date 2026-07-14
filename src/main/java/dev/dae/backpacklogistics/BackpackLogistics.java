package dev.dae.backpacklogistics;

import dev.dae.backpacklogistics.client.ClientEvents;
import dev.dae.backpacklogistics.client.ClientSetup;
import dev.dae.backpacklogistics.init.ModItems;
import dev.dae.backpacklogistics.init.ModNetwork;
import dev.dae.backpacklogistics.update.GithubUpdateChecker;
import dev.dae.backpacklogistics.update.ServerUpdateSender;
import dev.dae.backpacklogistics.update.UpdateUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(BackpackLogistics.MOD_ID)
public class BackpackLogistics {
	public static final String MOD_ID = "backpack_logistics";

	public BackpackLogistics(IEventBus modBus, ModContainer modContainer) {
		modContainer.registerConfig(ModConfig.Type.COMMON, BackpackLogisticsConfig.SPEC);
		ModItems.registerHandlers(modBus);
		modBus.addListener(ModNetwork::register);
		NeoForge.EVENT_BUS.addListener(BackpackLogistics::onPlayerLoggedIn);
		NeoForge.EVENT_BUS.addListener(BackpackLogistics::onServerStarted);
		if (FMLEnvironment.dist.isClient()) {
			ClientSetup.init();
			ClientEvents.register();
		}
	}

	private static void onServerStarted(ServerStartedEvent event) {
		GithubUpdateChecker.checkOnServerStart(event.getServer());
	}

	private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		if (!player.connection.hasChannel(ModNetwork.PingPayload.TYPE)) {
			player.sendSystemMessage(
					Component.translatable("message.backpack_logistics.client_missing").withStyle(ChatFormatting.GOLD));
			return;
		}
		// let capable clients compare against the version this server actually runs
		if (player.connection.hasChannel(ModNetwork.ServerVersionPayload.TYPE)) {
			PacketDistributor.sendToPlayer(player, new ModNetwork.ServerVersionPayload(UpdateUtil.currentVersion()), new CustomPacketPayload[0]);
		}
		// remind operators when an update sits downloaded, waiting for the restart
		String pendingVersion = ServerUpdateSender.getPendingRestartVersion();
		if (pendingVersion != null && player.getServer() != null && player.getServer().getPlayerList().isOp(player.getGameProfile())) {
			player.sendSystemMessage(
					Component.translatable("message.backpack_logistics.update_pending_restart", pendingVersion).withStyle(ChatFormatting.GOLD));
		}
	}
}
