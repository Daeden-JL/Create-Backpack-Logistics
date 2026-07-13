package dev.dae.backpacklogistics.client.gui;

import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;

/**
 * A compact value selector: left-click +1, right-click -1, mouse wheel adjusts,
 * holding shift steps by 8 and ctrl by 64.
 */
public class NumberSelector extends WidgetBase {
	private final String translLabelKey;
	private final IntSupplier getValue;
	private final IntConsumer setValue;
	private final int minValue;
	private final int maxValue;
	private final List<Component> tooltip;

	public NumberSelector(Position position, int width, String translLabelKey, IntSupplier getValue, IntConsumer setValue, int minValue, int maxValue) {
		super(position, new Dimension(width, 18));
		this.translLabelKey = translLabelKey;
		this.getValue = getValue;
		this.setValue = setValue;
		this.minValue = minValue;
		this.maxValue = maxValue;
		tooltip = List.of(
				Component.translatable(translLabelKey + ".tooltip"),
				Component.translatable("gui.backpack_logistics.number_selector.controls").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		GuiHelper.renderControlBackground(guiGraphics, x, y, getWidth(), getHeight());
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		Component text = Component.translatable(translLabelKey, Component.literal(String.valueOf(getValue.getAsInt())).withStyle(ChatFormatting.WHITE))
				.withStyle(ChatFormatting.GRAY);
		int xOffset = (getWidth() - font.width(text)) / 2;
		int yOffset = (int) Math.ceil((getHeight() - 9) / 2D);
		guiGraphics.drawString(font, text, x + xOffset, y + yOffset, DyeColor.BLACK.getTextColor(), false);
	}

	@Override
	public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
		if (isMouseOver(mouseX, mouseY)) {
			guiGraphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isMouseOver(mouseX, mouseY)) {
			return false;
		}
		if (button == 0) {
			adjust(getStep());
			return true;
		} else if (button == 1) {
			adjust(-getStep());
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		adjust(scrollY > 0 ? getStep() : -getStep());
		return true;
	}

	private int getStep() {
		if (Screen.hasControlDown()) {
			return 64;
		}
		return Screen.hasShiftDown() ? 8 : 1;
	}

	private void adjust(int delta) {
		setValue.accept(Mth.clamp(getValue.getAsInt() + delta, minValue, maxValue));
	}

	@Override
	public void updateNarration(NarrationElementOutput narrationElementOutput) {
		//noop
	}
}
