package dev.dae.backpacklogistics.client.ponder;

import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import de.theidler.create_mobile_packages.entities.RoboBeeEntity;
import de.theidler.create_mobile_packages.entities.robo_entity.RoboEntity;
import de.theidler.create_mobile_packages.index.CMPEntities;
import java.lang.reflect.Field;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Scene text lives in en_us.json under backpack_logistics.ponder.<scene>.header/text_N;
 * the literals passed here are fallbacks and must be kept in sync with the lang file.
 */
public class ModPonderScenes {
	/** Robo bees fly high enough that the package hanging ~1.1 blocks below them clears placed blocks. */
	private static final double FLIGHT_HEIGHT = 1.9;

	private ModPonderScenes() {}

	public static void autoUnpacker(SceneBuilder sceneBuilder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(sceneBuilder);
		scene.title("auto_unpacker", "The Auto-Unpacker Upgrade");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.world().showSection(util.select().everywhere(), Direction.UP);
		BlockPos beePort = util.grid().at(1, 1, 1);
		BlockPos backpack = util.grid().at(3, 1, 3);
		Vec3 portHover = Vec3.atCenterOf(beePort).add(0, FLIGHT_HEIGHT, 0);
		Vec3 backpackHover = Vec3.atCenterOf(backpack).add(0, FLIGHT_HEIGHT, 0);

		scene.overlay()
				.showText(80)
				.text("Robo Bees from a Bee Port deliver packages addressed to you straight into your inventory")
				.attachKeyFrame()
				.placeNearTarget()
				.pointAt(util.vector().blockSurface(beePort, Direction.UP));
		scene.idle(20);

		ElementLink<EntityElement> bee = spawnRoboBee(scene, portHover, PackageStyles.getDefaultBox());
		scene.idle(20);
		fly(scene, bee, portHover, backpackHover, 40);
		scene.idle(20);

		scene.overlay()
				.showText(80)
				.text("With an Auto-Unpacker Upgrade in your backpack, arriving packages are unboxed directly into it")
				.attachKeyFrame()
				.placeNearTarget()
				.pointAt(util.vector().blockSurface(backpack, Direction.UP));
		scene.idle(20);
		setCarriedBox(scene, bee, ItemStack.EMPTY);
		scene.effects().indicateSuccess(backpack);
		fly(scene, bee, backpackHover, portHover, 40);
		discard(scene, bee);
		scene.idle(30);

		scene.overlay()
				.showText(70)
				.text("Contents follow the backpack's usual insert rules - whatever does not fit stays boxed up")
				.placeNearTarget()
				.pointAt(util.vector().blockSurface(backpack, Direction.UP));
		scene.idle(90);
		scene.overlay()
				.showText(90)
				.text("The upgrade tab has an extra address filter: packages sent to that address are claimed too, even while the backpack is placed as a block")
				.attachKeyFrame()
				.placeNearTarget()
				.pointAt(util.vector().blockSurface(backpack, Direction.NORTH));
		scene.idle(110);
	}

	public static void stockCaller(SceneBuilder sceneBuilder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(sceneBuilder);
		scene.title("stock_caller", "The Stock Caller Upgrade");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.world().showSection(util.select().everywhere(), Direction.UP);
		BlockPos ticker = util.grid().at(2, 1, 2);
		BlockPos packager = util.grid().at(5, 1, 3);
		BlockPos beePort = util.grid().at(2, 1, 4);
		BlockPos backpack = util.grid().at(4, 1, 5);
		Vec3 portHover = Vec3.atCenterOf(beePort).add(0, FLIGHT_HEIGHT, 0);
		Vec3 backpackHover = Vec3.atCenterOf(backpack).add(0, FLIGHT_HEIGHT, 0);

		scene.overlay()
				.showText(80)
				.text("Right-click any Stock Ticker or Stock Link with the Stock Caller Upgrade to link it to that logistics network")
				.attachKeyFrame()
				.placeNearTarget()
				.pointAt(util.vector().blockSurface(ticker, Direction.NORTH));
		scene.idle(100);
		scene.overlay()
				.showText(80)
				.text("Insert the upgrade into a backpack and pick the items to keep stocked - the advanced version tracks up to nine")
				.attachKeyFrame()
				.placeNearTarget()
				.pointAt(util.vector().blockSurface(backpack, Direction.UP));
		scene.idle(100);
		scene.overlay()
				.showText(90)
				.text("'Call below' and 'Fill to' set the stock range: dropping under the lower bound orders enough to reach the upper bound")
				.placeNearTarget()
				.pointAt(util.vector().blockSurface(backpack, Direction.UP));
		scene.idle(110);

		scene.overlay()
				.showText(80)
				.text("When stock runs low, packagers on the network pack the missing items, addressed to you")
				.attachKeyFrame()
				.placeNearTarget()
				.pointAt(util.vector().blockSurface(packager, Direction.UP));
		scene.world().modifyBlockEntity(packager, PackagerBlockEntity.class, be -> {
			be.animationTicks = 20;
			be.animationInward = false;
			be.heldBox = PackageStyles.getDefaultBox();
		});
		scene.idle(40);

		// package hops from the packager over toward the bee port
		ElementLink<EntityElement> hop = spawnPackageEntity(scene, Vec3.atCenterOf(packager).add(0, 0.8, 0),
				new Vec3(-0.25, 0.3, 0.05));
		scene.world().modifyBlockEntity(packager, PackagerBlockEntity.class, be -> be.heldBox = ItemStack.EMPTY);
		scene.idle(30);
		discard(scene, hop);
		scene.idle(30);

		ElementLink<EntityElement> bee = spawnRoboBee(scene, portHover, PackageStyles.getDefaultBox());
		scene.overlay()
				.showText(90)
				.text("A Bee Port with a Robo Bee then flies the package to you - the Stock Caller unboxes it into the backpack automatically")
				.attachKeyFrame()
				.placeNearTarget()
				.pointAt(util.vector().blockSurface(beePort, Direction.UP));
		scene.idle(20);
		fly(scene, bee, portHover, backpackHover, 50);
		setCarriedBox(scene, bee, ItemStack.EMPTY);
		scene.effects().indicateSuccess(backpack);
		fly(scene, bee, backpackHover, portHover, 40);
		discard(scene, bee);
		scene.idle(30);
	}

	public static void sender(SceneBuilder sceneBuilder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(sceneBuilder);
		scene.title("sender", "The Sender Upgrade");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.world().showSection(util.select().everywhere(), Direction.UP);
		BlockPos beePort = util.grid().at(2, 1, 1);
		BlockPos chest = util.grid().at(3, 1, 1);
		BlockPos backpack = util.grid().at(2, 1, 3);
		Vec3 portHover = Vec3.atCenterOf(beePort).add(0, FLIGHT_HEIGHT, 0);
		Vec3 backpackHover = Vec3.atCenterOf(backpack).add(0, FLIGHT_HEIGHT, 0);

		scene.overlay()
				.showText(80)
				.text("Link the Sender Upgrade to a logistics network by right-clicking a Stock Ticker or Stock Link")
				.attachKeyFrame()
				.placeNearTarget()
				.pointAt(util.vector().blockSurface(backpack, Direction.UP));
		scene.idle(100);
		scene.overlay()
				.showText(90)
				.text("Pick the items to watch: rising above 'Send above' ships the excess away until only the 'Keep' amount remains")
				.attachKeyFrame()
				.placeNearTarget()
				.pointAt(util.vector().blockSurface(backpack, Direction.UP));
		scene.idle(110);

		scene.overlay()
				.showText(80)
				.text("Enter the address of a Bee Port; a Robo Bee will fly to you and collect the excess as a package")
				.attachKeyFrame()
				.placeNearTarget()
				.pointAt(util.vector().blockSurface(backpack, Direction.NORTH));
		scene.idle(20);
		ElementLink<EntityElement> bee = spawnRoboBee(scene, portHover, null);
		scene.idle(10);
		fly(scene, bee, portHover, backpackHover, 40);
		setCarriedBox(scene, bee, PackageStyles.getDefaultBox());
		scene.idle(20);
		fly(scene, bee, backpackHover, portHover, 40);
		discard(scene, bee);
		scene.effects().indicateSuccess(beePort);
		scene.idle(20);

		scene.overlay()
				.showText(90)
				.text("The bee delivers it to the Bee Port whose address filter matches, which pushes the items into adjacent inventories")
				.attachKeyFrame()
				.placeNearTarget()
				.pointAt(util.vector().blockSurface(chest, Direction.UP));
		scene.effects().indicateSuccess(chest);
		scene.idle(110);
	}

	private static ElementLink<EntityElement> spawnRoboBee(CreateSceneBuilder scene, Vec3 position, ItemStack carriedBox) {
		return scene.world().createEntity(level -> {
			RoboBeeEntity bee = RoboBeeEntity.createEmpty(CMPEntities.ROBO_BEE_ENTITY.get(), level);
			bee.moveTo(position.x, position.y, position.z, 0, 0);
			if (carriedBox != null && !carriedBox.isEmpty()) {
				setSynchedData(bee, "DATA_ITEM_STACK", carriedBox);
				setSynchedData(bee, "PACKAGE_HEIGHT_SCALE", 1.0F);
			}
			return bee;
		});
	}

	private static void setCarriedBox(CreateSceneBuilder scene, ElementLink<EntityElement> beeLink, ItemStack box) {
		scene.world().modifyEntity(beeLink, entity -> {
			if (entity instanceof RoboBeeEntity bee) {
				setSynchedData(bee, "DATA_ITEM_STACK", box);
				setSynchedData(bee, "PACKAGE_HEIGHT_SCALE", box.isEmpty() ? 0.0F : 1.0F);
			}
		});
	}

	private static ElementLink<EntityElement> spawnPackageEntity(CreateSceneBuilder scene, Vec3 position, Vec3 motion) {
		ItemStack boxStack = PackageStyles.getDefaultBox();
		return scene.world().createEntity(level -> {
			PackageEntity packageEntity = new PackageEntity(level, position.x, position.y, position.z);
			packageEntity.setDeltaMovement(motion);
			packageEntity.box = boxStack;
			return packageEntity;
		});
	}

	/** Steps the bee along a straight path, updating its synched yaw to face travel direction. */
	private static void fly(CreateSceneBuilder scene, ElementLink<EntityElement> bee, Vec3 from, Vec3 to, int totalTicks) {
		int stepTicks = 2;
		int steps = Math.max(1, totalTicks / stepTicks);
		float yRot = (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90.0F;
		for (int i = 1; i <= steps; i++) {
			Vec3 point = from.lerp(to, i / (double) steps);
			scene.world().modifyEntity(bee, entity -> {
				entity.setPos(point.x, point.y, point.z);
				entity.setDeltaMovement(Vec3.ZERO);
				entity.setYRot(yRot);
				entity.setYHeadRot(yRot);
				if (entity instanceof RoboBeeEntity roboBee) {
					setSynchedData(roboBee, "ROT_YAW", yRot);
				}
			});
			scene.idle(stepTicks);
		}
	}

	private static void discard(CreateSceneBuilder scene, ElementLink<EntityElement> entity) {
		scene.world().modifyEntity(entity, Entity::discard);
	}

	/**
	 * CMP's robo bee reads carried-package stack, package height and yaw from its synched
	 * entity data; the accessors are private, so the display-only ponder bee sets them
	 * reflectively. Failures are swallowed - worst case the bee flies without its cargo visual.
	 */
	@SuppressWarnings("unchecked")
	private static <T> void setSynchedData(RoboEntity bee, String fieldName, T value) {
		try {
			Field field = RoboEntity.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			EntityDataAccessor<T> accessor = (EntityDataAccessor<T>) field.get(null);
			bee.getEntityData().set(accessor, value);
		} catch (ReflectiveOperationException | ClassCastException e) {
			// cosmetic only - ignore
		}
	}
}
