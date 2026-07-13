package dev.dae.backpacklogistics;

import dev.dae.backpacklogistics.client.ClientEvents;
import dev.dae.backpacklogistics.client.ClientSetup;
import dev.dae.backpacklogistics.init.ModItems;
import dev.dae.backpacklogistics.init.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(BackpackLogistics.MOD_ID)
public class BackpackLogistics {
	public static final String MOD_ID = "backpack_logistics";

	public BackpackLogistics(IEventBus modBus) {
		ModItems.registerHandlers(modBus);
		modBus.addListener(ModNetwork::register);
		NeoForge.EVENT_BUS.addListener(BackpackLogistics::onPlayerLoggedIn);
		if (FMLEnvironment.dist.isClient()) {
			ClientSetup.init();
			ClientEvents.register();
		}
	}

	private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && !player.connection.hasChannel(ModNetwork.PingPayload.TYPE)) {
			player.sendSystemMessage(
					Component.translatable("message.backpack_logistics.client_missing").withStyle(ChatFormatting.GOLD));
		}
	}
}
