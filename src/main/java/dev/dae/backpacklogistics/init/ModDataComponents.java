package dev.dae.backpacklogistics.init;

import com.mojang.serialization.Codec;
import dev.dae.backpacklogistics.BackpackLogistics;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
	private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, BackpackLogistics.MOD_ID);

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> LINKED_NETWORK = DATA_COMPONENTS.registerComponentType(
			"linked_network", builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

	/** Upper threshold: stock callers fill to this amount, senders send down from above it. */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> UPPER_THRESHOLD = DATA_COMPONENTS.registerComponentType(
			"target_amount", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

	/** Lower threshold: stock callers order below this amount, senders keep this amount. */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LOWER_THRESHOLD = DATA_COMPONENTS.registerComponentType(
			"lower_threshold", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

	/** Per-filter-slot thresholds for stock callers; entries <= 0 fall back to the single-value components above. */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Integer>>> LOWER_THRESHOLDS = DATA_COMPONENTS.registerComponentType(
			"lower_thresholds", builder -> builder.persistent(Codec.INT.listOf())
					.networkSynchronized(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list())));

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Integer>>> UPPER_THRESHOLDS = DATA_COMPONENTS.registerComponentType(
			"upper_thresholds", builder -> builder.persistent(Codec.INT.listOf())
					.networkSynchronized(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list())));

	/**
	 * Per-filter-slot "actively working toward the target" flags. A stock caller slot turns
	 * active when stock drops below its lower threshold and stays active until the upper
	 * threshold is actually reached (senders mirror this for draining), so a partial fill
	 * that lands between the two bounds doesn't get stranded there.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Boolean>>> ACTIVE_SLOTS = DATA_COMPONENTS.registerComponentType(
			"active_slots", builder -> builder.persistent(Codec.BOOL.listOf())
					.networkSynchronized(ByteBufCodecs.BOOL.apply(ByteBufCodecs.list())));

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> UNPACK_ADDRESS = DATA_COMPONENTS.registerComponentType(
			"unpack_address", builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TARGET_ADDRESS = DATA_COMPONENTS.registerComponentType(
			"target_address", builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

	private ModDataComponents() {}

	public static void register(IEventBus modBus) {
		DATA_COMPONENTS.register(modBus);
	}
}
