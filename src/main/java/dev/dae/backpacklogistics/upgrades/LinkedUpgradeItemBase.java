package dev.dae.backpacklogistics.upgrades;

import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.dae.backpacklogistics.init.ModDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;

/**
 * Base for upgrades that tune to a Create logistics network by right-clicking a
 * logistically linked block (Stock Link, Stock Ticker, ...). Sneak-use in the air unlinks.
 */
public abstract class LinkedUpgradeItemBase<W extends IUpgradeWrapper> extends UpgradeItemBase<W> {
	protected LinkedUpgradeItemBase(IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		LogisticallyLinkedBehaviour link = BlockEntityBehaviour.get(level, pos, LogisticallyLinkedBehaviour.TYPE);
		if (link == null) {
			return super.useOn(context);
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!link.mayInteractMessage(player)) {
			return InteractionResult.SUCCESS;
		}
		context.getItemInHand().set(ModDataComponents.LINKED_NETWORK.get(), link.freqId);
		player.displayClientMessage(Component.translatable("item.backpack_logistics.linked_upgrade.linked"), true);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isSecondaryUseActive() && stack.has(ModDataComponents.LINKED_NETWORK.get())) {
			if (!level.isClientSide()) {
				stack.remove(ModDataComponents.LINKED_NETWORK.get());
				player.displayClientMessage(Component.translatable("item.backpack_logistics.linked_upgrade.unlinked"), true);
			}
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
		}
		return super.use(level, player, hand);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		super.appendHoverText(stack, context, tooltip, flagIn);
		if (stack.has(ModDataComponents.LINKED_NETWORK.get())) {
			tooltip.add(Component.translatable("item.backpack_logistics.linked_upgrade.tooltip_linked").withStyle(ChatFormatting.GOLD));
		} else {
			tooltip.add(Component.translatable("item.backpack_logistics.linked_upgrade.tooltip_not_linked").withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return super.isFoil(stack) || stack.has(ModDataComponents.LINKED_NETWORK.get());
	}
}
