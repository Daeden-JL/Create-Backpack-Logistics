package dev.dae.backpacklogistics.client.gui;

import dev.dae.backpacklogistics.upgrades.stockcaller.StockCallerUpgradeContainer;
import dev.dae.backpacklogistics.upgrades.stockcaller.StockCallerUpgradeWrapper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.TextBox;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicControl;

public class StockCallerUpgradeTab extends UpgradeSettingsTab<StockCallerUpgradeContainer> {
	private final FilterLogicControl<FilterLogic, FilterLogicContainer<FilterLogic>> filterLogicControl;

	public StockCallerUpgradeTab(StockCallerUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen,
			String upgradeName, int slotsPerRow) {
		super(upgradeContainer, position, screen,
				Component.translatable("gui.backpack_logistics.upgrades." + upgradeName),
				Component.translatable("gui.backpack_logistics.upgrades." + upgradeName + ".tooltip"));

		addHideableChild(new TextWidget(new Position(x + 3, y + 24), 96, () -> upgradeContainer.isLinked()
				? Component.translatable("gui.backpack_logistics.upgrades.stock_caller.linked").withStyle(ChatFormatting.DARK_GREEN)
				: Component.translatable("gui.backpack_logistics.upgrades.stock_caller.not_linked").withStyle(ChatFormatting.DARK_RED)));

		addHideableChild(new NumberSelector(new Position(x + 3, y + 36), 96,
				"gui.backpack_logistics.upgrades.stock_caller.lower_threshold",
				upgradeContainer::getLowerThreshold, upgradeContainer::setLowerThreshold, 1, StockCallerUpgradeWrapper.MAX_THRESHOLD));

		addHideableChild(new NumberSelector(new Position(x + 3, y + 56), 96,
				"gui.backpack_logistics.upgrades.stock_caller.upper_threshold",
				upgradeContainer::getUpperThreshold, upgradeContainer::setUpperThreshold, 1, StockCallerUpgradeWrapper.MAX_THRESHOLD));

		addHideableChild(new TextWidget(new Position(x + 3, y + 78), 96,
				() -> Component.translatable("gui.backpack_logistics.upgrades.stock_caller.address_label")));

		TextBox addressBox = new TextBox(new Position(x + 3, y + 89), new Dimension(96, 14)) {
			@Override
			protected void onEnterPressed() {
				setFocused(false);
			}
		};
		addressBox.setMaxLength(48);
		addressBox.setValueWithoutNotification(upgradeContainer.getDeliveryAddress());
		addressBox.setUnfocusedEmptyHint("-");
		addressBox.setResponder(upgradeContainer::setDeliveryAddress);
		addHideableChild(addressBox);

		addHideableChild(new TextWidget(new Position(x + 3, y + 108), 96,
				() -> Component.translatable("gui.backpack_logistics.upgrades.stock_caller.filter_label")));

		filterLogicControl = addHideableChild(new FilterLogicControl<>(screen, new Position(x + 3, y + 120),
				upgradeContainer.getFilterLogicContainer(), slotsPerRow));
	}

	@Override
	protected void moveSlotsToTab() {
		filterLogicControl.moveSlotsToView();
	}
}
