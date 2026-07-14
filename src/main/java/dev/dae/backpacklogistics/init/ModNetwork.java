package dev.dae.backpacklogistics.init;

import dev.dae.backpacklogistics.BackpackLogistics;
import dev.dae.backpacklogistics.update.ClientUpdateReceiver;
import dev.dae.backpacklogistics.update.ServerUpdateSender;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * All payloads are optional, so none of them ever block a connection.
 *
 * PingPayload is never sent - registering it makes it part of NeoForge's connection
 * negotiation, which lets either side detect whether the other has the mod installed.
 *
 * The update payloads implement server-to-client distribution of newer mod versions:
 * the client announces its version after login, and the server streams its newest jar
 * back in chunks when the client is outdated.
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

	/** Client -> server: announces the client's installed version of this mod after login. */
	public record VersionHelloPayload(String version) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<VersionHelloPayload> TYPE =
				new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackLogistics.MOD_ID, "version_hello"));
		public static final StreamCodec<RegistryFriendlyByteBuf, VersionHelloPayload> STREAM_CODEC =
				StreamCodec.composite(ByteBufCodecs.STRING_UTF8, VersionHelloPayload::version, VersionHelloPayload::new);

		@Override
		public CustomPacketPayload.Type<VersionHelloPayload> type() {
			return TYPE;
		}
	}

	/** Server -> client: announces the version the server is actually running. */
	public record ServerVersionPayload(String version) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<ServerVersionPayload> TYPE =
				new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackLogistics.MOD_ID, "server_version"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ServerVersionPayload> STREAM_CODEC =
				StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ServerVersionPayload::version, ServerVersionPayload::new);

		@Override
		public CustomPacketPayload.Type<ServerVersionPayload> type() {
			return TYPE;
		}
	}

	/** Server -> client: header of an incoming jar transfer. */
	public record UpdateStartPayload(String version, int totalSize, int chunkCount, String sha256) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<UpdateStartPayload> TYPE =
				new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackLogistics.MOD_ID, "update_start"));
		public static final StreamCodec<RegistryFriendlyByteBuf, UpdateStartPayload> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, UpdateStartPayload::version,
				ByteBufCodecs.VAR_INT, UpdateStartPayload::totalSize,
				ByteBufCodecs.VAR_INT, UpdateStartPayload::chunkCount,
				ByteBufCodecs.STRING_UTF8, UpdateStartPayload::sha256,
				UpdateStartPayload::new);

		@Override
		public CustomPacketPayload.Type<UpdateStartPayload> type() {
			return TYPE;
		}
	}

	/** Server -> client: one chunk of the jar being transferred. */
	public record UpdateChunkPayload(int index, byte[] data) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<UpdateChunkPayload> TYPE =
				new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BackpackLogistics.MOD_ID, "update_chunk"));
		public static final StreamCodec<RegistryFriendlyByteBuf, UpdateChunkPayload> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, UpdateChunkPayload::index,
				ByteBufCodecs.BYTE_ARRAY, UpdateChunkPayload::data,
				UpdateChunkPayload::new);

		@Override
		public CustomPacketPayload.Type<UpdateChunkPayload> type() {
			return TYPE;
		}
	}

	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1").optional();
		registrar.playBidirectional(PingPayload.TYPE, PingPayload.STREAM_CODEC, (payload, context) -> {});
		registrar.playToServer(VersionHelloPayload.TYPE, VersionHelloPayload.STREAM_CODEC, (payload, context) -> {
			if (context.player() instanceof ServerPlayer serverPlayer) {
				context.enqueueWork(() -> ServerUpdateSender.onClientVersionHello(serverPlayer, payload.version()));
			}
		});
		registrar.playToClient(ServerVersionPayload.TYPE, ServerVersionPayload.STREAM_CODEC, (payload, context) -> {
			if (FMLEnvironment.dist.isClient()) {
				context.enqueueWork(() -> ClientUpdateReceiver.onServerVersion(payload.version()));
			}
		});
		registrar.playToClient(UpdateStartPayload.TYPE, UpdateStartPayload.STREAM_CODEC, (payload, context) -> {
			if (FMLEnvironment.dist.isClient()) {
				context.enqueueWork(() -> ClientUpdateReceiver.onUpdateStart(payload));
			}
		});
		registrar.playToClient(UpdateChunkPayload.TYPE, UpdateChunkPayload.STREAM_CODEC, (payload, context) -> {
			if (FMLEnvironment.dist.isClient()) {
				context.enqueueWork(() -> ClientUpdateReceiver.onUpdateChunk(payload));
			}
		});
	}
}
