package dev.dae.backpacklogistics.upgrades.autounpacker;

import dev.dae.backpacklogistics.init.ModDataComponents;
import dev.dae.backpacklogistics.util.PackageAddressHelper;
import dev.dae.backpacklogistics.util.PackageUnpackHelper;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;

public class AutoUnpackerUpgradeWrapper extends UpgradeWrapperBase<AutoUnpackerUpgradeWrapper, AutoUnpackerUpgradeItem> implements ITickableUpgrade {
	private static final int COOLDOWN_TICKS = 10;

	public AutoUnpackerUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
	}

	public String getUnpackAddress() {
		return upgrade.getOrDefault(ModDataComponents.UNPACK_ADDRESS.get(), "");
	}

	public void setUnpackAddress(String address) {
		if (address.isBlank()) {
			upgrade.remove(ModDataComponents.UNPACK_ADDRESS.get());
		} else {
			upgrade.set(ModDataComponents.UNPACK_ADDRESS.get(), address);
		}
		save();
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (level.isClientSide() || isInCooldown(level) || !isEnabled()) {
			return;
		}
		setCooldown(level, COOLDOWN_TICKS);
		Player player = entity instanceof Player p ? p : PackageAddressHelper.resolveCarrierPlayer(entity);
		PackageUnpackHelper.unpackTargetedPackages(player, storageWrapper, getUnpackAddress(), level, pos);
	}
}
