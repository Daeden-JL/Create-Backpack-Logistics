package dev.dae.backpacklogistics.init;

import dev.dae.backpacklogistics.BackpackLogistics;
import dev.dae.backpacklogistics.upgrades.autounpacker.AutoUnpackerUpgradeContainer;
import dev.dae.backpacklogistics.upgrades.autounpacker.AutoUnpackerUpgradeItem;
import dev.dae.backpacklogistics.upgrades.autounpacker.AutoUnpackerUpgradeWrapper;
import dev.dae.backpacklogistics.upgrades.sender.SenderUpgradeContainer;
import dev.dae.backpacklogistics.upgrades.sender.SenderUpgradeItem;
import dev.dae.backpacklogistics.upgrades.sender.SenderUpgradeWrapper;
import dev.dae.backpacklogistics.upgrades.stockcaller.StockCallerUpgradeContainer;
import dev.dae.backpacklogistics.upgrades.stockcaller.StockCallerUpgradeItem;
import dev.dae.backpacklogistics.upgrades.stockcaller.StockCallerUpgradeWrapper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;

public class ModItems {
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BackpackLogistics.MOD_ID);
	private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BackpackLogistics.MOD_ID);

	public static final DeferredItem<AutoUnpackerUpgradeItem> AUTO_UNPACKER_UPGRADE = ITEMS.register(
			"auto_unpacker_upgrade", AutoUnpackerUpgradeItem::new);
	public static final DeferredItem<StockCallerUpgradeItem> STOCK_CALLER_UPGRADE = ITEMS.register(
			"stock_caller_upgrade", () -> new StockCallerUpgradeItem(() -> 1));
	public static final DeferredItem<StockCallerUpgradeItem> ADVANCED_STOCK_CALLER_UPGRADE = ITEMS.register(
			"advanced_stock_caller_upgrade", () -> new StockCallerUpgradeItem(() -> 9));
	public static final DeferredItem<SenderUpgradeItem> SENDER_UPGRADE = ITEMS.register(
			"sender_upgrade", () -> new SenderUpgradeItem(() -> 1));
	public static final DeferredItem<SenderUpgradeItem> ADVANCED_SENDER_UPGRADE = ITEMS.register(
			"advanced_sender_upgrade", () -> new SenderUpgradeItem(() -> 9));

	public static final UpgradeContainerType<AutoUnpackerUpgradeWrapper, AutoUnpackerUpgradeContainer> AUTO_UNPACKER_TYPE =
			new UpgradeContainerType<>(AutoUnpackerUpgradeContainer::new);
	public static final UpgradeContainerType<StockCallerUpgradeWrapper, StockCallerUpgradeContainer> STOCK_CALLER_TYPE =
			new UpgradeContainerType<>(StockCallerUpgradeContainer::new);
	public static final UpgradeContainerType<StockCallerUpgradeWrapper, StockCallerUpgradeContainer> ADVANCED_STOCK_CALLER_TYPE =
			new UpgradeContainerType<>(StockCallerUpgradeContainer::new);
	public static final UpgradeContainerType<SenderUpgradeWrapper, SenderUpgradeContainer> SENDER_TYPE =
			new UpgradeContainerType<>(SenderUpgradeContainer::new);
	public static final UpgradeContainerType<SenderUpgradeWrapper, SenderUpgradeContainer> ADVANCED_SENDER_TYPE =
			new UpgradeContainerType<>(SenderUpgradeContainer::new);

	@SuppressWarnings("unused")
	private static final java.util.function.Supplier<CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register("main",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup." + BackpackLogistics.MOD_ID))
					.icon(() -> new ItemStack(STOCK_CALLER_UPGRADE.get()))
					.displayItems((parameters, output) -> {
						output.accept(AUTO_UNPACKER_UPGRADE.get());
						output.accept(STOCK_CALLER_UPGRADE.get());
						output.accept(ADVANCED_STOCK_CALLER_UPGRADE.get());
						output.accept(SENDER_UPGRADE.get());
						output.accept(ADVANCED_SENDER_UPGRADE.get());
					})
					.build());

	private ModItems() {}

	public static void registerHandlers(IEventBus modBus) {
		ITEMS.register(modBus);
		CREATIVE_MODE_TABS.register(modBus);
		ModDataComponents.register(modBus);
		modBus.addListener(ModItems::registerContainers);
	}

	private static void registerContainers(RegisterEvent event) {
		if (event.getRegistryKey().equals(Registries.MENU)) {
			UpgradeContainerRegistry.register(AUTO_UNPACKER_UPGRADE.getId(), AUTO_UNPACKER_TYPE);
			UpgradeContainerRegistry.register(STOCK_CALLER_UPGRADE.getId(), STOCK_CALLER_TYPE);
			UpgradeContainerRegistry.register(ADVANCED_STOCK_CALLER_UPGRADE.getId(), ADVANCED_STOCK_CALLER_TYPE);
			UpgradeContainerRegistry.register(SENDER_UPGRADE.getId(), SENDER_TYPE);
			UpgradeContainerRegistry.register(ADVANCED_SENDER_UPGRADE.getId(), ADVANCED_SENDER_TYPE);
		}
	}
}
