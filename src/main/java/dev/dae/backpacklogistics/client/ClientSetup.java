package dev.dae.backpacklogistics.client;

import dev.dae.backpacklogistics.client.gui.AutoUnpackerUpgradeTab;
import dev.dae.backpacklogistics.client.gui.SenderUpgradeTab;
import dev.dae.backpacklogistics.client.gui.StockCallerUpgradeTab;
import dev.dae.backpacklogistics.upgrades.sender.SenderUpgradeContainer;
import dev.dae.backpacklogistics.upgrades.sender.SenderUpgradeWrapper;
import dev.dae.backpacklogistics.client.ponder.ModPonderPlugin;
import dev.dae.backpacklogistics.init.ModItems;
import dev.dae.backpacklogistics.upgrades.stockcaller.StockCallerUpgradeContainer;
import dev.dae.backpacklogistics.upgrades.stockcaller.StockCallerUpgradeWrapper;
import net.createmod.ponder.foundation.PonderIndex;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;

public class ClientSetup {
	private ClientSetup() {}

	public static void init() {
		PonderIndex.addPlugin(new ModPonderPlugin());
		UpgradeGuiManager.registerTab(ModItems.AUTO_UNPACKER_TYPE, AutoUnpackerUpgradeTab::new);
		UpgradeGuiManager.<StockCallerUpgradeWrapper, StockCallerUpgradeContainer, StockCallerUpgradeTab>registerTab(ModItems.STOCK_CALLER_TYPE,
				(container, position, screen) -> new StockCallerUpgradeTab(container, position, screen, "stock_caller", 1));
		UpgradeGuiManager.<StockCallerUpgradeWrapper, StockCallerUpgradeContainer, StockCallerUpgradeTab>registerTab(ModItems.ADVANCED_STOCK_CALLER_TYPE,
				(container, position, screen) -> new StockCallerUpgradeTab(container, position, screen, "advanced_stock_caller", 3));
		UpgradeGuiManager.<SenderUpgradeWrapper, SenderUpgradeContainer, SenderUpgradeTab>registerTab(ModItems.SENDER_TYPE,
				(container, position, screen) -> new SenderUpgradeTab(container, position, screen, "sender", 1));
		UpgradeGuiManager.<SenderUpgradeWrapper, SenderUpgradeContainer, SenderUpgradeTab>registerTab(ModItems.ADVANCED_SENDER_TYPE,
				(container, position, screen) -> new SenderUpgradeTab(container, position, screen, "advanced_sender", 3));
	}
}
