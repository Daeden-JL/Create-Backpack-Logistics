package dev.dae.backpacklogistics.util;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class PackageAddressHelper {
	private PackageAddressHelper() {}

	/**
	 * Mirrors Create Mobile Packages' player address convention: a package matches a player
	 * when its address is exactly the player's name, or ends with "@&lt;player name&gt;".
	 */
	public static boolean addressMatchesPlayer(String address, Player player) {
		String playerName = player.getGameProfile().getName();
		int atIndex = address.lastIndexOf('@');
		return atIndex == -1 ? address.equals(playerName) : address.substring(atIndex + 1).equals(playerName);
	}

	/**
	 * Resolves the player the backpack effectively belongs to for delivery purposes.
	 * For carried backpacks the tick entity IS the player. For backpacks mounted on a
	 * contraption (train etc.), the SB Create integration ticks upgrades with the
	 * contraption entity - in that case a player riding the same contraption acts as
	 * the carrier, so bees deliver to and collect from them.
	 */
	@Nullable
	public static ServerPlayer resolveCarrierPlayer(@Nullable Entity entity) {
		if (entity instanceof ServerPlayer serverPlayer) {
			return serverPlayer;
		}
		if (entity == null) {
			return null;
		}
		for (Entity passenger : entity.getIndirectPassengers()) {
			if (passenger instanceof ServerPlayer serverPlayer) {
				return serverPlayer;
			}
		}
		return null;
	}
}
