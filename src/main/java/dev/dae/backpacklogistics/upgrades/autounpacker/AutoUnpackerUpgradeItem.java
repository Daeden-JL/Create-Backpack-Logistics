package dev.dae.backpacklogistics.upgrades.autounpacker;

import java.util.List;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem.UpgradeConflictDefinition;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public class AutoUnpackerUpgradeItem extends UpgradeItemBase<AutoUnpackerUpgradeWrapper> {
	public static final UpgradeType<AutoUnpackerUpgradeWrapper> TYPE = new UpgradeType<>(AutoUnpackerUpgradeWrapper::new);

	public AutoUnpackerUpgradeItem() {
		super(Config.SERVER.maxUpgradesPerStorage);
	}

	@Override
	public UpgradeType<AutoUnpackerUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}
}
