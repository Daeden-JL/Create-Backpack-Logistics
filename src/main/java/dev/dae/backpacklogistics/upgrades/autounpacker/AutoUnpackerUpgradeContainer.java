package dev.dae.backpacklogistics.upgrades.autounpacker;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

public class AutoUnpackerUpgradeContainer extends UpgradeContainerBase<AutoUnpackerUpgradeWrapper, AutoUnpackerUpgradeContainer> {
	private static final String DATA_UNPACK_ADDRESS = "unpackAddress";

	public AutoUnpackerUpgradeContainer(Player player, int upgradeContainerId, AutoUnpackerUpgradeWrapper upgradeWrapper,
			UpgradeContainerType<AutoUnpackerUpgradeWrapper, AutoUnpackerUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
	}

	@Override
	public void handlePacket(CompoundTag data) {
		if (data.contains(DATA_UNPACK_ADDRESS)) {
			upgradeWrapper.setUnpackAddress(data.getString(DATA_UNPACK_ADDRESS));
		}
	}

	public String getUnpackAddress() {
		return upgradeWrapper.getUnpackAddress();
	}

	public void setUnpackAddress(String address) {
		upgradeWrapper.setUnpackAddress(address);
		sendDataToServer(() -> NBTHelper.putString(new CompoundTag(), DATA_UNPACK_ADDRESS, address));
	}
}
