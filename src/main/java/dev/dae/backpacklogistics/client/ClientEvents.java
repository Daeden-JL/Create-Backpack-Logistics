package dev.dae.backpacklogistics.client;

import dev.dae.backpacklogistics.init.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

public class ClientEvents {
	private ClientEvents() {}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(ClientEvents::onLoggingIn);
	}

	private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
		if (!NetworkRegistry.hasChannel(event.getConnection(), ConnectionProtocol.PLAY, ModNetwork.PingPayload.TYPE.id())) {
			event.getPlayer().displayClientMessage(
					Component.translatable("message.backpack_logistics.server_missing").withStyle(ChatFormatting.GOLD), false);
		}
	}
}
