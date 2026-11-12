package com.summy.enchantment.devcheck;

import com.summy.enchantment.SummyEnchantment;
import com.summy.enchantment.api.SummyEnchantmentApi;
import com.summy.enchantment.combat.ArrowTraits;
import com.summy.enchantment.combat.CombatFormulas;
import com.summy.enchantment.combat.ElectricArc;
import com.summy.enchantment.combat.EnchantmentEffects;
import com.summy.enchantment.compat.SummyReliquaryCompat;
import com.summy.enchantment.compat.SummyEnchantmentScriptHooks;
import com.summy.enchantment.registry.EnchantmentRef;
import com.summy.enchantment.registry.ModEnchantments;
import com.summy.enchantment.registry.ModStatusEffects;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.screen.AnvilScreenHandler;
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
				SpearDevCheckResult result = SpearDevCheckBridge.tick(player, serverTick);
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
			if ("core".equals(MODE)) {
				switch (serverTick) {
					case 20 -> prepareCoreWorld(player);
					case 40 -> checkCoreRules(player);
					case 60 -> checkLifeShield(player);
					case 80 -> checkLifeMending(player);
					case 100 -> checkDeathsBlessing(player);
					case 110 -> checkSoulGrace(player);
					case 120 -> checkMeleeSettlement(player);
					case 140 -> checkElectricBladeResult(player);
					case 150 -> checkKubeJSEvents(player);
					case 170 -> finish("Fabric 核心自检结束");
					default -> {
					}
				}
				return;
			}
			switch (serverTick) {
				case 20 -> prepareWorld(player);
				case 40 -> checkMajruszOutgoing(player);
				case 60 -> checkMajruszIncoming(player);
				case 70 -> checkKubeJSEvents(player);
				case 100 -> finish("Fabric 客户端自检结束");
				default -> {
				}
			}
		} catch (Throwable throwable) {
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
		check("Fabric 原生 Majrusz 前置已加载", isLoaded("majruszsenchantments")
				&& isLoaded("majruszlibrary"), "world=" + world.getRegistryKey().getValue());
		check("测试世界可写", player.isAlive(), "player=" + player.getName().getString());
		check("破空之愿/本能释放双向互斥",
				areMutuallyIncompatible(ModEnchantments.SKY_WISH, ModEnchantments.INSTINCTIVE_RELEASE),
				"sky_to_inst=" + !compatible(ModEnchantments.SKY_WISH, ModEnchantments.INSTINCTIVE_RELEASE)
						+ " inst_to_sky=" + !compatible(
								ModEnchantments.INSTINCTIVE_RELEASE, ModEnchantments.SKY_WISH));
	}

	private static void checkCoreRules(ServerPlayerEntity player) {
		check("动态附魔 API 数量", SummyEnchantmentApi.enchantmentIds().size() == 32,
				"size=" + SummyEnchantmentApi.enchantmentIds().size());
		check("附魔等级元数据", SummyEnchantmentApi.enchantmentMaxLevel("sharpness_plus") == 5,
				"sharpness_plus=" + SummyEnchantmentApi.enchantmentMaxLevel("sharpness_plus"));
		check("宝藏与诅咒标签",
				SummyEnchantmentApi.isTreasure("execution")
						&& SummyEnchantmentApi.isTreasure("hidden_blade")
						&& SummyEnchantmentApi.isCursed("life_mending"),
				"execution=" + SummyEnchantmentApi.isTreasure("execution")
						+ " hidden_blade=" + SummyEnchantmentApi.isTreasure("hidden_blade")
						+ " life_mending=" + SummyEnchantmentApi.isCursed("life_mending"));
		check("未知附魔查询安全", SummyEnchantmentApi.enchantmentMaxLevel("missing:missing") == 0
						&& SummyEnchantmentApi.enchantmentRarity("missing:missing").isEmpty(),
				"max=" + SummyEnchantmentApi.enchantmentMaxLevel("missing:missing"));

		player.clearStatusEffects();
		player.addStatusEffect(new StatusEffectInstance(
				StatusEffects.STRENGTH, 200, 0, false, false, false));
		check("原版力量重定向为自定义力量",
				player.getStatusEffect(ModStatusEffects.STRENGTH) != null
						&& player.getStatusEffect(StatusEffects.STRENGTH) == null,
				"custom=" + (player.getStatusEffect(ModStatusEffects.STRENGTH) != null)
						+ " vanilla=" + (player.getStatusEffect(StatusEffects.STRENGTH) != null));
		check("力量Plus I 倍率", Math.abs(CombatFormulas.strengthMultiplier(player) - 1.5F) < 0.001F,
				"multiplier=" + CombatFormulas.strengthMultiplier(player));
		player.clearStatusEffects();

		Registry<Enchantment> enchantments = player.getServerWorld().getRegistryManager()
				.get(RegistryKeys.ENCHANTMENT);
		RegistryEntry<Enchantment> protection = enchantments.getEntry(Enchantments.PROTECTION)
				.orElse(null);
		check("保护最大等级为 V", protection != null && protection.value().getMaxLevel() == 5,
				"max=" + (protection == null ? "missing" : protection.value().getMaxLevel()));

		RegistryEntry<Enchantment> power = enchantments.getEntry(Enchantments.POWER).orElse(null);
		boolean originalCrossbowCompat = com.summy.enchantment.config.SummyEnchantmentConfig.get()
				.enableCrossbowCompat;
		try {
			com.summy.enchantment.config.SummyEnchantmentConfig.get().enableCrossbowCompat = true;
			check("弓附魔可用于弩",
					power != null && power.value().isAcceptableItem(new ItemStack(Items.CROSSBOW)),
					"power=" + (power != null));
			check("SE 弩专属附魔可用于弩",
					ModEnchantments.ARROW_VELOCITY.entry() != null
							&& ModEnchantments.ARROW_VELOCITY.entry().value()
									.isAcceptableItem(new ItemStack(Items.CROSSBOW)),
					"arrow_velocity=" + (ModEnchantments.ARROW_VELOCITY.entry() != null));
		} finally {
			com.summy.enchantment.config.SummyEnchantmentConfig.get()
					.enableCrossbowCompat = originalCrossbowCompat;
		}

		RegistryEntry<Enchantment> loyalty = enchantments.getEntry(Enchantments.LOYALTY).orElse(null);
		ItemStack trident = enchanted(Items.TRIDENT, loyalty, 1);
		check("忠诚改版识别", EnchantmentEffects.isLoyaltyReworkActive(trident),
				"loyalty=" + (loyalty != null));
		player.getInventory().clear();
		player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, trident);
		EnchantmentEffects.setLoyaltyThrowCooldown(player);
		check("忠诚假三叉戟冷却已登记",
				player.getItemCooldownManager().isCoolingDown(Items.TRIDENT),
				"cooling=" + player.getItemCooldownManager().isCoolingDown(Items.TRIDENT));
		EnchantmentEffects.tickLoyaltyCooldown(player);
		check("无在飞假三叉戟时冷却撤销",
				!player.getItemCooldownManager().isCoolingDown(Items.TRIDENT),
				"cooling=" + player.getItemCooldownManager().isCoolingDown(Items.TRIDENT));

		AnvilScreenHandler anvil = new AnvilScreenHandler(1, player.getInventory());
		check("铁砧菜单加载与点数成本入口", anvil.getLevelCost() == 0,
				"cost=" + anvil.getLevelCost());

		EnchantmentEffects.clearHiddenBlade(player.getUuid());
		player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,
				enchanted(Items.IRON_SWORD, ModEnchantments.HIDDEN_BLADE, 1));
		for (int i = 0; i < 20; i++) {
			EnchantmentEffects.tickHiddenBlade(player);
		}
		check("藏剑蓄力累计",
				EnchantmentEffects.hiddenBladeChargedTicks(player) == 20
						&& EnchantmentEffects.hiddenBladeBonus(player) > 0.0F,
				"ticks=" + EnchantmentEffects.hiddenBladeChargedTicks(player)
						+ " bonus=" + EnchantmentEffects.hiddenBladeBonus(player));
		EnchantmentEffects.onHiddenBladeSwing(player, null, false);
		check("藏剑挥砍清空并进入冷却",
				EnchantmentEffects.hiddenBladeChargedTicks(player) == 0
						&& EnchantmentEffects.hiddenBladeCooldownRemaining(player) > 0,
				"ticks=" + EnchantmentEffects.hiddenBladeChargedTicks(player)
						+ " cooldown=" + EnchantmentEffects.hiddenBladeCooldownRemaining(player));
		EnchantmentEffects.clearHiddenBlade(player.getUuid());

		ArrowTraits.clearPlayer(player);
		PersistentProjectileEntity arrow = net.minecraft.entity.EntityType.ARROW.create(player.getServerWorld());
		if (arrow == null) {
			check("死亡之眼箭实体创建", false, "EntityType.ARROW.create=null");
		} else {
			arrow.setPosition(player.getX(), player.getY() + 1.0D, player.getZ());
			player.getServerWorld().spawnEntity(arrow);
			ArrowTraits.markDeathEye(arrow, player);
			ZombieEntity target = spawnTarget(player, 100.0F, 100.0F);
			ArrowTraits.recordHit(arrow, target);
			check("死亡之眼命中加层",
					ArrowTraits.hasDeathEye(arrow) && ArrowTraits.stacks(player.getUuid()) == 1,
					"stacks=" + ArrowTraits.stacks(player.getUuid())
							+ " multiplier=" + ArrowTraits.deathEyeMultiplier(arrow));
			ArrowTraits.recordMiss(arrow);
			check("死亡之眼落空清层", ArrowTraits.stacks(player.getUuid()) == 0,
					"stacks=" + ArrowTraits.stacks(player.getUuid()));
			target.discard();
			arrow.discard();
		}
		player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, ItemStack.EMPTY);
	}

	private static void checkMajruszOutgoing(ServerPlayerEntity player) {
		RegistryEntry<Enchantment> majrusz = external(player, "majruszsenchantments", "death_wish");
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
		RegistryEntry<Enchantment> majrusz = external(player, "majruszsenchantments", "death_wish");
		player.getInventory().clear();
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
		ZombieEntity victim = spawnTarget(player, 100.0F, 50.0F);
		victim.equipStack(EquipmentSlot.CHEST,
				enchanted(Items.IRON_CHESTPLATE, ModEnchantments.DEATHS_BLESSING, 1));
		victim.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, enchanted(Items.IRON_SWORD, majrusz, 1));
		victim.damage(player.getDamageSources().playerAttack(player), 10.0F);
		float incomingLost = 50.0F - victim.getHealth();
		check("死神祝福：受伤侧只乘一层", Math.abs(incomingLost - 9.5F) < 0.05F,
				"lost=" + incomingLost + " Majrusz=" + (majrusz != null));
		victim.discard();
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
	}

	private static void checkSoulGrace(ServerPlayerEntity player) {
		player.getInventory().clear();
		player.clearStatusEffects();
		player.equipStack(EquipmentSlot.CHEST,
				enchanted(Items.IRON_CHESTPLATE, ModEnchantments.SOUL_GRACE, 1));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 200, 0));
		player.setHealth(1.0F);
		player.setAbsorptionAmount(0.0F);
		player.timeUntilRegen = 0;
		boolean damaged = player.damage(player.getDamageSources().generic(), 100.0F);
		check("灵魂加护拦截致命伤害",
				damaged && player.isAlive()
						&& Math.abs(player.getHealth() - player.getMaxHealth()) < 0.01F,
				"damaged=" + damaged + " alive=" + player.isAlive() + " health=" + player.getHealth());
		check("灵魂加护清除负面并挂抗性",
				player.getStatusEffect(StatusEffects.POISON) == null
						&& player.getStatusEffect(StatusEffects.RESISTANCE) != null,
				"poison=" + (player.getStatusEffect(StatusEffects.POISON) != null)
						+ " resistance=" + (player.getStatusEffect(StatusEffects.RESISTANCE) != null));
		check("灵魂加护冷却为内置计时显示",
				player.getStatusEffect(ModStatusEffects.SOUL_GRACE) != null,
				"display=" + (player.getStatusEffect(ModStatusEffects.SOUL_GRACE) != null));

		EnchantmentEffects.clearSoulGraceCooldown(player.getUuid());
		player.clearStatusEffects();
		player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
		player.setHealth(player.getMaxHealth());
	}

	private static void checkMeleeSettlement(ServerPlayerEntity player) {
		player.getInventory().clear();
		player.clearStatusEffects();
		player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
		player.setHealth(10.0F);
		player.setAbsorptionAmount(0.0F);
		ItemStack weapon = enchanted(Items.IRON_SWORD, ModEnchantments.SPELLBLADE, 5);
		weapon.addEnchantment(ModEnchantments.LIFESTEAL.entry(), 3);
		check("魔剑/嗜血公式",
				Math.abs(EnchantmentEffects.spellbladeMagic(weapon, 10.0F) - 4.0F) < 0.001F
						&& Math.abs(EnchantmentEffects.lifestealHeal(weapon, 10.0F) - 2.4F) < 0.001F,
				"magic=" + EnchantmentEffects.spellbladeMagic(weapon, 10.0F)
						+ " heal=" + EnchantmentEffects.lifestealHeal(weapon, 10.0F));

		ZombieEntity target = spawnTarget(player, 100.0F, 100.0F);
		SummyReliquaryCompat.settleHit("devcheck", player, weapon, target,
				player.getDamageSources().playerAttack(player), 10.0F, 10.0F,
				true, false, false, true);
		float lost = 100.0F - target.getHealth();
		float healed = player.getHealth() - 10.0F;
		check("魔剑追加伤害落地", Math.abs(lost - 4.0F) < 0.05F, "lost=" + lost);
		check("嗜血按实际伤害回复", Math.abs(healed - 2.4F) < 0.05F, "healed=" + healed);
		target.discard();

		ZombieEntity executable = spawnTarget(player, 100.0F, 10.0F);
		ItemStack execution = enchanted(Items.IRON_SWORD, ModEnchantments.EXECUTION, 1);
		boolean executed = EnchantmentEffects.tryExecuteReport(player, executable, execution);
		check("斩杀阈值内补刀", executed && (executable.isDead() || executable.getHealth() <= 0.0F),
				"executed=" + executed + " health=" + executable.getHealth());
		executable.discard();

		startElectricBlade(player);
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
	}

	private static void startElectricBlade(ServerPlayerEntity player) {
		electricPrimary = spawnTarget(player, 100.0F, 100.0F);
		electricSecondary = spawnTarget(player, 100.0F, 100.0F);
		electricTertiary = spawnTarget(player, 100.0F, 100.0F);
		electricPrimary.setPosition(player.getX() + 3.0D, player.getY(), player.getZ());
		electricSecondary.setPosition(player.getX() + 4.0D, player.getY(), player.getZ());
		electricTertiary.setPosition(player.getX() + 5.0D, player.getY(), player.getZ());
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
		electricPrimary = null;
		electricSecondary = null;
		electricTertiary = null;
	}

	private static void prepareCoreWorld(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		player.getInventory().clear();
		player.clearStatusEffects();
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
		EnchantmentEffects.clearLifeShield(player);
		world.getEntitiesByClass(ZombieEntity.class, player.getBoundingBox().expand(32.0D),
				zombie -> zombie.getCommandTags().contains(TEST_TAG)).forEach(ZombieEntity::discard);

		long loadedEnchantments = ModEnchantments.all().stream()
				.filter(ref -> ref.entry() != null)
				.count();
		check("32 个动态附魔已加载", loadedEnchantments == 32,
				"loaded=" + loadedEnchantments + " dimension=" + world.getRegistryKey().getValue());
		check("测试世界可写", player.isAlive(), "player=" + player.getName().getString());
		check("破空之愿/本能释放双向互斥",
				areMutuallyIncompatible(ModEnchantments.SKY_WISH, ModEnchantments.INSTINCTIVE_RELEASE),
				"sky_to_inst=" + !compatible(ModEnchantments.SKY_WISH, ModEnchantments.INSTINCTIVE_RELEASE)
						+ " inst_to_sky=" + !compatible(
								ModEnchantments.INSTINCTIVE_RELEASE, ModEnchantments.SKY_WISH));
	}

	private static void checkLifeShield(ServerPlayerEntity player) {
		RegistryEntry<Enchantment> celestial = external(
				player, "celestial_enchantments", "life_shield");
		ItemStack shielded = enchanted(Items.IRON_CHESTPLATE, ModEnchantments.LIFE_SHIELD, 5);
		if (celestial != null) {
			shielded.addEnchantment(celestial, 5);
		}
		player.equipStack(EquipmentSlot.CHEST, shielded);
		EnchantmentEffects.clearLifeShield(player);
		player.setAbsorptionAmount(0.0F);
		player.setHealth(10.0F);
		player.heal(5.0F);
		float seShield = EnchantmentEffects.lifeShield(player);
		float absorption = player.getAbsorptionAmount();
		if (celestial != null) {
			check("Celestial 生命护盾：SE 退让", Math.abs(seShield) < 0.01F, "SE=" + seShield);
			check("Celestial 生命护盾：对方结算", absorption >= 5.0F,
					"absorption=" + absorption + " health=" + player.getHealth());
		} else {
			check("SE 生命护盾单装：属性累积", Math.abs(seShield - 5.0F) < 0.01F, "SE=" + seShield);
			check("SE 生命护盾单装：吸收池同步", absorption >= 5.0F, "absorption=" + absorption);
		}

		player.equipStack(EquipmentSlot.CHEST,
				enchanted(Items.IRON_CHESTPLATE, ModEnchantments.LIFE_SHIELD, 5));
		EnchantmentEffects.clearLifeShield(player);
		player.setAbsorptionAmount(0.0F);
		player.setHealth(10.0F);
		player.heal(5.0F);
		check("移除 Celestial 后 SE 恢复", EnchantmentEffects.lifeShield(player) >= 5.0F,
				"SE=" + EnchantmentEffects.lifeShield(player));
		check("SE 生命护盾上限", Math.abs(EnchantmentEffects.lifeShieldCap(player) - 20.0F) < 0.01F,
				"cap=" + EnchantmentEffects.lifeShieldCap(player));
	}

	private static void checkLifeMending(ServerPlayerEntity player) {
		RegistryEntry<Enchantment> apotheosis = external(player, "apotheosis", "life_mending");
		ItemStack chest = damaged(Items.IRON_CHESTPLATE, ModEnchantments.LIFE_MENDING, 3, 20);
		ItemStack boots = apotheosis == null
				? ItemStack.EMPTY
				: damaged(Items.IRON_BOOTS, apotheosis, 1, 20);
		player.getInventory().clear();
		player.equipStack(EquipmentSlot.CHEST, chest);
		player.equipStack(EquipmentSlot.FEET, boots);
		player.setAbsorptionAmount(0.0F);
		EnchantmentEffects.clearLifeShield(player);
		player.setHealth(10.0F);
		player.heal(4.0F);
		check("生命修补：SE 优先", Math.abs(player.getHealth() - 10.0F) < 0.01F,
				"health=" + player.getHealth());
		check("生命修补：SE 修到 12 点", chest.getDamage() == 8,
				"damage=" + chest.getDamage());
		if (!boots.isEmpty()) {
			check("生命修补：Apoth 被跳过", boots.getDamage() == 20,
					"damage=" + boots.getDamage());
			player.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
			boots.setDamage(2);
			player.setHealth(10.0F);
			player.heal(4.0F);
			check("生命修补：SE 无目标后 Apoth 生效", boots.getDamage() == 0,
					"damage=" + boots.getDamage());
			check("生命修补：Apoth 保留剩余治疗", Math.abs(player.getHealth() - 12.0F) < 0.01F,
					"health=" + player.getHealth());
		}
		player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
		player.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);
	}

	private static void checkDeathsBlessing(ServerPlayerEntity player) {
		RegistryEntry<Enchantment> majrusz = external(
				player, "majruszsenchantments", "death_wish");
		player.getInventory().clear();
		player.equipStack(EquipmentSlot.CHEST,
				enchanted(Items.IRON_CHESTPLATE, ModEnchantments.DEATHS_BLESSING, 1));
		if (majrusz != null) {
			player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,
					enchanted(Items.IRON_SWORD, majrusz, 1));
		}
		player.setHealth(10.0F);
		player.setAbsorptionAmount(0.0F);

		ZombieEntity target = spawnTarget(player, 100.0F, 100.0F);
		target.damage(player.getDamageSources().playerAttack(player), 10.0F);
		float outgoingLost = 100.0F - target.getHealth();
		check("死神祝福：造成侧只乘一层", Math.abs(outgoingLost - 15.0F) < 0.05F,
				"lost=" + outgoingLost + " Majrusz=" + (majrusz != null));
		target.discard();

		player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
		player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, ItemStack.EMPTY);
		ZombieEntity victim = spawnTarget(player, 100.0F, 50.0F);
		victim.equipStack(EquipmentSlot.CHEST,
				enchanted(Items.IRON_CHESTPLATE, ModEnchantments.DEATHS_BLESSING, 1));
		if (majrusz != null) {
			victim.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,
					enchanted(Items.IRON_SWORD, majrusz, 1));
		}
		victim.damage(player.getDamageSources().playerAttack(player), 10.0F);
		float incomingLost = 50.0F - victim.getHealth();
		check("死神祝福：受伤侧只乘一层", Math.abs(incomingLost - 9.5F) < 0.05F,
				"lost=" + incomingLost + " Majrusz=" + (majrusz != null));
		victim.discard();

		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
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
		zombie.addStatusEffect(new StatusEffectInstance(
				StatusEffects.FIRE_RESISTANCE, 1200, 0, false, false, false));
		// 与 Forge 自检保持一致：SE 会跳过实体首次 tick，先模拟完成首 tick 再断言装备附魔。
		zombie.age = 1;
		world.spawnEntity(zombie);
		return zombie;
	}

	private static boolean areMutuallyIncompatible(EnchantmentRef first, EnchantmentRef second) {
		return first.entry() != null && second.entry() != null
				&& !compatible(first, second) && !compatible(second, first);
	}

	private static boolean compatible(EnchantmentRef first, EnchantmentRef second) {
		RegistryEntry<Enchantment> firstEntry = first.entry();
		RegistryEntry<Enchantment> secondEntry = second.entry();
		return firstEntry == null || secondEntry == null
				|| EnchantmentHelper.isCompatible(List.of(firstEntry), secondEntry);
	}

	private static ItemStack enchanted(Item item, EnchantmentRef enchantment, int level) {
		return enchanted(item, enchantment.entry(), level);
	}

	private static ItemStack enchanted(Item item, RegistryEntry<Enchantment> enchantment, int level) {
		ItemStack stack = new ItemStack(item);
		if (enchantment != null) {
			stack.addEnchantment(enchantment, level);
		}
		return stack;
	}

	private static ItemStack damaged(
			Item item, EnchantmentRef enchantment, int level, int damage) {
		return damaged(item, enchantment.entry(), level, damage);
	}

	private static ItemStack damaged(
			Item item, RegistryEntry<Enchantment> enchantment, int level, int damage) {
		ItemStack stack = enchanted(item, enchantment, level);
		stack.setDamage(Math.min(damage, stack.getMaxDamage() - 1));
		return stack;
	}

	private static RegistryEntry<Enchantment> external(
			ServerPlayerEntity player, String namespace, String path) {
		Registry<Enchantment> registry = player.getServerWorld().getRegistryManager()
				.get(RegistryKeys.ENCHANTMENT);
		RegistryKey<Enchantment> key = RegistryKey.of(
				RegistryKeys.ENCHANTMENT, Identifier.of(namespace, path));
		return registry.getEntry(key).orElse(null);
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
