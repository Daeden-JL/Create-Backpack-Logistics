package dev.dae.backpacklogistics.client.gui;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;

/**
 * Renders a single line of (possibly dynamic) text.
 */
public class TextWidget extends WidgetBase {
	private final Supplier<Component> text;

	public TextWidget(Position position, int width, Supplier<Component> text) {
		super(position, new Dimension(width, 10));
		this.text = text;
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		//noop
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		guiGraphics.drawString(font, text.get(), x, y, 4210752, false);
	}

	@Override
	public void updateNarration(NarrationElementOutput narrationElementOutput) {
		//noop
	}
}
