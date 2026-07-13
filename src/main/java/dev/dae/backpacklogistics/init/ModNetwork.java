package dev.dae.backpacklogistics.init;

import dev.dae.backpacklogistics.BackpackLogistics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The mod's only network channel. It is never actually sent - registering it as an optional
 * channel makes it part of NeoForge's connection negotiation, which lets either side detect
 * whether the other side has the mod installed (see hasChannel checks in {@code BackpackLogistics}
 * and {@code ClientEvents}). Being optional, it never blocks a connection.
 */
public class ModNetwork {
	private ModNetwork() {}

	public record PingPayload() implements CustomPacketPayload {
		public static final PingPayload INSTANCE = new PingPayload();
		public static final CustomPacketPayload.Type<PingPayload> TYPE =
				new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackLogistics.MOD_ID, "ping"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PingPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public CustomPacketPayload.Type<PingPayload> type() {
			return TYPE;
		}
	}

	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1").optional();
		registrar.playBidirectional(PingPayload.TYPE, PingPayload.STREAM_CODEC, (payload, context) -> {});
	}
}
