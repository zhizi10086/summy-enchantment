package com.summy.enchantment.devcheck;

import com.summy.enchantment.SummyEnchantment;
import com.summy.enchantment.combat.CombatFormulas;
import com.summy.enchantment.combat.ElectricArc;
import com.summy.enchantment.combat.EnchantmentEffects;
import com.summy.enchantment.compat.SummyEnchantmentScriptHooks;
import com.summy.enchantment.registry.ModEnchantments;
import com.summy.enchantment.registry.ModStatusEffects;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
				case 40 -> checkMajruszOutgoing(player);
				case 60 -> checkMajruszIncoming(player);
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
