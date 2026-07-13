package dev.dae.backpacklogistics.upgrades.stockcaller;

import dev.dae.backpacklogistics.upgrades.LinkedUpgradeItemBase;
import java.util.List;
import java.util.function.IntSupplier;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem.UpgradeConflictDefinition;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public class StockCallerUpgradeItem extends LinkedUpgradeItemBase<StockCallerUpgradeWrapper> {
	public static final UpgradeType<StockCallerUpgradeWrapper> TYPE = new UpgradeType<>(StockCallerUpgradeWrapper::new);
	private final IntSupplier filterSlotCount;

	public StockCallerUpgradeItem(IntSupplier filterSlotCount) {
		super(Config.SERVER.maxUpgradesPerStorage);
		this.filterSlotCount = filterSlotCount;
	}

	@Override
	public UpgradeType<StockCallerUpgradeWrapper> getType() {
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
