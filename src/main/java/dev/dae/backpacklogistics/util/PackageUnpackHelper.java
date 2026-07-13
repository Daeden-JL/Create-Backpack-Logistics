package dev.dae.backpacklogistics.util;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.item.ItemHelper;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;

/**
 * Shared logic for unboxing Create packages into a backpack, used by the Auto-Unpacker
 * and Stock Caller upgrades.
 */
public class PackageUnpackHelper {
	private PackageUnpackHelper() {}

	/**
	 * Unpacks all packages addressed to the player (or matching customAddress) found in the
	 * backpack and, when a player is present, the player's inventory. Plays a soft pickup
	 * sound when anything was unboxed.
	 *
	 * @return true when at least one package was fully unpacked
	 */
	public static boolean unpackTargetedPackages(@Nullable Player player, IStorageWrapper storageWrapper, String customAddress, Level level, BlockPos soundPos) {
		ITrackedContentsItemHandler backpackInventory = storageWrapper.getInventoryForUpgradeProcessing();
		boolean unpackedAny = false;

		for (int slot = 0; slot < backpackInventory.getSlots(); slot++) {
			ItemStack stack = backpackInventory.getStackInSlot(slot);
			if (isTargetedPackage(stack, player, customAddress)) {
				unpackedAny |= unpackFromBackpackSlot(backpackInventory, slot, player);
			}
		}

		if (player != null) {
			Inventory playerInventory = player.getInventory();
			for (int slot = 0; slot < playerInventory.getContainerSize(); slot++) {
				ItemStack stack = playerInventory.getItem(slot);
				if (!isTargetedPackage(stack, player, customAddress)) {
					continue;
				}
				if (unpackPackageInto(stack, backpackInventory)) {
					stack.shrink(1);
					if (stack.isEmpty()) {
						playerInventory.setItem(slot, ItemStack.EMPTY);
					}
					playerInventory.setChanged();
					unpackedAny = true;
				}
			}
		}

		if (unpackedAny) {
			level.playSound(null, player != null ? player.blockPosition() : soundPos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 0.9F);
		}
		return unpackedAny;
	}

	private static boolean isTargetedPackage(ItemStack stack, @Nullable Player player, String customAddress) {
		if (!PackageItem.isPackage(stack)) {
			return false;
		}
		String packageAddress = PackageItem.getAddress(stack);
		if (packageAddress.isBlank()) {
			return false;
		}
		if (player != null && PackageAddressHelper.addressMatchesPlayer(packageAddress, player)) {
			return true;
		}
		return !customAddress.isBlank() && PackageItem.matchAddress(packageAddress, customAddress);
	}

	private static boolean unpackFromBackpackSlot(ITrackedContentsItemHandler backpackInventory, int slot, @Nullable Player player) {
		ItemStack box = backpackInventory.extractItem(slot, 1, false);
		if (box.isEmpty()) {
			return false;
		}
		boolean fullyUnpacked = unpackPackageInto(box, backpackInventory);
		if (!fullyUnpacked) {
			ItemStack remainder = backpackInventory.insertItem(slot, box, false);
			if (!remainder.isEmpty()) {
				remainder = ItemHandlerHelper.insertItemStacked(backpackInventory, remainder, false);
			}
			if (!remainder.isEmpty()) {
				if (player != null) {
					ItemHandlerHelper.giveItemToPlayer(player, remainder);
				} else {
					// could not fit the leftover package back; as a last resort leave it in the slot it came from
					backpackInventory.insertItem(slot, remainder, false);
				}
			}
		}
		return fullyUnpacked;
	}

	private static boolean unpackPackageInto(ItemStack box, IItemHandler destination) {
		ItemStackHandler contents = PackageItem.getContents(box);
		boolean fullyUnpacked = true;
		boolean anyMoved = false;
		for (int slot = 0; slot < contents.getSlots(); slot++) {
			ItemStack contentStack = contents.getStackInSlot(slot);
			if (contentStack.isEmpty()) {
				continue;
			}
			ItemStack remainder = ItemHandlerHelper.insertItemStacked(destination, contentStack, false);
			if (remainder.getCount() != contentStack.getCount()) {
				anyMoved = true;
			}
			contents.setStackInSlot(slot, remainder);
			if (!remainder.isEmpty()) {
				fullyUnpacked = false;
			}
		}
		if (!fullyUnpacked && anyMoved) {
			box.set(AllDataComponents.PACKAGE_CONTENTS, ItemHelper.containerContentsFromHandler(contents));
		}
		return fullyUnpacked;
	}
}
