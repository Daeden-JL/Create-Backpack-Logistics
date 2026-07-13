package dev.dae.backpacklogistics.compat.rei;

import dev.dae.backpacklogistics.init.ModItems;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.displays.DefaultInformationDisplay;
import net.minecraft.network.chat.Component;

/**
 * The crafting recipes show up in REI automatically (they are plain shaped recipes);
 * this plugin adds the "information" pages describing how each module works.
 * Only loaded when REI is installed.
 */
@REIPluginClient
public class BackpackLogisticsReiPlugin implements REIClientPlugin {
	@Override
	public void registerDisplays(DisplayRegistry registry) {
		registry.add(DefaultInformationDisplay
				.createFromEntry(EntryStacks.of(ModItems.AUTO_UNPACKER_UPGRADE.get()),
						Component.translatable("item.backpack_logistics.auto_unpacker_upgrade"))
				.line(Component.translatable("info.backpack_logistics.auto_unpacker")));
		registry.add(DefaultInformationDisplay
				.createFromEntry(EntryStacks.of(ModItems.STOCK_CALLER_UPGRADE.get()),
						Component.translatable("item.backpack_logistics.stock_caller_upgrade"))
				.line(Component.translatable("info.backpack_logistics.stock_caller")));
		registry.add(DefaultInformationDisplay
				.createFromEntry(EntryStacks.of(ModItems.ADVANCED_STOCK_CALLER_UPGRADE.get()),
						Component.translatable("item.backpack_logistics.advanced_stock_caller_upgrade"))
				.line(Component.translatable("info.backpack_logistics.advanced_stock_caller")));
		registry.add(DefaultInformationDisplay
				.createFromEntry(EntryStacks.of(ModItems.SENDER_UPGRADE.get()),
						Component.translatable("item.backpack_logistics.sender_upgrade"))
				.line(Component.translatable("info.backpack_logistics.sender")));
		registry.add(DefaultInformationDisplay
				.createFromEntry(EntryStacks.of(ModItems.ADVANCED_SENDER_UPGRADE.get()),
						Component.translatable("item.backpack_logistics.advanced_sender_upgrade"))
				.line(Component.translatable("info.backpack_logistics.advanced_sender")));
	}
}
