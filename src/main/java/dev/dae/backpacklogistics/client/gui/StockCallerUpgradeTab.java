package dev.dae.backpacklogistics.client.gui;

import dev.dae.backpacklogistics.upgrades.stockcaller.StockCallerUpgradeContainer;
import dev.dae.backpacklogistics.upgrades.stockcaller.StockCallerUpgradeWrapper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
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
	/** Which filter slot the threshold selectors below are editing (0-based). */
	private int selectedSlot = 0;

	public StockCallerUpgradeTab(StockCallerUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen,
			String upgradeName, int slotsPerRow) {
		super(upgradeContainer, position, screen,
				Component.translatable("gui.backpack_logistics.upgrades." + upgradeName),
				Component.translatable("gui.backpack_logistics.upgrades." + upgradeName + ".tooltip"));

		int slotCount = upgradeContainer.getFilterSlotCount();
		boolean perSlotSelector = slotCount > 1;
		int yOffset = 24;

		addHideableChild(new TextWidget(new Position(x + 3, y + yOffset), 96, () -> upgradeContainer.isLinked()
				? Component.translatable("gui.backpack_logistics.upgrades.stock_caller.linked").withStyle(ChatFormatting.DARK_GREEN)
				: Component.translatable("gui.backpack_logistics.upgrades.stock_caller.not_linked").withStyle(ChatFormatting.DARK_RED)));
		yOffset += 12;

		if (perSlotSelector) {
			addHideableChild(new NumberSelector(new Position(x + 3, y + yOffset), 96,
					"gui.backpack_logistics.upgrades.stock_caller.slot",
					() -> selectedSlot + 1, value -> selectedSlot = value - 1, 1, slotCount));
			yOffset += 20;
		}

		addHideableChild(new TextWidget(new Position(x + 3, y + yOffset), 96, () -> {
			ItemStack filterStack = upgradeContainer.getFilterStack(selectedSlot);
			return filterStack.isEmpty()
					? Component.translatable("gui.backpack_logistics.upgrades.stock_caller.slot_empty").withStyle(ChatFormatting.GRAY)
					: filterStack.getHoverName().copy().withStyle(ChatFormatting.DARK_AQUA);
		}));
		yOffset += 12;

		addHideableChild(new NumberSelector(new Position(x + 3, y + yOffset), 96,
				"gui.backpack_logistics.upgrades.stock_caller.lower_threshold",
				() -> upgradeContainer.getLowerThreshold(selectedSlot),
				value -> upgradeContainer.setLowerThreshold(selectedSlot, value),
				1, StockCallerUpgradeWrapper.MAX_THRESHOLD));
		yOffset += 20;

		addHideableChild(new NumberSelector(new Position(x + 3, y + yOffset), 96,
				"gui.backpack_logistics.upgrades.stock_caller.upper_threshold",
				() -> upgradeContainer.getUpperThreshold(selectedSlot),
				value -> upgradeContainer.setUpperThreshold(selectedSlot, value),
				1, StockCallerUpgradeWrapper.MAX_THRESHOLD));
		yOffset += 22;

		addHideableChild(new TextWidget(new Position(x + 3, y + yOffset), 96,
				() -> Component.translatable("gui.backpack_logistics.upgrades.stock_caller.address_label")));
		yOffset += 11;

		TextBox addressBox = new TextBox(new Position(x + 3, y + yOffset), new Dimension(96, 14)) {
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
		yOffset += 18;

		filterLogicControl = addHideableChild(new FilterLogicControl<>(screen, new Position(x + 3, y + yOffset),
				upgradeContainer.getFilterLogicContainer(), slotsPerRow));
	}

	@Override
	protected void moveSlotsToTab() {
		filterLogicControl.moveSlotsToView();
	}
}
