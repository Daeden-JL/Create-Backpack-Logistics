package dev.dae.backpacklogistics.client.ponder;

import dev.dae.backpacklogistics.BackpackLogistics;
import dev.dae.backpacklogistics.init.ModItems;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;

public class ModPonderPlugin implements PonderPlugin {
	@Override
	@NotNull
	public String getModId() {
		return BackpackLogistics.MOD_ID;
	}

	@Override
	public void registerScenes(@NotNull PonderSceneRegistrationHelper<ResourceLocation> helper) {
		PonderSceneRegistrationHelper<DeferredItem<?>> itemHelper = helper.withKeyFunction(DeferredHolder::getId);
		itemHelper.forComponents(ModItems.AUTO_UNPACKER_UPGRADE)
				.addStoryBoard("auto_unpacker", ModPonderScenes::autoUnpacker);
		itemHelper.forComponents(ModItems.STOCK_CALLER_UPGRADE, ModItems.ADVANCED_STOCK_CALLER_UPGRADE)
				.addStoryBoard("stock_caller", ModPonderScenes::stockCaller);
		itemHelper.forComponents(ModItems.SENDER_UPGRADE, ModItems.ADVANCED_SENDER_UPGRADE)
				.addStoryBoard("sender", ModPonderScenes::sender);
	}
}
