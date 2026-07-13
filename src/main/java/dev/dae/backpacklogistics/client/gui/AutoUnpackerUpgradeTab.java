package dev.dae.backpacklogistics.client.gui;

import dev.dae.backpacklogistics.upgrades.autounpacker.AutoUnpackerUpgradeContainer;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.TextBox;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public class AutoUnpackerUpgradeTab extends UpgradeSettingsTab<AutoUnpackerUpgradeContainer> {
	public AutoUnpackerUpgradeTab(AutoUnpackerUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
		super(upgradeContainer, position, screen,
				Component.translatable("gui.backpack_logistics.upgrades.auto_unpacker"),
				Component.translatable("gui.backpack_logistics.upgrades.auto_unpacker.tooltip"));

		addHideableChild(new TextWidget(new Position(x + 3, y + 24), 96,
				() -> Component.translatable("gui.backpack_logistics.upgrades.auto_unpacker.player_packages")));
		addHideableChild(new TextWidget(new Position(x + 3, y + 38), 96,
				() -> Component.translatable("gui.backpack_logistics.upgrades.auto_unpacker.extra_address")));

		TextBox addressBox = new TextBox(new Position(x + 3, y + 49), new Dimension(96, 14)) {
			@Override
			protected void onEnterPressed() {
				setFocused(false);
			}
		};
		addressBox.setMaxLength(48);
		addressBox.setValueWithoutNotification(upgradeContainer.getUnpackAddress());
		addressBox.setUnfocusedEmptyHint("-");
		addressBox.setResponder(upgradeContainer::setUnpackAddress);
		addHideableChild(addressBox);
	}

	@Override
	protected void moveSlotsToTab() {
		//no slots
	}
}
