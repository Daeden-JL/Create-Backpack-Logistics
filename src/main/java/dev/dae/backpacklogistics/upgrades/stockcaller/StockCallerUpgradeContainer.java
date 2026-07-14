package dev.dae.backpacklogistics.upgrades.stockcaller;

import dev.dae.backpacklogistics.init.ModDataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

public class StockCallerUpgradeContainer extends UpgradeContainerBase<StockCallerUpgradeWrapper, StockCallerUpgradeContainer> {
	private static final String DATA_LOWER_THRESHOLD = "lowerThreshold";
	private static final String DATA_UPPER_THRESHOLD = "upperThreshold";
	private static final String DATA_THRESHOLD_SLOT = "thresholdSlot";
	private static final String DATA_DELIVERY_ADDRESS = "deliveryAddress";

	private final FilterLogicContainer<FilterLogic> filterLogicContainer;

	public StockCallerUpgradeContainer(Player player, int upgradeContainerId, StockCallerUpgradeWrapper upgradeWrapper,
			UpgradeContainerType<StockCallerUpgradeWrapper, StockCallerUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
		filterLogicContainer = new FilterLogicContainer<>(() -> this.upgradeWrapper.getFilterLogic(), this, slots::add);
	}

	public FilterLogicContainer<FilterLogic> getFilterLogicContainer() {
		return filterLogicContainer;
	}

	@Override
	public void handlePacket(CompoundTag data) {
		if (data.contains(DATA_LOWER_THRESHOLD)) {
			upgradeWrapper.setLowerThreshold(data.getInt(DATA_THRESHOLD_SLOT), data.getInt(DATA_LOWER_THRESHOLD));
		} else if (data.contains(DATA_UPPER_THRESHOLD)) {
			upgradeWrapper.setUpperThreshold(data.getInt(DATA_THRESHOLD_SLOT), data.getInt(DATA_UPPER_THRESHOLD));
		} else if (data.contains(DATA_DELIVERY_ADDRESS)) {
			upgradeWrapper.setDeliveryAddress(data.getString(DATA_DELIVERY_ADDRESS));
		} else {
			filterLogicContainer.handlePacket(data);
		}
	}

	public int getLowerThreshold(int slot) {
		return upgradeWrapper.getLowerThreshold(slot);
	}

	public void setLowerThreshold(int slot, int lowerThreshold) {
		upgradeWrapper.setLowerThreshold(slot, lowerThreshold);
		sendDataToServer(() -> {
			CompoundTag tag = new CompoundTag();
			tag.putInt(DATA_THRESHOLD_SLOT, slot);
			tag.putInt(DATA_LOWER_THRESHOLD, lowerThreshold);
			return tag;
		});
	}

	public int getUpperThreshold(int slot) {
		return upgradeWrapper.getUpperThreshold(slot);
	}

	public void setUpperThreshold(int slot, int upperThreshold) {
		upgradeWrapper.setUpperThreshold(slot, upperThreshold);
		sendDataToServer(() -> {
			CompoundTag tag = new CompoundTag();
			tag.putInt(DATA_THRESHOLD_SLOT, slot);
			tag.putInt(DATA_UPPER_THRESHOLD, upperThreshold);
			return tag;
		});
	}

	/** The item currently in the given ghost filter slot (client display helper). */
	public net.minecraft.world.item.ItemStack getFilterStack(int slot) {
		var filterSlots = filterLogicContainer.getFilterSlots();
		return slot >= 0 && slot < filterSlots.size() ? filterSlots.get(slot).getItem() : net.minecraft.world.item.ItemStack.EMPTY;
	}

	public int getFilterSlotCount() {
		return filterLogicContainer.getFilterSlots().size();
	}

	public String getDeliveryAddress() {
		return upgradeWrapper.getDeliveryAddress();
	}

	public void setDeliveryAddress(String address) {
		upgradeWrapper.setDeliveryAddress(address);
		sendDataToServer(() -> NBTHelper.putString(new CompoundTag(), DATA_DELIVERY_ADDRESS, address));
	}

	public boolean isLinked() {
		return getUpgradeStack().has(ModDataComponents.LINKED_NETWORK.get());
	}
}
