package com.summy.enchantment.devcheck;

import com.summy.enchantment.SummyEnchantment;
import com.summy.enchantment.combat.CombatFormulas;
import com.summy.enchantment.combat.ElectricArc;
import com.summy.enchantment.combat.EnchantmentEffects;
import com.summy.enchantment.compat.SummyEnchantmentScriptHooks;
import com.summy.enchantment.config.SummyEnchantmentConfig;
import com.summy.enchantment.registry.ModAttributes;
import com.summy.enchantment.registry.ModEnchantments;
import com.summy.enchantment.registry.ModStatusEffects;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SE 的 Fabric 原生真实客户端自检。
 *
 * <p>只通过 {@code gradlew runClient -Pdevcheck -PexternalModsDir=...} 启用。
 * 自检会在专用世界中准备物品与僵尸目标，结束后自动退出客户端；
 * 正式 jar 排除本包，不影响发布行为。
 */
public final class EnchantmentFabricDevCheck {
	private static final boolean ENABLED = Boolean.getBoolean("summy_enchantment.devcheck");
	/** full = 完整兼容自检；kubejs = KubeJS；spear = 矛模组专项。 */
	private static final String MODE = System.getProperty("summy_enchantment.devcheck.mode", "full");
	private static final int CLIENT_IDLE_STOP_TICKS = 1200;
	private static final int CLIENT_TOTAL_STOP_TICKS = 6000;
	private static final String TEST_TAG = "se_devcheck_target";

	private static final List<String> FAILURES = new ArrayList<>();
	private static int serverTick;
	private static int clientTick;
	private static int idleTicks;
	private static int totalClientTicks;
	private static int checks;
	private static boolean finished;
	private static boolean registered;
	private static ZombieEntity electricPrimary;
	private static ZombieEntity electricSecondary;
	private static ZombieEntity electricTertiary;
	private static ZombieEntity electricFriend;
	private static WolfEntity electricPet;
	private static ServerWorld electricWorld;
	private static ZombieEntity soulGraceVictim;
	private static ZombieEntity mobStrengthVictim;
	private static boolean mobStrengthOldRework;
	private static boolean mobStrengthOldApply;

	private EnchantmentFabricDevCheck() {
	}

	/** 由客户端入口在 devcheck 开关打开时注册；重复调用不会重复挂事件。 */
	public static void register() {
		if (!ENABLED || registered) {
			return;
		}
		registered = true;
		ServerTickEvents.END_SERVER_TICK.register(server -> onServerTick(server));
		ClientTickEvents.END_CLIENT_TICK.register(EnchantmentFabricDevCheck::onClientTick);
	}

	private static void onServerTick(net.minecraft.server.MinecraftServer server) {
		if (!ENABLED) {
			return;
		}
		List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
		if (players.isEmpty()) {
			serverTick = 0;
			return;
		}
		ServerPlayerEntity player = players.get(0);
		serverTick++;
		try {
			if ("spear".equals(MODE)) {
				if (!isLoaded("spears")) {
					if (serverTick == 20) {
						check("矛专项前置", false, "未安装 mod id=spears");
						finish("缺少 Spears");
					}
					return;
				}
				SpearDevCheckResult result = SpearFabricDevCheck.tick(player, serverTick);
				checks += result.checks();
				FAILURES.addAll(result.failures());
				if (serverTick == 280) {
					finish("Fabric 矛兼容自检结束");
				}
				return;
			}
			if ("kubejs".equals(MODE)) {
				if (serverTick == 40) {
					checkKubeJSEvents(player);
				} else if (serverTick == 60) {
					finish("Fabric KubeJS-only 自检结束");
				}
				return;
			}
			switch (serverTick) {
				case 20 -> prepareWorld(player);
				case 21 -> checkFabricPlayerAttributeMerge(player);
				case 22 -> checkAutoSmeltPipeline(player);
				case 24 -> checkExtraArrowFailureIsNotRetried(player);
				case 26 -> checkRevengeWindowCleanup(player);
				case 28 -> checkCrossbowMultishotSpread(player);
				case 40 -> checkMajruszOutgoing(player);
				case 60 -> checkMajruszIncoming(player);
				case 61 -> checkMixedAbsorptionPool(player);
				case 62 -> checkEnchantmentCompatibilityMatrix(player);
				case 70 -> checkMobStrengthToggle(player);
				case 75 -> checkMobStrengthAutoRestore();
				case 80 -> checkSoulGrace(player);
				case 110 -> checkExecutionAndStartElectric(player);
				case 130 -> checkElectricBladeResult(player);
				case 135 -> checkSoulGraceExpired();
				case 150 -> checkKubeJSEvents(player);
				case 170 -> finish("Fabric 客户端自检结束");
				default -> {
				}
			}
		} catch (RuntimeException | Error throwable) {
			FAILURES.add("步骤 " + serverTick + " 异常：" + throwable);
			log("步骤 " + serverTick + " 异常：" + throwable);
			throwable.printStackTrace();
			finish("步骤异常");
		}
	}

	private static void onClientTick(MinecraftClient client) {
		if (!ENABLED) {
			return;
		}
		// 测试机窗口失焦时单人游戏会暂停；自检期间关闭该暂停，确保集成服务端继续推进。
		client.options.pauseOnLostFocus = false;
		totalClientTicks++;
		if (totalClientTicks > CLIENT_TOTAL_STOP_TICKS) {
			finish("硬停止：客户端总 tick 超限");
			return;
		}
		if (client.player == null || client.world == null) {
			idleTicks++;
			if (idleTicks > CLIENT_IDLE_STOP_TICKS) {
				finish("硬停止：等待测试世界超时");
			}
			return;
		}
		idleTicks = 0;
		clientTick++;
		if (finished && clientTick > 20) {
			client.stop();
		}
	}

	private static void prepareWorld(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		player.getInventory().clear();
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
		EnchantmentEffects.clearLifeShield(player);
		List<ZombieEntity> stale = world.getEntitiesByClass(
				ZombieEntity.class, player.getBoundingBox().expand(32.0D),
				zombie -> zombie.getCommandTags().contains(TEST_TAG));
		stale.forEach(ZombieEntity::discard);
		log("测试准备：维度=" + world.getRegistryKey().getValue()
				+ "，Majrusz=" + isLoaded("majruszsenchantments")
				+ "，MajruszLibrary=" + isLoaded("majruszlibrary"));
		if (!isLoaded("majruszsenchantments") || !isLoaded("majruszlibrary")) {
			log("Majrusz 前置未安装，跳过外部联动场景并保留 SE 基础规则断言");
		}
		check("测试世界可写", player.isAlive(), "player=" + player.getName().getString());
		check("破空之愿/本能释放双向互斥",
				!EnchantmentHelper.isCompatible(List.of(ModEnchantments.SKY_WISH),
						ModEnchantments.INSTINCTIVE_RELEASE)
						&& !EnchantmentHelper.isCompatible(List.of(ModEnchantments.INSTINCTIVE_RELEASE),
								ModEnchantments.SKY_WISH),
				"sky_to_inst=" + EnchantmentHelper.isCompatible(List.of(ModEnchantments.SKY_WISH),
						ModEnchantments.INSTINCTIVE_RELEASE)
						+ " inst_to_sky=" + EnchantmentHelper.isCompatible(
								List.of(ModEnchantments.INSTINCTIVE_RELEASE), ModEnchantments.SKY_WISH));
	}

	private static void checkFabricPlayerAttributeMerge(ServerPlayerEntity player) {
		check("Fabric 玩家实例已挂载生命护盾属性",
				player.getAttributes().hasAttribute(ModAttributes.LIFE_SHIELD),
				"attribute=" + ModAttributes.LIFE_SHIELD);

		AttributeContainer competing = new AttributeContainer(
				PlayerEntity.createPlayerAttributes()
						.add(EntityAttributes.GENERIC_FLYING_SPEED, 2.0D)
						.build()
		);
		boolean merged = invokeEnsureLifeShieldAttribute(competing);
		check("Fabric 属性追加保留第三方默认属性",
				merged
						&& competing.hasAttribute(ModAttributes.LIFE_SHIELD)
						&& Math.abs(competing.getBaseValue(EntityAttributes.GENERIC_FLYING_SPEED) - 2.0D) < 1.0E-6D,
				"merged=" + merged
						+ " shield=" + competing.hasAttribute(ModAttributes.LIFE_SHIELD)
						+ " flying=" + competing.getBaseValue(EntityAttributes.GENERIC_FLYING_SPEED));

		AttributeContainer existing = new AttributeContainer(
				PlayerEntity.createPlayerAttributes()
						.add(ModAttributes.LIFE_SHIELD, 3.0D)
						.build()
		);
		boolean touchedExisting = invokeEnsureLifeShieldAttribute(existing);
		check("Fabric 属性追加不覆盖已有生命护盾",
				!touchedExisting
						&& Math.abs(existing.getBaseValue(ModAttributes.LIFE_SHIELD) - 3.0D) < 1.0E-6D,
				"touched=" + touchedExisting
						+ " base=" + existing.getBaseValue(ModAttributes.LIFE_SHIELD));

		AttributeContainer source = new AttributeContainer(PlayerEntity.createPlayerAttributes().build());
		boolean sourceEntry = invokeEnsureLifeShieldAttribute(source);
		AttributeContainer restored = new AttributeContainer(PlayerEntity.createPlayerAttributes().build());
		boolean restoredEntry = invokeEnsureLifeShieldAttribute(restored);
		if (sourceEntry) {
			source.getCustomInstance(ModAttributes.LIFE_SHIELD).setBaseValue(3.5D);
			restored.readNbt(source.toNbt());
		}
		check("Fabric 属性追加支持 NBT 恢复",
				sourceEntry
						&& restoredEntry
						&& Math.abs(restored.getBaseValue(ModAttributes.LIFE_SHIELD) - 3.5D) < 1.0E-6D,
				"source=" + sourceEntry
						+ " restored=" + restoredEntry
						+ " base=" + (restoredEntry ? restored.getBaseValue(ModAttributes.LIFE_SHIELD) : "missing"));
	}

	private static boolean invokeEnsureLifeShieldAttribute(AttributeContainer container) {
		try {
			Method method = ModAttributes.class.getDeclaredMethod(
					"ensureLifeShieldAttribute", AttributeContainer.class);
			method.setAccessible(true);
			Object changed = method.invoke(null, container);
			return changed instanceof Boolean value && value;
		} catch (ReflectiveOperationException exception) {
			log("调用 Fabric 属性追加入口失败：" + exception);
			return false;
		}
	}

	private static void checkAutoSmeltPipeline(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		BlockPos pos = player.getBlockPos().add(0, 12, 0);
		Box probe = new Box(
				pos.getX() - 2.0D, pos.getY() - 2.0D, pos.getZ() - 2.0D,
				pos.getX() + 3.0D, pos.getY() + 3.0D, pos.getZ() + 3.0D
		);
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		boolean oldAutoSmelt = config.enableAutoSmelt;
		boolean oldSneaking = player.isSneaking();
		try {
			config.enableAutoSmelt = true;
			player.setSneaking(false);
			clearDropProbe(world, probe);
			ItemStack tool = enchanted(Items.DIAMOND_PICKAXE, ModEnchantments.AUTO_SMELT, 1);
			Block.dropStacks(Blocks.IRON_ORE.getDefaultState(), world, pos, null, player, tool);
			List<ItemEntity> smeltedDrops = world.getEntitiesByClass(ItemEntity.class, probe, entity -> true);
			long ingots = smeltedDrops.stream()
					.filter(entity -> entity.getStack().isOf(Items.IRON_INGOT))
					.count();
			long rawOre = smeltedDrops.stream()
					.filter(entity -> entity.getStack().isOf(Items.RAW_IRON))
					.count();
			check("自动熔炼保留原掉落入口并替换产物", ingots == 1 && rawOre == 0,
					"ingots=" + ingots + " raw_iron=" + rawOre);

			clearDropProbe(world, probe);
			player.setSneaking(true);
			Block.dropStacks(Blocks.IRON_ORE.getDefaultState(), world, pos, null, player, tool);
			List<ItemEntity> sneakingDrops = world.getEntitiesByClass(ItemEntity.class, probe, entity -> true);
			long sneakingRaw = sneakingDrops.stream()
					.filter(entity -> entity.getStack().isOf(Items.RAW_IRON))
					.count();
			check("自动熔炼潜行时绕开", sneakingRaw == 1,
					"raw_iron=" + sneakingRaw);

			clearDropProbe(world, probe);
			player.setSneaking(false);
			Block.dropStacks(Blocks.DIAMOND_ORE.getDefaultState(), world, pos, null, player, tool);
			long experienceOrbs = world.getEntitiesByClass(ExperienceOrbEntity.class, probe, entity -> true).size();
			check("自动熔炼保留方块经验结算", experienceOrbs > 0,
					"experience_orbs=" + experienceOrbs);

			boolean blastOnlyProbe = world.getRecipeManager()
					.getAllMatches(RecipeType.BLASTING, new SimpleInventory(new ItemStack(Items.DIRT)), world)
					.stream()
					.filter(recipe -> recipe instanceof AbstractCookingRecipe)
					.map(recipe -> ((AbstractCookingRecipe) recipe).getOutput(world.getRegistryManager()))
					.anyMatch(output -> output.isOf(Items.DIAMOND));
			if (blastOnlyProbe) {
				clearDropProbe(world, probe);
				Block.dropStacks(Blocks.DIRT.getDefaultState(), world, pos, null, player, tool);
				List<ItemEntity> blastProbeDrops = world.getEntitiesByClass(ItemEntity.class, probe, entity -> true);
				long dirt = blastProbeDrops.stream()
						.filter(entity -> entity.getStack().isOf(Items.DIRT))
						.count();
				long diamonds = blastProbeDrops.stream()
						.filter(entity -> entity.getStack().isOf(Items.DIAMOND))
						.count();
				check("仅有高炉配方时不触发自动熔炼", dirt == 1 && diamonds == 0,
						"dirt=" + dirt + " diamonds=" + diamonds);
			}
		} finally {
			config.enableAutoSmelt = oldAutoSmelt;
			player.setSneaking(oldSneaking);
			clearDropProbe(world, probe);
		}
	}

	private static void clearDropProbe(ServerWorld world, Box probe) {
		world.getEntitiesByClass(ItemEntity.class, probe, entity -> true).forEach(ItemEntity::discard);
		world.getEntitiesByClass(ExperienceOrbEntity.class, probe, entity -> true)
				.forEach(ExperienceOrbEntity::discard);
	}

	private static void checkExtraArrowFailureIsNotRetried(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		BlockPos pos = player.getBlockPos().add(0, 18, 0);
		RejectingZombieEntity target = new RejectingZombieEntity(world);
		target.refreshPositionAndAngles(
				pos.getX() + 2.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
		target.setAiDisabled(true);
		target.setNoGravity(true);
		target.setPersistent();
		target.addCommandTag(TEST_TAG);
		world.spawnEntity(target);

		PersistentProjectileEntity arrow = net.minecraft.entity.EntityType.ARROW.create(world);
		if (arrow == null) {
			throw new IllegalStateException("无法创建额外箭测试实体");
		}
		arrow.setOwner(player);
		arrow.setPosition(pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D);
		arrow.setNoGravity(true);
		arrow.setVelocity(1.0D, 0.0D, 0.0D);
		EnchantmentEffects.markExtraHitArrow(arrow);
		world.spawnEntity(arrow);
		for (int i = 0; i < 8 && !arrow.isRemoved() && target.damageAttempts == 0; i++) {
			arrow.tick();
		}
		check("额外箭首次伤害失败后不重试", target.damageAttempts == 1,
				"damage_attempts=" + target.damageAttempts);
		arrow.discard();
		target.discard();
	}

	private static void checkRevengeWindowCleanup(ServerPlayerEntity player) {
		Map<Object, Object> windows = staticMap(EnchantmentEffects.class, "REVENGE_WINDOW");
		windows.clear();
		UUID playerId = player.getUuid();
		windows.put(playerId, player.getWorld().getTime() - 1L);
		EnchantmentEffects.revengeMultiplier(player);
		check("复仇过期窗口即时清理", !windows.containsKey(playerId),
				"remaining=" + windows.size());

		windows.put(playerId, player.getWorld().getTime() + 60L);
		EnchantmentEffects.clearRevengeWindow(playerId);
		check("复仇断线窗口清理", !windows.containsKey(playerId),
				"remaining=" + windows.size());

		windows.put(playerId, player.getWorld().getTime() + 60L);
		windows.put(UUID.randomUUID(), player.getWorld().getTime() + 60L);
		EnchantmentEffects.clearRevengeWindows();
		check("复仇停服窗口清理", windows.isEmpty(),
				"remaining=" + windows.size());
		windows.clear();
	}

	private static void checkCrossbowMultishotSpread(ServerPlayerEntity player) {
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		float oldSpread = config.multishotSpreadDegrees;
		ItemStack previous = player.getMainHandStack().copy();
		ServerWorld world = player.getServerWorld();
		try {
			config.multishotSpreadDegrees = 0.0F;
			List<PersistentProjectileEntity> parallel = shootTestCrossbow(player, world);
			double maxParallelAngle = parallel.stream()
					.mapToDouble(arrow -> projectileAngleFromLook(arrow, player))
					.max()
					.orElse(180.0D);
			check("弩多重射击 0 度为完全平行",
					parallel.size() == 3 && maxParallelAngle < 0.5D,
					"arrows=" + parallel.size() + " max_angle=" + maxParallelAngle);
			discardTestArrows(world, player);

			config.multishotSpreadDegrees = 15.0F;
			List<PersistentProjectileEntity> spread = shootTestCrossbow(player, world);
			long center = spread.stream()
					.filter(arrow -> projectileAngleFromLook(arrow, player) < 1.0D)
					.count();
			long sides = spread.stream()
					.filter(arrow -> {
						double angle = projectileAngleFromLook(arrow, player);
						return angle >= 14.0D && angle <= 16.0D;
					})
					.count();
			check("弩多重射击配置角作用于左右箭",
					spread.size() == 3 && center == 1 && sides == 2,
					"arrows=" + spread.size() + " center=" + center + " sides=" + sides);
		} finally {
			config.multishotSpreadDegrees = oldSpread;
			discardTestArrows(world, player);
			player.setStackInHand(Hand.MAIN_HAND, previous);
		}
	}

	private static List<PersistentProjectileEntity> shootTestCrossbow(
			ServerPlayerEntity player, ServerWorld world) {
		discardTestArrows(world, player);
		ItemStack crossbow = new ItemStack(Items.CROSSBOW);
		crossbow.addEnchantment(Enchantments.MULTISHOT, 1);
		NbtCompound nbt = crossbow.getOrCreateNbt();
		nbt.putBoolean("Charged", true);
		NbtList projectiles = new NbtList();
		for (int i = 0; i < 3; i++) {
			projectiles.add(new ItemStack(Items.ARROW).writeNbt(new NbtCompound()));
		}
		nbt.put("ChargedProjectiles", projectiles);
		player.setStackInHand(Hand.MAIN_HAND, crossbow);
		CrossbowItem.shootAll(world, player, Hand.MAIN_HAND, crossbow, 1.0F, 0.0F);
		return world.getEntitiesByClass(
				PersistentProjectileEntity.class,
				player.getBoundingBox().expand(3.0D),
				arrow -> arrow.getOwner() == player
		);
	}

	private static double projectileAngleFromLook(PersistentProjectileEntity arrow, ServerPlayerEntity player) {
		Vec3d velocity = arrow.getVelocity().normalize();
		Vec3d look = player.getRotationVec(1.0F).normalize();
		double dot = Math.max(-1.0D, Math.min(1.0D, velocity.dotProduct(look)));
		return Math.toDegrees(Math.acos(dot));
	}

	private static void discardTestArrows(ServerWorld world, ServerPlayerEntity player) {
		world.getEntitiesByClass(
				PersistentProjectileEntity.class,
				player.getBoundingBox().expand(3.0D),
				arrow -> arrow.getOwner() == player
		).forEach(PersistentProjectileEntity::discard);
	}

	@SuppressWarnings("unchecked")
	private static Map<Object, Object> staticMap(Class<?> owner, String fieldName) {
		try {
			Field field = owner.getDeclaredField(fieldName);
			field.setAccessible(true);
			Object value = field.get(null);
			if (value instanceof Map<?, ?> map) {
				return (Map<Object, Object>) map;
			}
		} catch (ReflectiveOperationException exception) {
			log("读取静态映射失败：" + owner.getSimpleName() + "#" + fieldName + " " + exception);
		}
		return Map.of();
	}

	private static final class RejectingZombieEntity extends ZombieEntity {
		private int damageAttempts;

		private RejectingZombieEntity(ServerWorld world) {
			super(net.minecraft.entity.EntityType.ZOMBIE, world);
		}

		@Override
		public boolean damage(DamageSource source, float amount) {
			damageAttempts++;
			return false;
		}
	}

	private static void checkMajruszOutgoing(ServerPlayerEntity player) {
		Enchantment majrusz = external("majruszsenchantments", "death_wish");
		player.getInventory().clear();
		player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, enchanted(Items.IRON_SWORD, majrusz, 1));
		player.setStackInHand(net.minecraft.util.Hand.OFF_HAND, ItemStack.EMPTY);
		player.equipStack(net.minecraft.entity.EquipmentSlot.CHEST,
				enchanted(Items.IRON_CHESTPLATE, ModEnchantments.DEATHS_BLESSING, 1));
		player.equipStack(net.minecraft.entity.EquipmentSlot.LEGS, ItemStack.EMPTY);
		player.equipStack(net.minecraft.entity.EquipmentSlot.FEET, ItemStack.EMPTY);
		player.equipStack(net.minecraft.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);
		player.setHealth(player.getMaxHealth() * 0.5F);
		player.setAbsorptionAmount(0.0F);

		ZombieEntity target = spawnTarget(player, 100.0F, 100.0F);
		target.damage(player.getDamageSources().playerAttack(player), 10.0F);
		float outgoingLost = 100.0F - target.getHealth();
		check("死神祝福：造成侧只乘一层", Math.abs(outgoingLost - 15.0F) < 0.05F,
				"lost=" + outgoingLost + " Majrusz=" + (majrusz != null));
		target.discard();
	}

	private static void checkMajruszIncoming(ServerPlayerEntity player) {
		Enchantment majrusz = external("majruszsenchantments", "death_wish");
		player.getInventory().clear();
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
		ZombieEntity victim = spawnTarget(player, 100.0F, 50.0F);
		victim.equipStack(EquipmentSlot.CHEST,
				enchanted(Items.PAPER, ModEnchantments.DEATHS_BLESSING, 1));
		victim.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, enchanted(Items.IRON_SWORD, majrusz, 1));
		victim.damage(player.getDamageSources().playerAttack(player), 10.0F);
		float incomingLost = 50.0F - victim.getHealth();
		check("死神祝福：受伤侧只乘一层", Math.abs(incomingLost - 9.5F) < 0.05F,
				"lost=" + incomingLost + " Majrusz=" + (majrusz != null));
		victim.discard();
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
	}

	private static void checkMobStrengthToggle(ServerPlayerEntity player) {
		var config = com.summy.enchantment.config.SummyEnchantmentConfig.get();
		mobStrengthOldRework = config.enableStrengthRework;
		mobStrengthOldApply = config.applyStrengthToMobs;
		ZombieEntity zombie = spawnTarget(player, 100.0F, 100.0F);
		mobStrengthVictim = zombie;
		try {
			config.enableStrengthRework = true;
			config.applyStrengthToMobs = false;
			zombie.clearStatusEffects();
			zombie.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 1));
			check("关闭生物力量时保留原版力量",
					zombie.getStatusEffect(StatusEffects.STRENGTH) != null
							&& zombie.getStatusEffect(ModStatusEffects.STRENGTH) == null,
					"vanilla=" + (zombie.getStatusEffect(StatusEffects.STRENGTH) != null)
							+ " custom=" + (zombie.getStatusEffect(ModStatusEffects.STRENGTH) != null));
			check("原版力量生物不吃 SE 力量乘区",
					Math.abs(CombatFormulas.strengthMultiplier(zombie) - 1.0F) < 0.001F,
					"multiplier=" + CombatFormulas.strengthMultiplier(zombie));

			config.applyStrengthToMobs = true;
			zombie.clearStatusEffects();
			zombie.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 1));
			check("开启生物力量后重定向为自定义力量",
					zombie.getStatusEffect(StatusEffects.STRENGTH) == null
							&& zombie.getStatusEffect(ModStatusEffects.STRENGTH) != null,
					"vanilla=" + (zombie.getStatusEffect(StatusEffects.STRENGTH) != null)
							+ " custom=" + (zombie.getStatusEffect(ModStatusEffects.STRENGTH) != null));

			config.applyStrengthToMobs = false;
		} catch (Throwable throwable) {
			config.enableStrengthRework = mobStrengthOldRework;
			config.applyStrengthToMobs = mobStrengthOldApply;
			zombie.discard();
			mobStrengthVictim = null;
			throw throwable;
		}
	}

	private static void checkMobStrengthAutoRestore() {
		var config = com.summy.enchantment.config.SummyEnchantmentConfig.get();
		ZombieEntity zombie = mobStrengthVictim;
		try {
			check("关闭开关后自定义力量由维护 tick 自动还原",
					zombie != null
							&& zombie.getStatusEffect(StatusEffects.STRENGTH) != null
							&& zombie.getStatusEffect(ModStatusEffects.STRENGTH) == null,
					zombie == null ? "victim=null"
							: "vanilla=" + (zombie.getStatusEffect(StatusEffects.STRENGTH) != null)
									+ " custom="
									+ (zombie.getStatusEffect(ModStatusEffects.STRENGTH) != null));
		} finally {
			config.enableStrengthRework = mobStrengthOldRework;
			config.applyStrengthToMobs = mobStrengthOldApply;
			if (zombie != null) {
				zombie.discard();
			}
			mobStrengthVictim = null;
		}
	}

	private static void checkSoulGrace(ServerPlayerEntity player) {
		soulGraceVictim = spawnTarget(player, 100.0F, 1.0F);
		soulGraceVictim.setNoGravity(true);
		soulGraceVictim.setPosition(player.getX(), player.getY() + 20.0D, player.getZ());
		soulGraceVictim.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 1200, 0));
		ItemStack chest = enchanted(Items.IRON_CHESTPLATE, ModEnchantments.SOUL_GRACE, 1);
		chest.addEnchantment(ModEnchantments.CALAMITY, 1);
		soulGraceVictim.equipStack(EquipmentSlot.CHEST, chest);
		EnchantmentEffects.clearSoulGraceCooldown(soulGraceVictim.getUuid());
		soulGraceVictim.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 200, 0));
		soulGraceVictim.timeUntilRegen = 0;
		boolean damaged = soulGraceVictim.damage(
				player.getDamageSources().playerAttack(player), 100.0F);
		check("灵魂加护拦截致命伤害并满血复活",
				damaged && soulGraceVictim.isAlive()
						&& Math.abs(soulGraceVictim.getHealth() - soulGraceVictim.getMaxHealth()) < 0.01F,
				"damaged=" + damaged + " health=" + soulGraceVictim.getHealth());
		check("灵魂加护清除负面并挂抗性",
				soulGraceVictim.getStatusEffect(StatusEffects.POISON) == null
						&& soulGraceVictim.getStatusEffect(StatusEffects.RESISTANCE) != null,
				"poison=" + (soulGraceVictim.getStatusEffect(StatusEffects.POISON) != null)
						+ " resistance=" + (soulGraceVictim.getStatusEffect(StatusEffects.RESISTANCE) != null));
		check("灵魂加护冷却为内置计时显示",
				soulGraceVictim.getStatusEffect(ModStatusEffects.SOUL_GRACE) != null,
				"display=" + (soulGraceVictim.getStatusEffect(ModStatusEffects.SOUL_GRACE) != null));
		check("灵魂加护窗口内恶咒被抑制",
				EnchantmentEffects.isCalamitySuppressed(soulGraceVictim),
				"suppressed=" + EnchantmentEffects.isCalamitySuppressed(soulGraceVictim));
		soulGraceVictim.removeStatusEffect(ModStatusEffects.SOUL_GRACE);
		check("清除冷却显示效果后灵魂加护权威计时仍阻止重复触发",
				!EnchantmentEffects.trySoulGrace(soulGraceVictim),
				"secondTrigger=false");
	}

	private static void checkSoulGraceExpired() {
		if (soulGraceVictim == null) {
			check("灵魂加护恶咒窗口结束", false, "victim=null");
			return;
		}
		check("灵魂加护 2 秒后恢复恶咒锁血",
				!EnchantmentEffects.isCalamitySuppressed(soulGraceVictim)
						&& Math.abs(soulGraceVictim.getHealth() - 1.0F) < 0.01F,
				"suppressed=" + EnchantmentEffects.isCalamitySuppressed(soulGraceVictim)
						+ " health=" + soulGraceVictim.getHealth());
		EnchantmentEffects.clearSoulGraceCooldown(soulGraceVictim.getUuid());
		soulGraceVictim.discard();
		soulGraceVictim = null;
	}

	private static void checkMixedAbsorptionPool(ServerPlayerEntity player) {
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		boolean oldLifeShield = config.enableLifeShield;
		try {
			config.enableLifeShield = true;
			player.equipStack(EquipmentSlot.CHEST,
					enchanted(Items.IRON_CHESTPLATE, ModEnchantments.LIFE_SHIELD, 1));
			EnchantmentEffects.clearLifeShield(player);
			player.setAbsorptionAmount(0.0F);
			float cap = EnchantmentEffects.lifeShieldCap(player, 1);
			float added = EnchantmentEffects.addLifeShield(player, cap);
			float external = Math.max(0.5F, cap / 2.0F);
			check("生命护盾混合池：初始 SE 份额",
					Math.abs(added - cap) < 0.01F
							&& Math.abs(EnchantmentEffects.lifeShield(player) - cap) < 0.01F
							&& Math.abs(player.getAbsorptionAmount() - cap) < 0.01F,
					"cap=" + cap + " added=" + added
							+ " se=" + EnchantmentEffects.lifeShield(player)
							+ " absorption=" + player.getAbsorptionAmount());

			player.setAbsorptionAmount(cap + external);
			EnchantmentEffects.tickLifeShield(player);
			check("生命护盾混合池：外部吸收不改变 SE 名义值",
					Math.abs(EnchantmentEffects.lifeShield(player) - cap) < 0.01F,
					"se=" + EnchantmentEffects.lifeShield(player)
							+ " absorption=" + player.getAbsorptionAmount());

			player.setAbsorptionAmount(cap);
			EnchantmentEffects.tickLifeShield(player);
			check("生命护盾混合池：共享池下降优先结算 SE",
					Math.abs(EnchantmentEffects.lifeShield(player) - (cap - external)) < 0.01F,
					"se=" + EnchantmentEffects.lifeShield(player)
							+ " expected=" + (cap - external));

			EnchantmentEffects.clearLifeShield(player);
			check("生命护盾混合池：清除 SE 保留外部吸收",
					Math.abs(player.getAbsorptionAmount() - external) < 0.01F,
					"absorption=" + player.getAbsorptionAmount() + " external=" + external);

			float readded = EnchantmentEffects.addLifeShield(player, cap);
			check("生命护盾混合池：重新增加只增加 SE 份额",
					Math.abs(readded - cap) < 0.01F
							&& Math.abs(EnchantmentEffects.lifeShield(player) - cap) < 0.01F
							&& Math.abs(player.getAbsorptionAmount() - (external + cap)) < 0.01F,
					"added=" + readded + " se=" + EnchantmentEffects.lifeShield(player)
							+ " absorption=" + player.getAbsorptionAmount());
		} finally {
			EnchantmentEffects.clearLifeShield(player);
			player.setAbsorptionAmount(0.0F);
			player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
			config.enableLifeShield = oldLifeShield;
		}
	}

	private static void checkEnchantmentCompatibilityMatrix(ServerPlayerEntity player) {
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		boolean oldSharpness = config.enableCustomSharpness;
		boolean oldCrossbowCompat = config.enableCrossbowCompat;
		boolean oldInfinityMending = config.enableInfinityMending;
		try {
			config.enableCustomSharpness = true;
			config.enableCrossbowCompat = true;
			config.enableInfinityMending = true;
			check("附魔互斥：锋利Plus与原版锋利",
					incompatible(Enchantments.SHARPNESS, ModEnchantments.SHARPNESS_PLUS)
							&& incompatible(ModEnchantments.SHARPNESS_PLUS, Enchantments.SHARPNESS),
					"vanilla_to_custom=" + incompatible(Enchantments.SHARPNESS, ModEnchantments.SHARPNESS_PLUS)
							+ " custom_to_vanilla=" + incompatible(
									ModEnchantments.SHARPNESS_PLUS, Enchantments.SHARPNESS));
			check("附魔互斥：自动熔炼与精准采集",
					incompatible(ModEnchantments.AUTO_SMELT, Enchantments.SILK_TOUCH)
							&& incompatible(Enchantments.SILK_TOUCH, ModEnchantments.AUTO_SMELT),
					"auto_to_silk=" + incompatible(ModEnchantments.AUTO_SMELT, Enchantments.SILK_TOUCH)
							+ " silk_to_auto=" + incompatible(
									Enchantments.SILK_TOUCH, ModEnchantments.AUTO_SMELT));
			check("附魔互斥：无尽箭袋与无限",
					incompatible(ModEnchantments.ENDLESS_QUIVER, Enchantments.INFINITY)
							&& incompatible(Enchantments.INFINITY, ModEnchantments.ENDLESS_QUIVER),
					"endless_to_infinity=" + incompatible(
							ModEnchantments.ENDLESS_QUIVER, Enchantments.INFINITY)
							+ " infinity_to_endless=" + incompatible(
									Enchantments.INFINITY, ModEnchantments.ENDLESS_QUIVER));
			check("附魔互斥：破空之愿与本能释放",
					incompatible(ModEnchantments.SKY_WISH, ModEnchantments.INSTINCTIVE_RELEASE)
							&& incompatible(ModEnchantments.INSTINCTIVE_RELEASE, ModEnchantments.SKY_WISH),
					"sky_to_instinct=" + incompatible(
							ModEnchantments.SKY_WISH, ModEnchantments.INSTINCTIVE_RELEASE)
							+ " instinct_to_sky=" + incompatible(
									ModEnchantments.INSTINCTIVE_RELEASE, ModEnchantments.SKY_WISH));
			check("附魔互通：弓弩物品可附性",
					Enchantments.POWER.isAcceptableItem(new ItemStack(Items.CROSSBOW))
							&& Enchantments.PIERCING.isAcceptableItem(new ItemStack(Items.BOW)),
					"crossbow_power=" + Enchantments.POWER.isAcceptableItem(new ItemStack(Items.CROSSBOW))
							+ " bow_piercing=" + Enchantments.PIERCING.isAcceptableItem(new ItemStack(Items.BOW)));
			check("附魔共存：无限与经验修补",
					compatible(Enchantments.INFINITY, Enchantments.MENDING)
							&& compatible(Enchantments.MENDING, Enchantments.INFINITY),
					"infinity_to_mending=" + compatible(Enchantments.INFINITY, Enchantments.MENDING)
							+ " mending_to_infinity=" + compatible(Enchantments.MENDING, Enchantments.INFINITY));
			check("原版锋利退出随机候选",
					!Enchantments.SHARPNESS.isAvailableForRandomSelection(),
					"available=" + Enchantments.SHARPNESS.isAvailableForRandomSelection());

			String forbiddenSelection = "";
			List<Item> candidateItems = List.of(Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE,
					Items.BOW, Items.CROSSBOW);
			for (int draw = 0; draw < 256 && forbiddenSelection.isEmpty(); draw++) {
				for (Item item : candidateItems) {
					Set<Enchantment> candidates = new java.util.HashSet<>();
					for (EnchantmentLevelEntry entry : EnchantmentHelper.generateEnchantments(
							Random.create(0x5E5E5E5EL + draw), new ItemStack(item), 30, false)) {
						candidates.add(entry.enchantment);
					}
					if (candidates.contains(Enchantments.SHARPNESS)) {
						forbiddenSelection = item + ":[vanilla_sharpness]";
						break;
					}
					if (containsPair(candidates, ModEnchantments.AUTO_SMELT, Enchantments.SILK_TOUCH)) {
						forbiddenSelection = item + ":[auto_smelt,silk_touch]";
						break;
					}
					if (containsPair(candidates, ModEnchantments.ENDLESS_QUIVER, Enchantments.INFINITY)) {
						forbiddenSelection = item + ":[endless_quiver,infinity]";
						break;
					}
					if (containsPair(candidates,
							ModEnchantments.SKY_WISH, ModEnchantments.INSTINCTIVE_RELEASE)) {
						forbiddenSelection = item + ":[sky_wish,instinctive_release]";
						break;
					}
				}
			}
			check("附魔随机结果不含互斥组合", forbiddenSelection.isEmpty(),
					"rolls=1024 forbidden=" + !forbiddenSelection.isEmpty()
							+ " detail=" + forbiddenSelection);
		} finally {
			config.enableCustomSharpness = oldSharpness;
			config.enableCrossbowCompat = oldCrossbowCompat;
			config.enableInfinityMending = oldInfinityMending;
		}
	}

	private static boolean compatible(Enchantment first, Enchantment second) {
		return EnchantmentHelper.isCompatible(List.of(first), second);
	}

	private static boolean incompatible(Enchantment first, Enchantment second) {
		return !compatible(first, second);
	}

	private static boolean containsPair(Set<Enchantment> candidates, Enchantment first, Enchantment second) {
		return candidates.contains(first) && candidates.contains(second);
	}

	private static void checkExecutionAndStartElectric(ServerPlayerEntity player) {
		player.getInventory().clear();
		player.clearStatusEffects();
		player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
		player.setHealth(10.0F);
		player.setAbsorptionAmount(0.0F);
		ItemStack execution = enchanted(Items.IRON_SWORD, ModEnchantments.EXECUTION, 1);

		ZombieEntity executable = spawnTarget(player, 100.0F, 10.0F);
		boolean executed = EnchantmentEffects.tryExecuteReport(player, executable, execution);
		check("斩杀阈值内补刀",
				executed && (executable.isDead() || executable.getHealth() <= 0.0F),
				"executed=" + executed + " health=" + executable.getHealth());
		executable.discard();

		ZombieEntity protectedTarget = spawnTarget(player, 100.0F, 10.0F);
		protectedTarget.setInvulnerable(true);
		protectedTarget.timeUntilRegen = 7;
		boolean blocked = EnchantmentEffects.tryExecuteReport(player, protectedTarget, execution);
		check("斩杀被保护拒绝时返回 false 且恢复无敌帧",
				!blocked && protectedTarget.timeUntilRegen == 7,
				"executed=" + blocked + " timeUntilRegen=" + protectedTarget.timeUntilRegen);
		protectedTarget.discard();

		startElectricBlade(player);
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
	}

	private static void startElectricBlade(ServerPlayerEntity player) {
		electricWorld = player.getServerWorld();
		electricPrimary = spawnTarget(player, 100.0F, 100.0F);
		electricFriend = spawnTarget(player, 100.0F, 100.0F);
		electricSecondary = spawnTarget(player, 100.0F, 100.0F);
		electricTertiary = spawnTarget(player, 100.0F, 100.0F);
		for (ZombieEntity target : List.of(
				electricPrimary, electricFriend, electricSecondary, electricTertiary)) {
			target.setNoGravity(true);
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 1200, 0));
		}
		electricPet = net.minecraft.entity.EntityType.WOLF.create(electricWorld);
		if (electricPet == null) {
			throw new IllegalStateException("无法创建测试狼");
		}
		electricPet.setTamed(true);
		electricPet.setOwnerUuid(player.getUuid());
		electricPet.setAiDisabled(true);
		electricPet.setNoGravity(true);
		electricPet.setPersistent();
		electricPet.addCommandTag(TEST_TAG);
		electricPet.setHealth(electricPet.getMaxHealth());
		electricPrimary.setPosition(player.getX() + 3.0D, player.getY() + 10.0D, player.getZ());
		electricFriend.setPosition(player.getX() + 3.4D, player.getY() + 10.0D, player.getZ());
		electricPet.setPosition(player.getX() + 3.6D, player.getY() + 10.0D, player.getZ());
		electricSecondary.setPosition(player.getX() + 4.7D, player.getY() + 10.0D, player.getZ());
		electricTertiary.setPosition(player.getX() + 5.7D, player.getY() + 10.0D, player.getZ());
		electricWorld.spawnEntity(electricPet);

		Scoreboard scoreboard = electricWorld.getScoreboard();
		Team team = scoreboard.getTeam("se_devcheck_friends");
		if (team == null) {
			team = scoreboard.addTeam("se_devcheck_friends");
		}
		scoreboard.addPlayerToTeam(player.getEntityName(), team);
		scoreboard.addPlayerToTeam(electricFriend.getEntityName(), team);
		ItemStack weapon = enchanted(Items.IRON_SWORD, ModEnchantments.ELECTRIC_BLADE, 4);
		ElectricArc.start(player, electricPrimary, weapon, 10.0F, Set.of(electricPrimary.getId()));
	}

	private static void checkElectricBladeResult(ServerPlayerEntity player) {
		float secondLost = electricSecondary == null ? 0.0F : 100.0F - electricSecondary.getHealth();
		float thirdLost = electricTertiary == null ? 0.0F : 100.0F - electricTertiary.getHealth();
		check("电刀跨 tick 链式衰减",
				Math.abs(secondLost - 8.0F) < 0.05F && Math.abs(thirdLost - 6.4F) < 0.05F,
				"second=" + secondLost + " third=" + thirdLost + " primaryHealth="
						+ (electricPrimary == null ? "null" : electricPrimary.getHealth()));
		check("电刀跳过队友与攻击者宠物",
				electricFriend != null && electricPet != null
						&& Math.abs(electricFriend.getHealth() - 100.0F) < 0.01F
						&& Math.abs(electricPet.getHealth() - electricPet.getMaxHealth()) < 0.01F,
				"friend=" + (electricFriend == null ? "null" : electricFriend.getHealth())
						+ " pet=" + (electricPet == null ? "null" : electricPet.getHealth()));
		discardElectricTargets();
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
	}

	private static void discardElectricTargets() {
		if (electricPrimary != null) {
			electricPrimary.discard();
		}
		if (electricSecondary != null) {
			electricSecondary.discard();
		}
		if (electricTertiary != null) {
			electricTertiary.discard();
		}
		if (electricFriend != null) {
			electricFriend.discard();
		}
		if (electricPet != null) {
			electricPet.discard();
		}
		if (electricWorld != null) {
			Team team = electricWorld.getScoreboard().getTeam("se_devcheck_friends");
			if (team != null) {
				electricWorld.getScoreboard().removeTeam(team);
			}
		}
		electricPrimary = null;
		electricSecondary = null;
		electricTertiary = null;
		electricFriend = null;
		electricPet = null;
		electricWorld = null;
	}

	/** 只触发全部 KubeJS 只读事件；无 KubeJS 时核心桥自动短路。 */
	private static void checkKubeJSEvents(ServerPlayerEntity player) {
		ZombieEntity target = spawnTarget(player, 100.0F, 100.0F);
		ItemStack weapon = enchanted(Items.IRON_SWORD, ModEnchantments.SPELLBLADE, 1);
		SummyEnchantmentScriptHooks.fireLifeShieldChanged(player, "selftest", 2.0F, 3.0F, 20.0F);
		SummyEnchantmentScriptHooks.fireDeathEyeChanged(player, null, target, "reset", 2, 0);
		SummyEnchantmentScriptHooks.fireHiddenBladeConsumed(player, target, true, 20, 0.4F, 0.1F);
		SummyEnchantmentScriptHooks.fireMeleeSettled(
				"selftest", player, target, weapon, player.getDamageSources().playerAttack(player),
				10.0F, 12.0F, true, true, 1.0F, 2.0F, 0.5F, 0.25F, false, false, false);
		SummyEnchantmentScriptHooks.fireProjectileSettled(
				"selftest", player, target, target, weapon, player.getDamageSources().playerAttack(player),
				8.0F, 10.0F, true, false, 1.25F, 0.5F, 1.0F, false);
		SummyEnchantmentScriptHooks.fireElectricArcSettled(
				1L, 1, 2, 1, player, target, weapon, player.getDamageSources().playerAttack(player),
				12.0F, 9.6F, true, 0.5F, 1.0F);
		SummyEnchantmentScriptHooks.fireElectricArcFinished(1L, player, weapon, 1, 2, "no_target");
		log("KubeJS 事件桥已触发");
		target.discard();
	}

	private static ZombieEntity spawnTarget(ServerPlayerEntity player, float maxHealth, float health) {
		ServerWorld world = player.getServerWorld();
		ZombieEntity zombie = net.minecraft.entity.EntityType.ZOMBIE.create(world);
		if (zombie == null) {
			throw new IllegalStateException("无法创建测试僵尸");
		}
		zombie.setPosition(player.getX() + 3.0D, player.getY(), player.getZ());
		zombie.setAiDisabled(true);
		zombie.setPersistent();
		zombie.setInvulnerable(false);
		zombie.addCommandTag(TEST_TAG);
		zombie.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
		zombie.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(0.0D);
		zombie.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).setBaseValue(0.0D);
		zombie.setHealth(health);
		zombie.setVelocity(Vec3d.ZERO);
		zombie.timeUntilRegen = 0;
		// 让测试实体具备正常战斗实体“已完成首次 tick”的状态；
		// SE 在 age=0 时会跳过装备附魔读取，避免构造阶段栏位尚未建立导致空指针。
		zombie.age = 1;
		world.spawnEntity(zombie);
		return zombie;
	}

	private static ItemStack enchanted(Item item, Enchantment enchantment, int level) {
		ItemStack stack = new ItemStack(item);
		if (enchantment != null) {
			stack.addEnchantment(enchantment, level);
		}
		return stack;
	}

	private static Enchantment external(String namespace, String path) {
		return Registries.ENCHANTMENT.get(new Identifier(namespace, path));
	}

	private static boolean isLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	private static void check(String label, boolean ok, String detail) {
		checks++;
		if (!ok) {
			FAILURES.add(label + "：" + detail);
		}
		log((ok ? "OK  " : "FAIL") + " " + label + "：" + detail);
	}

	private static void finish(String reason) {
		if (finished) {
			return;
		}
		finished = true;
		log("===== " + reason + "：检查 " + checks + " 项，失败 " + FAILURES.size() + " 项 =====");
		for (String failure : FAILURES) {
			log("FAILED：" + failure);
		}
		MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().stop());
	}

	private static void log(String message) {
		SummyEnchantment.LOGGER.info("[SE-DEVCHECK] {}", message);
	}
}
