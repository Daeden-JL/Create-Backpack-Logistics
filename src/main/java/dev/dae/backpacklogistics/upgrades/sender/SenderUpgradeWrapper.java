package dev.dae.backpacklogistics.upgrades.sender;

import com.simibubi.create.content.logistics.box.PackageItem;
import de.theidler.create_mobile_packages.robo.RoboManager;
import de.theidler.create_mobile_packages.robo.RoboTrashStore;
import dev.dae.backpacklogistics.init.ModDataComponents;
import dev.dae.backpacklogistics.util.PackageAddressHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;

/**
 * Watches the backpack for filtered items above a threshold and hands the excess to
 * Create Mobile Packages' outbox (robo trash store). A robo bee then picks the package
 * up from the player and flies it to the bee port matching the configured address.
 */
public class SenderUpgradeWrapper extends UpgradeWrapperBase<SenderUpgradeWrapper, SenderUpgradeItem>
		implements ITickableUpgrade, IFilteredUpgrade {
	private static final int CHECK_INTERVAL_TICKS = 100;
	private static final int MAX_STACKS_PER_SEND = 9;
	public static final int DEFAULT_LOWER_THRESHOLD = 64;
	public static final int DEFAULT_UPPER_THRESHOLD = 128;
	public static final int MAX_THRESHOLD = 9999;

	private final FilterLogic filterLogic;

	public SenderUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
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

	/** Items are kept up to this amount; sending trims the count back down to it. */
	public int getLowerThreshold() {
		return upgrade.getOrDefault(ModDataComponents.LOWER_THRESHOLD.get(), DEFAULT_LOWER_THRESHOLD);
	}

	public void setLowerThreshold(int lowerThreshold) {
		int clamped = Mth.clamp(lowerThreshold, 0, MAX_THRESHOLD);
		upgrade.set(ModDataComponents.LOWER_THRESHOLD.get(), clamped);
		if (getUpperThreshold() < clamped) {
			upgrade.set(ModDataComponents.UPPER_THRESHOLD.get(), clamped);
		}
		save();
	}

	/** Sending starts once the count rises above this amount. */
	public int getUpperThreshold() {
		return upgrade.getOrDefault(ModDataComponents.UPPER_THRESHOLD.get(), DEFAULT_UPPER_THRESHOLD);
	}

	public void setUpperThreshold(int upperThreshold) {
		int clamped = Mth.clamp(upperThreshold, 0, MAX_THRESHOLD);
		upgrade.set(ModDataComponents.UPPER_THRESHOLD.get(), clamped);
		if (getLowerThreshold() > clamped) {
			upgrade.set(ModDataComponents.LOWER_THRESHOLD.get(), clamped);
		}
		save();
	}

	public String getTargetAddress() {
		return upgrade.getOrDefault(ModDataComponents.TARGET_ADDRESS.get(), "");
	}

	public void setTargetAddress(String address) {
		if (address.isBlank()) {
			upgrade.remove(ModDataComponents.TARGET_ADDRESS.get());
		} else {
			upgrade.set(ModDataComponents.TARGET_ADDRESS.get(), address);
		}
		save();
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (level.isClientSide() || isInCooldown(level) || !isEnabled()) {
			return;
		}
		setCooldown(level, CHECK_INTERVAL_TICKS);

		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		String address = getTargetAddress();
		if (address.isBlank()) {
			return;
		}

		ServerPlayer player = PackageAddressHelper.resolveCarrierPlayer(entity);
		if (player != null) {
			sendViaBeePickup(serverLevel, player, address);
		} else {
			sendViaAdjacentPort(address);
		}
	}

	/** Carried backpack: queue excess in the player's CMP outbox; a robo bee collects it. */
	private void sendViaBeePickup(ServerLevel serverLevel, ServerPlayer player, String address) {
		UUID networkId = getLinkedNetwork();
		if (networkId == null) {
			return;
		}

		RoboManager roboManager = RoboManager.get(serverLevel);
		RoboTrashStore outbox = roboManager.getTrashStore(networkId, player.getUUID());
		if (outbox != null && outbox.hasItems()) {
			// previous batch (or the player's own outbox items) not picked up yet
			return;
		}

		List<ItemStack> toSend = collectExcess();
		if (toSend.isEmpty()) {
			return;
		}

		roboManager.setTrashTargetAddress(networkId, player.getUUID(), address);
		roboManager.setTrashSlots(serverLevel, networkId, player.getUUID(), toSend);
	}

	/**
	 * Placed backpack: box the excess into an addressed package inside the backpack.
	 * A Bee Port next to the backpack pulls packages from adjacent inventories and ships them.
	 */
	private void sendViaAdjacentPort(String address) {
		IItemHandler backpackInventory = storageWrapper.getInventoryForUpgradeProcessing();
		// don't queue another package while one is still waiting for pickup
		for (int slot = 0; slot < backpackInventory.getSlots(); slot++) {
			ItemStack stack = backpackInventory.getStackInSlot(slot);
			if (PackageItem.isPackage(stack) && PackageItem.matchAddress(stack, address)) {
				return;
			}
		}

		List<ItemStack> toSend = collectExcess();
		if (toSend.isEmpty()) {
			return;
		}

		ItemStack box = PackageItem.containing(toSend);
		PackageItem.addAddress(box, address);
		ItemStack remainder = ItemHandlerHelper.insertItemStacked(backpackInventory, box, false);
		if (!remainder.isEmpty()) {
			// no room for the package; put the items back
			for (ItemStack stack : toSend) {
				ItemHandlerHelper.insertItemStacked(backpackInventory, stack, false);
			}
		}
	}

	private List<ItemStack> collectExcess() {
		List<ItemStack> toSend = new ArrayList<>();
		IItemHandler backpackInventory = storageWrapper.getInventoryForUpgradeProcessing();
		int lowerThreshold = getLowerThreshold();
		int upperThreshold = Math.max(getUpperThreshold(), lowerThreshold);
		var filterHandler = filterLogic.getFilterHandler();
		List<ItemStack> alreadyHandled = new ArrayList<>();

		for (int slot = 0; slot < filterHandler.getSlots(); slot++) {
			ItemStack target = filterHandler.getStackInSlot(slot);
			if (target.isEmpty() || containsSameItem(alreadyHandled, target)) {
				continue;
			}
			alreadyHandled.add(target);

			int have = countMatching(backpackInventory, target);

			// once stock rises above the upper bound, keep draining every check until the lower
			// bound is actually reached - otherwise a send that undershoots (the 9-stack batch
			// cap, a slot that can't extract cleanly) strands the item between the bounds forever
			boolean draining = isSlotDraining(slot);
			if (have > upperThreshold) {
				draining = true;
			} else if (have <= lowerThreshold) {
				draining = false;
			}
			if (draining != isSlotDraining(slot)) {
				setSlotDraining(slot, draining);
			}
			if (!draining) {
				continue;
			}

			int excess = have - lowerThreshold;
			while (excess > 0 && toSend.size() < MAX_STACKS_PER_SEND) {
				int extracted = extractMatching(backpackInventory, target, Math.min(excess, target.getMaxStackSize()), toSend);
				if (extracted <= 0) {
					break;
				}
				excess -= extracted;
			}
			if (toSend.size() >= MAX_STACKS_PER_SEND) {
				break;
			}
		}
		return toSend;
	}

	private static boolean containsSameItem(List<ItemStack> stacks, ItemStack stack) {
		for (ItemStack existing : stacks) {
			if (ItemStack.isSameItemSameComponents(existing, stack)) {
				return true;
			}
		}
		return false;
	}

	private boolean isSlotDraining(int slot) {
		List<Boolean> values = upgrade.get(ModDataComponents.ACTIVE_SLOTS.get());
		return values != null && slot >= 0 && slot < values.size() && Boolean.TRUE.equals(values.get(slot));
	}

	private void setSlotDraining(int slot, boolean draining) {
		List<Boolean> current = upgrade.get(ModDataComponents.ACTIVE_SLOTS.get());
		List<Boolean> values = new ArrayList<>(current != null ? current : List.of());
		while (values.size() <= slot) {
			values.add(Boolean.FALSE);
		}
		values.set(slot, draining);
		upgrade.set(ModDataComponents.ACTIVE_SLOTS.get(), List.copyOf(values));
		save();
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
	 * Extracts up to amount of items matching target from the handler as a single outgoing stack.
	 *
	 * @return how many items were actually extracted
	 */
	private int extractMatching(IItemHandler handler, ItemStack target, int amount, List<ItemStack> out) {
		int remaining = amount;
		ItemStack collected = ItemStack.EMPTY;
		for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, target)) {
				continue;
			}
			ItemStack extracted = handler.extractItem(slot, remaining, false);
			if (extracted.isEmpty()) {
				continue;
			}
			if (collected.isEmpty()) {
				collected = extracted;
			} else {
				collected.grow(extracted.getCount());
			}
			remaining -= extracted.getCount();
		}
		if (collected.isEmpty()) {
			return 0;
		}
		out.add(collected);
		return collected.getCount();
	}
}
