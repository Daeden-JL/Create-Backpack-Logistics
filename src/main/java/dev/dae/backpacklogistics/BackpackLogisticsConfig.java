package dev.dae.backpacklogistics;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BackpackLogisticsConfig {
	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.BooleanValue CHECK_FOR_UPDATES;
	public static final ModConfigSpec.BooleanValue AUTO_DOWNLOAD_UPDATES;
	public static final ModConfigSpec.BooleanValue ACCEPT_UPDATES_FROM_SERVER;

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		builder.push("updates");
		CHECK_FOR_UPDATES = builder
				.comment("Whether dedicated servers check GitHub for new releases of this mod on startup")
				.define("checkForUpdates", true);
		AUTO_DOWNLOAD_UPDATES = builder
				.comment("Whether a found update is automatically downloaded into the mods folder (applies on restart).",
						"Only happens when every dependency the new release requires is already installed in a matching version.")
				.define("autoDownloadUpdates", true);
		ACCEPT_UPDATES_FROM_SERVER = builder
				.comment("Whether this client accepts a newer version of the mod pushed by the server it joins (applies on restart)")
				.define("acceptUpdatesFromServer", true);
		builder.pop();
		SPEC = builder.build();
	}

	private BackpackLogisticsConfig() {}
}
