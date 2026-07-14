package dev.dae.backpacklogistics.update;

import dev.dae.backpacklogistics.BackpackLogisticsConfig;
import dev.dae.backpacklogistics.init.ModNetwork;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client side of the server-push update: assembles the chunked jar transfer, verifies it
 * and swaps the client's own copy of the mod so the new version loads on the next launch.
 */
public class ClientUpdateReceiver {
	@Nullable
	private static Transfer activeTransfer = null;

	private ClientUpdateReceiver() {}

	/**
	 * Warns the player when the server is still running an OLDER version than this client -
	 * the window after a server auto-downloaded an update but has not restarted yet. New
	 * features (like per-slot thresholds) silently degrade in that window, so say so.
	 */
	public static void onServerVersion(String serverVersion) {
		if (!UpdateUtil.isNewerVersion(UpdateUtil.currentVersion(), serverVersion)) {
			return;
		}
		Component message = Component.translatable("message.backpack_logistics.server_outdated",
				serverVersion, UpdateUtil.currentVersion()).withStyle(ChatFormatting.GOLD);
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> {
			if (minecraft.player != null) {
				minecraft.player.displayClientMessage(message, false);
			}
		});
	}

	public static void onUpdateStart(ModNetwork.UpdateStartPayload payload) {
		if (!BackpackLogisticsConfig.ACCEPT_UPDATES_FROM_SERVER.get()) {
			UpdateUtil.LOGGER.info("Server offered Backpack Logistics {} but acceptUpdatesFromServer is disabled", payload.version());
			activeTransfer = null;
			return;
		}
		if (payload.totalSize() <= 0 || payload.totalSize() > UpdateUtil.MAX_JAR_BYTES || payload.chunkCount() <= 0) {
			UpdateUtil.LOGGER.warn("Rejected update transfer with implausible size {} / {} chunks", payload.totalSize(), payload.chunkCount());
			activeTransfer = null;
			return;
		}
		if (!UpdateUtil.isNewerVersion(payload.version(), UpdateUtil.currentVersion())) {
			activeTransfer = null;
			return;
		}
		activeTransfer = new Transfer(payload.version(), payload.sha256(), new byte[payload.totalSize()], payload.chunkCount());
	}

	public static void onUpdateChunk(ModNetwork.UpdateChunkPayload payload) {
		Transfer transfer = activeTransfer;
		if (transfer == null) {
			return;
		}
		if (!transfer.acceptChunk(payload.index(), payload.data())) {
			UpdateUtil.LOGGER.warn("Discarding malformed update transfer from server");
			activeTransfer = null;
			return;
		}
		if (transfer.isComplete()) {
			activeTransfer = null;
			finishTransfer(transfer);
		}
	}

	private static void finishTransfer(Transfer transfer) {
		byte[] jar = transfer.buffer;
		if (!UpdateUtil.sha256(jar).equalsIgnoreCase(transfer.sha256)) {
			UpdateUtil.LOGGER.warn("Update from server failed checksum verification - discarded");
			return;
		}
		if (!UpdateUtil.isValidModJar(jar, transfer.version)) {
			UpdateUtil.LOGGER.warn("Update from server is not a valid Backpack Logistics {} jar - discarded", transfer.version);
			return;
		}
		Path currentJar = UpdateUtil.currentJarPath();
		if (currentJar == null) {
			UpdateUtil.LOGGER.info("Not running from a jar (dev environment?) - server update ignored");
			return;
		}
		UpdateUtil.SwapResult result = UpdateUtil.swapJar(currentJar, jar, transfer.version);
		String key = switch (result) {
			case APPLIED -> "message.backpack_logistics.client_update_installed";
			case STAGED -> "message.backpack_logistics.update_staged";
			case FAILED -> "message.backpack_logistics.update_failed";
		};
		Component message = Component.translatable(key, transfer.version).withStyle(ChatFormatting.GOLD);
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> {
			if (minecraft.player != null) {
				minecraft.player.displayClientMessage(message, false);
			}
		});
		UpdateUtil.LOGGER.info("Received Backpack Logistics {} from server: {}", transfer.version, result);
	}

	private static class Transfer {
		private final String version;
		private final String sha256;
		private final byte[] buffer;
		private final int expectedChunks;
		private int receivedChunks = 0;
		private int writeOffset = 0;

		private Transfer(String version, String sha256, byte[] buffer, int expectedChunks) {
			this.version = version;
			this.sha256 = sha256;
			this.buffer = buffer;
			this.expectedChunks = expectedChunks;
		}

		private boolean acceptChunk(int index, byte[] data) {
			if (index != receivedChunks || writeOffset + data.length > buffer.length) {
				return false;
			}
			System.arraycopy(data, 0, buffer, writeOffset, data.length);
			writeOffset += data.length;
			receivedChunks++;
			return true;
		}

		private boolean isComplete() {
			return receivedChunks == expectedChunks && writeOffset == buffer.length;
		}
	}
}
