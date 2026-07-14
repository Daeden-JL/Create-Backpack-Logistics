package dev.dae.backpacklogistics.update;

import dev.dae.backpacklogistics.init.ModNetwork;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Streams the newest available jar of this mod to clients that report an older version.
 * The newest available jar is either the running one, or the one the GitHub updater has
 * downloaded and staged for the next restart.
 */
public class ServerUpdateSender {
	private static final int CHUNK_SIZE = 30_000;

	@Nullable
	private static volatile PendingUpdate pendingUpdate = null;
	@Nullable
	private static volatile PendingUpdate runningJar = null;

	private ServerUpdateSender() {}

	public static void setPendingUpdate(String version, byte[] jarBytes) {
		pendingUpdate = new PendingUpdate(version, jarBytes, UpdateUtil.sha256(jarBytes));
	}

	/** The version downloaded and waiting for a server restart, or null when up to date. */
	@Nullable
	public static String getPendingRestartVersion() {
		PendingUpdate pending = pendingUpdate;
		return pending != null && UpdateUtil.isNewerVersion(pending.version(), UpdateUtil.currentVersion())
				? pending.version()
				: null;
	}

	public static void onClientVersionHello(ServerPlayer player, String clientVersion) {
		PendingUpdate newest = newestAvailable();
		if (newest == null || !UpdateUtil.isNewerVersion(newest.version(), clientVersion)) {
			return;
		}
		// old clients registered fewer payload types; never send what they can't decode
		if (!player.connection.hasChannel(ModNetwork.UpdateStartPayload.TYPE.id())
				|| !player.connection.hasChannel(ModNetwork.UpdateChunkPayload.TYPE.id())) {
			return;
		}
		UpdateUtil.LOGGER.info("Sending Backpack Logistics {} to {} (client has {})", newest.version(),
				player.getGameProfile().getName(), clientVersion);

		byte[] jar = newest.jarBytes();
		int chunkCount = (jar.length + CHUNK_SIZE - 1) / CHUNK_SIZE;
		PacketDistributor.sendToPlayer(player,
				new ModNetwork.UpdateStartPayload(newest.version(), jar.length, chunkCount, newest.sha256()),
				new CustomPacketPayload[0]);
		for (int i = 0; i < chunkCount; i++) {
			int from = i * CHUNK_SIZE;
			byte[] chunk = Arrays.copyOfRange(jar, from, Math.min(from + CHUNK_SIZE, jar.length));
			PacketDistributor.sendToPlayer(player, new ModNetwork.UpdateChunkPayload(i, chunk), new CustomPacketPayload[0]);
		}
	}

	@Nullable
	private static PendingUpdate newestAvailable() {
		PendingUpdate pending = pendingUpdate;
		PendingUpdate running = getRunningJar();
		if (pending == null) {
			return running;
		}
		if (running == null || UpdateUtil.isNewerVersion(pending.version(), running.version())) {
			return pending;
		}
		return running;
	}

	@Nullable
	private static PendingUpdate getRunningJar() {
		PendingUpdate cached = runningJar;
		if (cached != null) {
			return cached;
		}
		Path jarPath = UpdateUtil.currentJarPath();
		if (jarPath == null) {
			return null;
		}
		try {
			byte[] bytes = Files.readAllBytes(jarPath);
			if (bytes.length > UpdateUtil.MAX_JAR_BYTES) {
				return null;
			}
			cached = new PendingUpdate(UpdateUtil.currentVersion(), bytes, UpdateUtil.sha256(bytes));
			runningJar = cached;
			return cached;
		} catch (IOException e) {
			UpdateUtil.LOGGER.warn("Could not read own jar for client distribution", e);
			return null;
		}
	}

	private record PendingUpdate(String version, byte[] jarBytes, String sha256) {}
}
