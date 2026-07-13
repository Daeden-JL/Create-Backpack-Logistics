package dev.dae.backpacklogistics.upgrades.sender;

import dev.dae.backpacklogistics.init.ModDataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

public class SenderUpgradeContainer extends UpgradeContainerBase<SenderUpgradeWrapper, SenderUpgradeContainer> {
	private static final String DATA_LOWER_THRESHOLD = "lowerThreshold";
	private static final String DATA_UPPER_THRESHOLD = "upperThreshold";
	private static final String DATA_TARGET_ADDRESS = "targetAddress";

	private final FilterLogicContainer<FilterLogic> filterLogicContainer;

	public SenderUpgradeContainer(Player player, int upgradeContainerId, SenderUpgradeWrapper upgradeWrapper,
			UpgradeContainerType<SenderUpgradeWrapper, SenderUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
		filterLogicContainer = new FilterLogicContainer<>(() -> this.upgradeWrapper.getFilterLogic(), this, slots::add);
	}

	public FilterLogicContainer<FilterLogic> getFilterLogicContainer() {
		return filterLogicContainer;
	}

	@Override
	public void handlePacket(CompoundTag data) {
		if (data.contains(DATA_LOWER_THRESHOLD)) {
			upgradeWrapper.setLowerThreshold(data.getInt(DATA_LOWER_THRESHOLD));
		} else if (data.contains(DATA_UPPER_THRESHOLD)) {
			upgradeWrapper.setUpperThreshold(data.getInt(DATA_UPPER_THRESHOLD));
		} else if (data.contains(DATA_TARGET_ADDRESS)) {
			upgradeWrapper.setTargetAddress(data.getString(DATA_TARGET_ADDRESS));
		} else {
			filterLogicContainer.handlePacket(data);
		}
	}

	public int getLowerThreshold() {
		return upgradeWrapper.getLowerThreshold();
	}

	public void setLowerThreshold(int lowerThreshold) {
		upgradeWrapper.setLowerThreshold(lowerThreshold);
		sendDataToServer(() -> {
			CompoundTag tag = new CompoundTag();
			tag.putInt(DATA_LOWER_THRESHOLD, lowerThreshold);
			return tag;
		});
	}

	public int getUpperThreshold() {
		return upgradeWrapper.getUpperThreshold();
	}

	public void setUpperThreshold(int upperThreshold) {
		upgradeWrapper.setUpperThreshold(upperThreshold);
		sendDataToServer(() -> {
			CompoundTag tag = new CompoundTag();
			tag.putInt(DATA_UPPER_THRESHOLD, upperThreshold);
			return tag;
		});
	}

	public String getTargetAddress() {
		return upgradeWrapper.getTargetAddress();
	}

	public void setTargetAddress(String address) {
		upgradeWrapper.setTargetAddress(address);
		sendDataToServer(() -> NBTHelper.putString(new CompoundTag(), DATA_TARGET_ADDRESS, address));
	}

	public boolean isLinked() {
		return getUpgradeStack().has(ModDataComponents.LINKED_NETWORK.get());
	}
}
