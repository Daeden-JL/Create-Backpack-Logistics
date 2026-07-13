package dev.dae.backpacklogistics.update;

import dev.dae.backpacklogistics.BackpackLogistics;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.Nullable;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateUtil {
	public static final Logger LOGGER = LoggerFactory.getLogger("BackpackLogistics/Updater");
	/** Hard cap on how large a downloaded/received jar may be. */
	public static final int MAX_JAR_BYTES = 32 * 1024 * 1024;

	private UpdateUtil() {}

	public static String currentVersion() {
		return ModList.get().getModContainerById(BackpackLogistics.MOD_ID)
				.map(container -> container.getModInfo().getVersion().toString())
				.orElse("0.0.0");
	}

	@Nullable
	public static Path currentJarPath() {
		var modFileInfo = ModList.get().getModFileById(BackpackLogistics.MOD_ID);
		if (modFileInfo == null) {
			return null;
		}
		Path path = modFileInfo.getFile().getFilePath();
		// in dev the "jar" is a build directory - self updating makes no sense there
		return Files.isRegularFile(path) && path.toString().endsWith(".jar") ? path : null;
	}

	public static String sha256(byte[] data) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Sanity-checks that the bytes are a mod jar of THIS mod at the expected version:
	 * a readable zip whose neoforge.mods.toml declares our modId and the expected version.
	 */
	public static boolean isValidModJar(byte[] jarBytes, String expectedVersion) {
		if (jarBytes.length == 0 || jarBytes.length > MAX_JAR_BYTES) {
			return false;
		}
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(jarBytes))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (!entry.getName().equals("META-INF/neoforge.mods.toml")) {
					continue;
				}
				String toml = new String(readLimited(zip, 1024 * 1024), StandardCharsets.UTF_8);
				return toml.contains("modId = \"" + BackpackLogistics.MOD_ID + "\"")
						&& toml.contains("version = \"" + expectedVersion + "\"");
			}
		} catch (IOException e) {
			LOGGER.warn("Downloaded update is not a readable jar", e);
		}
		return false;
	}

	public static byte[] readLimited(InputStream in, int maxBytes) throws IOException {
		byte[] data = in.readNBytes(maxBytes + 1);
		if (data.length > maxBytes) {
			throw new IOException("Stream exceeds size limit of " + maxBytes + " bytes");
		}
		return data;
	}

	public enum SwapResult {
		APPLIED,
		STAGED,
		FAILED
	}

	/**
	 * Replaces the currently loaded jar with the new version. The running jar is deleted
	 * (Java opens mod jars with share-delete semantics, so this works on all platforms in
	 * the common case) and the new jar written next to it. If the old jar cannot be removed,
	 * the new version is left beside it with a .update suffix so the game never sees two
	 * copies of the mod, and the swap has to be finished by hand.
	 */
	public static SwapResult swapJar(Path oldJar, byte[] newJarBytes, String newVersion) {
		Path modsDir = oldJar.getParent();
		Path newJar = modsDir.resolve("create_backpack_logistics-" + newVersion + ".jar");
		Path staging = modsDir.resolve("create_backpack_logistics-" + newVersion + ".jar.update");
		try {
			Files.write(staging, newJarBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			LOGGER.error("Could not write update file {}", staging, e);
			return SwapResult.FAILED;
		}

		try {
			if (!newJar.equals(oldJar)) {
				Files.delete(oldJar);
			}
		} catch (IOException e) {
			LOGGER.warn("Could not remove the running jar {}. The update was saved as {} - swap the files manually after shutting down.",
					oldJar.getFileName(), staging.getFileName(), e);
			return SwapResult.STAGED;
		}

		try {
			Files.move(staging, newJar, StandardCopyOption.REPLACE_EXISTING);
			return SwapResult.APPLIED;
		} catch (IOException e) {
			LOGGER.error("Removed old jar but could not move {} into place - restore it manually!", staging, e);
			return SwapResult.FAILED;
		}
	}

	public static boolean isNewerVersion(String candidate, String reference) {
		return compareVersions(candidate, reference) > 0;
	}

	/** Numeric-aware dotted version comparison; good enough for x.y.z schemes. */
	public static int compareVersions(String a, String b) {
		String[] partsA = a.split("[.\\-+]");
		String[] partsB = b.split("[.\\-+]");
		int length = Math.max(partsA.length, partsB.length);
		for (int i = 0; i < length; i++) {
			String partA = i < partsA.length ? partsA[i] : "0";
			String partB = i < partsB.length ? partsB[i] : "0";
			int result;
			if (partA.matches("\\d+") && partB.matches("\\d+")) {
				result = Long.compare(Long.parseLong(partA), Long.parseLong(partB));
			} else {
				result = partA.compareTo(partB);
			}
			if (result != 0) {
				return result;
			}
		}
		return 0;
	}

	/** True when the installed mod list satisfies a required mod id + maven version range. */
	public static boolean isDependencySatisfied(String modId, String versionRange) {
		return ModList.get().getModContainerById(modId).map(container -> {
			IModInfo info = container.getModInfo();
			try {
				var range = org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec(versionRange);
				return range.containsVersion(info.getVersion());
			} catch (Exception e) {
				LOGGER.warn("Unparseable version range '{}' for dependency {}", versionRange, modId);
				return true;
			}
		}).orElse(false);
	}
}
