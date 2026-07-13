package dev.dae.backpacklogistics.compat.jei;

import dev.dae.backpacklogistics.BackpackLogistics;
import dev.dae.backpacklogistics.init.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The crafting recipes show up in JEI automatically (they are plain shaped recipes);
 * this plugin adds the "information" pages describing how each module works.
 */
@JeiPlugin
public class BackpackLogisticsJeiPlugin implements IModPlugin {
	private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(BackpackLogistics.MOD_ID, "main");

	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addIngredientInfo(ModItems.AUTO_UNPACKER_UPGRADE.get(),
				Component.translatable("info.backpack_logistics.auto_unpacker"));
		registration.addIngredientInfo(ModItems.STOCK_CALLER_UPGRADE.get(),
				Component.translatable("info.backpack_logistics.stock_caller"));
		registration.addIngredientInfo(ModItems.ADVANCED_STOCK_CALLER_UPGRADE.get(),
				Component.translatable("info.backpack_logistics.advanced_stock_caller"));
		registration.addIngredientInfo(ModItems.SENDER_UPGRADE.get(),
				Component.translatable("info.backpack_logistics.sender"));
		registration.addIngredientInfo(ModItems.ADVANCED_SENDER_UPGRADE.get(),
				Component.translatable("info.backpack_logistics.advanced_sender"));
	}
}
