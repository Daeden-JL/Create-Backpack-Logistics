package dev.dae.backpacklogistics.upgrades.stockcaller;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import dev.dae.backpacklogistics.init.ModDataComponents;
import dev.dae.backpacklogistics.util.PackageAddressHelper;
import dev.dae.backpacklogistics.util.PackageUnpackHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;

/**
 * Keeps filtered items within a stock range: when the count drops below the lower threshold,
 * it orders enough from the linked logistics network to reach the upper threshold. Arriving
 * packages are unpacked into the backpack automatically.
 *
 * Carried by a player, requests are addressed to the player and delivered by robo bee. Placed
 * as a block, requests are addressed to the configured delivery address instead - a Bee Port
 * with a matching address filter next to the backpack will push deliveries into it.
 */
public class StockCallerUpgradeWrapper extends UpgradeWrapperBase<StockCallerUpgradeWrapper, StockCallerUpgradeItem>
		implements ITickableUpgrade, IFilteredUpgrade {
	private static final int UNPACK_INTERVAL_TICKS = 10;
	private static final int CHECK_INTERVAL_TICKS = 100;
	private static final long PROMISE_TIMEOUT_TICKS = 1200;
	public static final int DEFAULT_LOWER_THRESHOLD = 16;
	public static final int DEFAULT_UPPER_THRESHOLD = 64;
	public static final int MAX_THRESHOLD = 9999;

	private final FilterLogic filterLogic;
	private final List<Promise> promises = new ArrayList<>();
	private long nextStockCheckTime = 0;

	public StockCallerUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new FilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount(), ModCoreDataComponents.FILTER_ATTRIBUTES);
	}

	@Override
	public FilterLogic getFilterLogic() {
		return filterLogic;
	}

	@Nullable
	public UUID getLinkedNetwork() {
		return upgrade.get(ModDataComponents.LINKED_NETWORK.get());
	}

	/** Per-slot lower threshold; unset slots fall back to the legacy single value, then the default. */
	public int getLowerThreshold(int slot) {
		int perSlot = getListValue(ModDataComponents.LOWER_THRESHOLDS.get(), slot);
		if (perSlot > 0) {
			return perSlot;
		}
		return upgrade.getOrDefault(ModDataComponents.LOWER_THRESHOLD.get(), DEFAULT_LOWER_THRESHOLD);
	}

	public void setLowerThreshold(int slot, int lowerThreshold) {
		int clamped = Mth.clamp(lowerThreshold, 1, MAX_THRESHOLD);
		setListValue(ModDataComponents.LOWER_THRESHOLDS.get(), slot, clamped);
		if (getUpperThreshold(slot) < clamped) {
			setListValue(ModDataComponents.UPPER_THRESHOLDS.get(), slot, clamped);
		}
		save();
	}

	/** Per-slot upper threshold; unset slots fall back to the legacy single value, then the default. */
	public int getUpperThreshold(int slot) {
		int perSlot = getListValue(ModDataComponents.UPPER_THRESHOLDS.get(), slot);
		if (perSlot > 0) {
			return perSlot;
		}
		return upgrade.getOrDefault(ModDataComponents.UPPER_THRESHOLD.get(), DEFAULT_UPPER_THRESHOLD);
	}

	public void setUpperThreshold(int slot, int upperThreshold) {
		int clamped = Mth.clamp(upperThreshold, 1, MAX_THRESHOLD);
		setListValue(ModDataComponents.UPPER_THRESHOLDS.get(), slot, clamped);
		if (getLowerThreshold(slot) > clamped) {
			setListValue(ModDataComponents.LOWER_THRESHOLDS.get(), slot, clamped);
		}
		save();
	}

	private int getListValue(DataComponentType<List<Integer>> component, int slot) {
		List<Integer> values = upgrade.get(component);
		return values != null && slot >= 0 && slot < values.size() ? values.get(slot) : 0;
	}

	private void setListValue(DataComponentType<List<Integer>> component, int slot, int value) {
		List<Integer> current = upgrade.get(component);
		List<Integer> values = new ArrayList<>(current != null ? current : List.of());
		while (values.size() <= slot) {
			values.add(0);
		}
		values.set(slot, value);
		upgrade.set(component, List.copyOf(values));
	}

	/** Whether the given slot is currently filling toward its upper threshold (see the tick loop). */
	private boolean isSlotFilling(int slot) {
		List<Boolean> values = upgrade.get(ModDataComponents.ACTIVE_SLOTS.get());
		return values != null && slot >= 0 && slot < values.size() && Boolean.TRUE.equals(values.get(slot));
	}

	private void setSlotFilling(int slot, boolean filling) {
		List<Boolean> current = upgrade.get(ModDataComponents.ACTIVE_SLOTS.get());
		List<Boolean> values = new ArrayList<>(current != null ? current : List.of());
		while (values.size() <= slot) {
			values.add(Boolean.FALSE);
		}
		values.set(slot, filling);
		upgrade.set(ModDataComponents.ACTIVE_SLOTS.get(), List.copyOf(values));
		save();
	}

	/** Address used to receive deliveries (and unpack packages) while the backpack is placed as a block. */
	public String getDeliveryAddress() {
		return upgrade.getOrDefault(ModDataComponents.UNPACK_ADDRESS.get(), "");
	}

	public void setDeliveryAddress(String address) {
		if (address.isBlank()) {
			upgrade.remove(ModDataComponents.UNPACK_ADDRESS.get());
		} else {
			upgrade.set(ModDataComponents.UNPACK_ADDRESS.get(), address);
		}
		save();
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (level.isClientSide() || isInCooldown(level) || !isEnabled()) {
			return;
		}
		setCooldown(level, UNPACK_INTERVAL_TICKS);

		ServerPlayer player = PackageAddressHelper.resolveCarrierPlayer(entity);
		String deliveryAddress = getDeliveryAddress();

		// the stock caller includes the auto-unpacker's behaviour
		PackageUnpackHelper.unpackTargetedPackages(player, storageWrapper, deliveryAddress, level, pos);

		if (level.getGameTime() < nextStockCheckTime) {
			return;
		}
		nextStockCheckTime = level.getGameTime() + CHECK_INTERVAL_TICKS;

		UUID networkId = getLinkedNetwork();
		if (networkId == null) {
			return;
		}
		// carried: deliveries are addressed to the player; placed: to the configured address
		String requestAddress = player != null ? player.getGameProfile().getName() : deliveryAddress;
		if (requestAddress.isBlank()) {
			return;
		}

		prunePromises(level.getGameTime());

		// each filter slot has its own stock range; duplicate items are handled by their first slot only
		ItemStackHandler filterHandler = filterLogic.getFilterHandler();
		List<ItemStack> alreadyHandled = new ArrayList<>();
		InventorySummary networkStock = null;

		for (int slot = 0; slot < filterHandler.getSlots(); slot++) {
			ItemStack target = filterHandler.getStackInSlot(slot);
			if (target.isEmpty() || containsSameItem(alreadyHandled, target)) {
				continue;
			}
			alreadyHandled.add(target);

			int lowerThreshold = getLowerThreshold(slot);
			int upperThreshold = Math.max(getUpperThreshold(slot), lowerThreshold);
			int have = countMatching(storageWrapper.getInventoryForUpgradeProcessing(), target)
					+ countInPendingPackages(player, deliveryAddress, target)
					+ getPromisedCount(target);

			// once stock drops below the lower bound, keep ordering every check until the upper
			// bound is actually reached - otherwise a call that undershoots (limited network
			// stock, a busy packager, a partial delivery) strands the slot between the bounds
			// forever, since "have" alone would never dip below the lower bound again to retrigger
			boolean filling = isSlotFilling(slot);
			if (have < lowerThreshold) {
				filling = true;
			} else if (have >= upperThreshold) {
				filling = false;
			}
			if (filling != isSlotFilling(slot)) {
				setSlotFilling(slot, filling);
			}
			if (!filling) {
				continue;
			}

			int toRequest = upperThreshold - have;
			if (networkStock == null) {
				networkStock = LogisticsManager.getSummaryOfNetwork(networkId, true);
			}
			toRequest = Math.min(toRequest, networkStock.getCountOf(target));
			if (toRequest <= 0) {
				continue;
			}

			PackageOrderWithCrafts order = PackageOrderWithCrafts.simple(List.of(new BigItemStack(target.copyWithCount(1), toRequest)));
			if (LogisticsManager.broadcastPackageRequest(networkId, RequestType.RESTOCK, order, null, requestAddress)) {
				promises.add(new Promise(target.copy(), toRequest, level.getGameTime() + PROMISE_TIMEOUT_TICKS));
			}
		}
	}

	private static boolean containsSameItem(List<ItemStack> stacks, ItemStack stack) {
		for (ItemStack existing : stacks) {
			if (ItemStack.isSameItemSameComponents(existing, stack)) {
				return true;
			}
		}
		return false;
	}

	private int countMatching(IItemHandler handler, ItemStack target) {
		int count = 0;
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, target)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/**
	 * Counts items still boxed up in packages meant for this backpack (in the carrier's
	 * inventory and in the backpack itself) so they are not requested a second time.
	 */
	private int countInPendingPackages(@Nullable Player player, String deliveryAddress, ItemStack target) {
		int count = 0;
		if (player != null) {
			Inventory playerInventory = player.getInventory();
			for (int slot = 0; slot < playerInventory.getContainerSize(); slot++) {
				count += countInPackage(playerInventory.getItem(slot), player, deliveryAddress, target);
			}
		}
		IItemHandler backpackInventory = storageWrapper.getInventoryForUpgradeProcessing();
		for (int slot = 0; slot < backpackInventory.getSlots(); slot++) {
			count += countInPackage(backpackInventory.getStackInSlot(slot), player, deliveryAddress, target);
		}
		return count;
	}

	private int countInPackage(ItemStack maybePackage, @Nullable Player player, String deliveryAddress, ItemStack target) {
		if (!PackageItem.isPackage(maybePackage)) {
			return 0;
		}
		String address = PackageItem.getAddress(maybePackage);
		if (address.isBlank()) {
			return 0;
		}
		boolean matchesTarget = player != null && PackageAddressHelper.addressMatchesPlayer(address, player)
				|| !deliveryAddress.isBlank() && PackageItem.matchAddress(address, deliveryAddress);
		if (!matchesTarget) {
			return 0;
		}
		int count = 0;
		ItemStackHandler contents = PackageItem.getContents(maybePackage);
		for (int slot = 0; slot < contents.getSlots(); slot++) {
			ItemStack contentStack = contents.getStackInSlot(slot);
			if (!contentStack.isEmpty() && ItemStack.isSameItemSameComponents(contentStack, target)) {
				count += contentStack.getCount();
			}
		}
		return count * maybePackage.getCount();
	}

	private int getPromisedCount(ItemStack target) {
		int count = 0;
		for (Promise promise : promises) {
			if (ItemStack.isSameItemSameComponents(promise.stack, target)) {
				count += promise.count;
			}
		}
		return count;
	}

	private void prunePromises(long gameTime) {
		promises.removeIf(promise -> promise.expiryGameTime <= gameTime);
	}

	private record Promise(ItemStack stack, int count, long expiryGameTime) {}
}
