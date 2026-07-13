package dev.dae.backpacklogistics.upgrades.sender;

import dev.dae.backpacklogistics.upgrades.LinkedUpgradeItemBase;
import java.util.List;
import java.util.function.IntSupplier;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem.UpgradeConflictDefinition;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public class SenderUpgradeItem extends LinkedUpgradeItemBase<SenderUpgradeWrapper> {
	public static final UpgradeType<SenderUpgradeWrapper> TYPE = new UpgradeType<>(SenderUpgradeWrapper::new);
	private final IntSupplier filterSlotCount;

	public SenderUpgradeItem(IntSupplier filterSlotCount) {
		super(Config.SERVER.maxUpgradesPerStorage);
		this.filterSlotCount = filterSlotCount;
	}

	@Override
	public UpgradeType<SenderUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}

	public int getFilterSlotCount() {
		return filterSlotCount.getAsInt();
	}
}
