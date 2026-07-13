package dev.dae.backpacklogistics.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dae.backpacklogistics.BackpackLogisticsConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * Checks the GitHub releases of this mod on dedicated-server startup. When a newer release
 * exists AND all dependencies it declares (dependencies.json release asset) are satisfied by
 * the installed mod list, the new jar is downloaded, verified and swapped into the mods
 * folder to take effect on the next restart. The downloaded jar is also kept in memory so
 * outdated clients that join can be brought up to date immediately (see ServerUpdateSender).
 */
public class GithubUpdateChecker {
	private static final String REPO = "Daeden-JL/Create-Backpack-Logistics";
	private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";

	private GithubUpdateChecker() {}

	public static void checkOnServerStart(MinecraftServer server) {
		if (!server.isDedicatedServer() || !BackpackLogisticsConfig.CHECK_FOR_UPDATES.get()) {
			return;
		}
		CompletableFuture.runAsync(() -> {
			try {
				check(server);
			} catch (Exception e) {
				UpdateUtil.LOGGER.warn("Update check failed", e);
			}
		});
	}

	private static void check(MinecraftServer server) throws IOException, InterruptedException {
		UpdateUtil.LOGGER.info("Checking github.com/{} for Backpack Logistics updates (current version: {})", REPO, UpdateUtil.currentVersion());
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
		HttpResponse<String> response = client.send(apiRequest(LATEST_RELEASE_URL), HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() == 404) {
			UpdateUtil.LOGGER.debug("No releases published yet");
			return;
		}
		if (response.statusCode() != 200) {
			UpdateUtil.LOGGER.warn("GitHub release lookup returned status {}", response.statusCode());
			return;
		}

		JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
		String remoteVersion = release.get("tag_name").getAsString().replaceFirst("^v", "");
		String localVersion = UpdateUtil.currentVersion();
		if (!UpdateUtil.isNewerVersion(remoteVersion, localVersion)) {
			UpdateUtil.LOGGER.info("Backpack Logistics {} is up to date (latest release: {})", localVersion, remoteVersion);
			return;
		}
		UpdateUtil.LOGGER.info("Backpack Logistics update available: {} -> {}", localVersion, remoteVersion);

		JsonArray assets = release.getAsJsonArray("assets");
		JsonObject jarAsset = findAsset(assets, name -> name.endsWith(".jar") && name.startsWith("create_backpack_logistics"));
		if (jarAsset == null) {
			UpdateUtil.LOGGER.warn("Release {} has no mod jar asset - skipping update", remoteVersion);
			return;
		}

		// dependency gate: only auto-update when everything the new release needs is installed
		JsonObject dependenciesAsset = findAsset(assets, name -> name.equals("dependencies.json"));
		if (dependenciesAsset != null) {
			byte[] dependencyBytes = download(client, dependenciesAsset.get("browser_download_url").getAsString(), 1024 * 1024);
			JsonObject dependencies = JsonParser.parseString(new String(dependencyBytes)).getAsJsonObject();
			for (Map.Entry<String, JsonElement> dependency : dependencies.entrySet()) {
				if (!UpdateUtil.isDependencySatisfied(dependency.getKey(), dependency.getValue().getAsString())) {
					UpdateUtil.LOGGER.warn("Update {} NOT downloaded: dependency '{}' {} is not satisfied by the installed mods",
							remoteVersion, dependency.getKey(), dependency.getValue().getAsString());
					return;
				}
			}
		}

		if (!BackpackLogisticsConfig.AUTO_DOWNLOAD_UPDATES.get()) {
			UpdateUtil.LOGGER.info("Auto-download is disabled; get the update at https://github.com/{}/releases", REPO);
			return;
		}

		byte[] jarBytes = download(client, jarAsset.get("browser_download_url").getAsString(), UpdateUtil.MAX_JAR_BYTES);
		String expectedDigest = readSha256Digest(jarAsset);
		if (expectedDigest != null && !expectedDigest.equalsIgnoreCase(UpdateUtil.sha256(jarBytes))) {
			UpdateUtil.LOGGER.warn("Downloaded jar does not match the release digest - discarding");
			return;
		}
		if (!UpdateUtil.isValidModJar(jarBytes, remoteVersion)) {
			UpdateUtil.LOGGER.warn("Downloaded file is not a valid Backpack Logistics {} jar - discarding", remoteVersion);
			return;
		}

		// make the new version available to outdated clients right away
		ServerUpdateSender.setPendingUpdate(remoteVersion, jarBytes);

		Path currentJar = UpdateUtil.currentJarPath();
		if (currentJar == null) {
			UpdateUtil.LOGGER.info("Not running from a jar (dev environment?) - downloaded update kept for client distribution only");
			return;
		}
		UpdateUtil.SwapResult result = UpdateUtil.swapJar(currentJar, jarBytes, remoteVersion);
		server.execute(() -> notifyOperators(server, remoteVersion, result));
		switch (result) {
			case APPLIED -> UpdateUtil.LOGGER.info("Update {} installed - it will take effect on the next server restart", remoteVersion);
			case STAGED -> UpdateUtil.LOGGER.warn("Update {} staged next to the running jar - finish the swap manually", remoteVersion);
			case FAILED -> UpdateUtil.LOGGER.error("Update {} could not be installed", remoteVersion);
		}
	}

	private static void notifyOperators(MinecraftServer server, String version, UpdateUtil.SwapResult result) {
		String key = switch (result) {
			case APPLIED -> "message.backpack_logistics.update_installed";
			case STAGED -> "message.backpack_logistics.update_staged";
			case FAILED -> "message.backpack_logistics.update_failed";
		};
		Component message = Component.translatable(key, version).withStyle(ChatFormatting.GOLD);
		server.getPlayerList().getPlayers().stream()
				.filter(player -> server.getPlayerList().isOp(player.getGameProfile()))
				.forEach(player -> player.sendSystemMessage(message));
	}

	@Nullable
	private static JsonObject findAsset(JsonArray assets, java.util.function.Predicate<String> nameMatches) {
		for (JsonElement element : assets) {
			JsonObject asset = element.getAsJsonObject();
			if (nameMatches.test(asset.get("name").getAsString())) {
				return asset;
			}
		}
		return null;
	}

	@Nullable
	private static String readSha256Digest(JsonObject asset) {
		JsonElement digest = asset.get("digest");
		if (digest == null || digest.isJsonNull()) {
			return null;
		}
		String value = digest.getAsString();
		return value.startsWith("sha256:") ? value.substring("sha256:".length()) : null;
	}

	private static byte[] download(HttpClient client, String url, int maxBytes) throws IOException, InterruptedException {
		HttpResponse<byte[]> response = client.send(apiRequest(url), HttpResponse.BodyHandlers.ofByteArray());
		if (response.statusCode() != 200) {
			throw new IOException("Download of " + url + " returned status " + response.statusCode());
		}
		if (response.body().length > maxBytes) {
			throw new IOException("Download of " + url + " exceeds the size limit");
		}
		return response.body();
	}

	private static HttpRequest apiRequest(String url) {
		return HttpRequest.newBuilder(URI.create(url))
				.header("User-Agent", "backpack-logistics-updater")
				.header("Accept", "application/vnd.github+json")
				.timeout(Duration.ofSeconds(60))
				.GET()
				.build();
	}
}
