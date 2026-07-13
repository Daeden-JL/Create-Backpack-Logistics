package dev.dae.backpacklogistics.client;

import dev.dae.backpacklogistics.init.ModNetwork;
import dev.dae.backpacklogistics.update.UpdateUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

public class ClientEvents {
	private ClientEvents() {}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(ClientEvents::onLoggingIn);
	}

	private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
		boolean serverHasMod = NetworkRegistry.hasChannel(event.getConnection(), ConnectionProtocol.PLAY, ModNetwork.PingPayload.TYPE.id());
		if (!serverHasMod) {
			event.getPlayer().displayClientMessage(
					Component.translatable("message.backpack_logistics.server_missing").withStyle(ChatFormatting.GOLD), false);
			return;
		}
		// announce our version so the server can push a newer build if it has one
		if (NetworkRegistry.hasChannel(event.getConnection(), ConnectionProtocol.PLAY, ModNetwork.VersionHelloPayload.TYPE.id())) {
			PacketDistributor.sendToServer(new ModNetwork.VersionHelloPayload(UpdateUtil.currentVersion()), new CustomPacketPayload[0]);
		}
	}
}
