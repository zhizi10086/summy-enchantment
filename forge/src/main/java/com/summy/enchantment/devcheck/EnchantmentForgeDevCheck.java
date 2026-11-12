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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.inventory.AnvilMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SE 的真实客户端开发自检。
 *
 * <p>只通过 {@code gradlew runClient -Pdevcheck} 启用；测试世界会清理玩家背包与装备，
 * 因此只能使用专用存档 {@code SeDevCheckWorld}。正式 jar 不包含本包。
 */
@EventBusSubscriber(modid = SummyEnchantment.MOD_ID, value = Dist.CLIENT)
public final class EnchantmentForgeDevCheck {
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
	private static Zombie electricPrimary;
	private static Zombie electricSecondary;
	private static Zombie electricTertiary;

	private EnchantmentForgeDevCheck() {
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (!ENABLED) {
			return;
		}
		if (event.getServer().getPlayerList().getPlayers().isEmpty()) {
			serverTick = 0;
			return;
		}
		ServerPlayer player = event.getServer().getPlayerList().getPlayers().get(0);
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
					finish("Forge 矛兼容自检结束");
				}
				return;
			}
			if ("kubejs".equals(MODE)) {
				if (serverTick == 40) {
					checkKubeJSEvents(player);
				} else if (serverTick == 60) {
					finish("KubeJS-only 自检结束");
				}
				return;
			}
			if ("core".equals(MODE)) {
				switch (serverTick) {
					case 20 -> prepareWorld(player);
					case 40 -> checkCoreRules(player);
					case 60 -> checkLifeShield(player);
					case 80 -> checkLifeMending(player);
					case 100 -> checkDeathsBlessing(player);
					case 110 -> checkSoulGrace(player);
					case 120 -> checkMeleeSettlement(player);
					case 140 -> checkElectricBladeResult(player);
					case 150 -> checkKubeJSEvents(player);
					case 170 -> finish("核心自检结束");
					default -> {
					}
				}
				return;
			}
			switch (serverTick) {
				case 20 -> prepareWorld(player);
				case 30 -> checkCompatibilityFixtures(player);
				case 40 -> checkLifeShield(player);
				case 60 -> checkLifeMending(player);
				case 80 -> checkDeathsBlessing(player);
				case 90 -> checkKubeJSEvents(player);
				case 110 -> finish("客户端自检结束");
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

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		if (!ENABLED) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		totalClientTicks++;
		if (totalClientTicks > CLIENT_TOTAL_STOP_TICKS) {
			finish("硬停止：客户端总 tick 超限");
			return;
		}
		if (client.player == null || client.level == null) {
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

	private static void prepareWorld(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		player.getInventory().clearContent();
		player.removeAllEffects();
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
		EnchantmentEffects.clearLifeShield(player);
		level.getEntitiesOfClass(Zombie.class, player.getBoundingBox().inflate(32.0D),
				zombie -> zombie.getTags().contains(TEST_TAG)).forEach(Zombie::discard);
		log("测试准备：维度=" + level.dimension().location()
				+ "，Celestial=" + isLoaded("celestial_enchantments")
				+ "，Apotheosis=" + isLoaded("apotheosis")
				+ "，Majrusz=" + isLoaded("majruszsenchantments")
				+ "，Farmer's Delight=" + isLoaded("farmersdelight")
				+ "，Twilight Dusk=" + isLoaded("twilight_dusk"));
		check("基础环境", level != null && player.isAlive(), "player=" + player.getName().getString());
		check("破空之愿/本能释放双向互斥",
				areMutuallyIncompatible(ModEnchantments.SKY_WISH, ModEnchantments.INSTINCTIVE_RELEASE),
				"sky_to_inst=" + !compatible(ModEnchantments.SKY_WISH, ModEnchantments.INSTINCTIVE_RELEASE)
						+ " inst_to_sky=" + !compatible(
								ModEnchantments.INSTINCTIVE_RELEASE, ModEnchantments.SKY_WISH));
	}

	private static void checkCoreRules(ServerPlayer player) {
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

		player.removeAllEffects();
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, false, false, false));
		check("原版力量重定向为自定义力量",
				player.hasEffect(ModStatusEffects.STRENGTH) && !player.hasEffect(MobEffects.DAMAGE_BOOST),
				"custom=" + player.hasEffect(ModStatusEffects.STRENGTH)
						+ " vanilla=" + player.hasEffect(MobEffects.DAMAGE_BOOST));
		check("力量Plus I 倍率", Math.abs(CombatFormulas.strengthMultiplier(player) - 1.5F) < 0.001F,
				"multiplier=" + CombatFormulas.strengthMultiplier(player));
		player.removeAllEffects();

		var enchantments = player.serverLevel().registryAccess()
				.registryOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> protection = enchantments.getHolder(Enchantments.PROTECTION).orElse(null);
		check("保护最大等级为 V", protection != null && protection.value().getMaxLevel() == 5,
				"max=" + (protection == null ? "missing" : protection.value().getMaxLevel()));

		Holder<Enchantment> power = enchantments.getHolder(Enchantments.POWER).orElse(null);
		boolean originalCrossbowCompat = com.summy.enchantment.config.SummyEnchantmentConfig.get()
				.enableCrossbowCompat;
		try {
			com.summy.enchantment.config.SummyEnchantmentConfig.get().enableCrossbowCompat = true;
			check("弓附魔可用于弩",
					power != null && power.value().canEnchant(new ItemStack(Items.CROSSBOW)),
					"power=" + (power != null));
			check("SE 弩专属附魔可用于弩",
					ModEnchantments.ARROW_VELOCITY.entry() != null
							&& ModEnchantments.ARROW_VELOCITY.entry().value()
									.canEnchant(new ItemStack(Items.CROSSBOW)),
					"arrow_velocity=" + (ModEnchantments.ARROW_VELOCITY.entry() != null));
		} finally {
			com.summy.enchantment.config.SummyEnchantmentConfig.get()
					.enableCrossbowCompat = originalCrossbowCompat;
		}

		Holder<Enchantment> loyalty = enchantments.getHolder(Enchantments.LOYALTY).orElse(null);
		ItemStack trident = enchanted(Items.TRIDENT, loyalty, 1);
		check("忠诚改版识别", EnchantmentEffects.isLoyaltyReworkActive(trident),
				"loyalty=" + (loyalty != null));
		player.getInventory().clearContent();
		player.setItemSlot(EquipmentSlot.MAINHAND, trident);
		EnchantmentEffects.setLoyaltyThrowCooldown(player);
		check("忠诚假三叉戟冷却已登记", player.getCooldowns().isOnCooldown(Items.TRIDENT),
				"cooling=" + player.getCooldowns().isOnCooldown(Items.TRIDENT));
		EnchantmentEffects.tickLoyaltyCooldown(player);
		check("无在飞假三叉戟时冷却撤销", !player.getCooldowns().isOnCooldown(Items.TRIDENT),
				"cooling=" + player.getCooldowns().isOnCooldown(Items.TRIDENT));

		AnvilMenu anvil = new AnvilMenu(1, player.getInventory());
		check("铁砧菜单加载与点数成本入口", anvil.getCost() == 0, "cost=" + anvil.getCost());

		EnchantmentEffects.clearHiddenBlade(player.getUUID());
		player.setItemSlot(EquipmentSlot.MAINHAND,
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
		EnchantmentEffects.clearHiddenBlade(player.getUUID());

		ArrowTraits.clearPlayer(player);
		AbstractArrow arrow = EntityType.ARROW.create(player.serverLevel());
		if (arrow == null) {
			check("死亡之眼箭实体创建", false, "EntityType.ARROW.create=null");
		} else {
			arrow.moveTo(player.getX(), player.getY() + 1.0D, player.getZ(), 0.0F, 0.0F);
			player.serverLevel().addFreshEntity(arrow);
			ArrowTraits.markDeathEye(arrow, player);
			Zombie target = spawnTarget(player, 100.0F, 100.0F);
			ArrowTraits.recordHit(arrow, target);
			check("死亡之眼命中加层",
					ArrowTraits.hasDeathEye(arrow) && ArrowTraits.stacks(player.getUUID()) == 1,
					"stacks=" + ArrowTraits.stacks(player.getUUID())
							+ " multiplier=" + ArrowTraits.deathEyeMultiplier(arrow));
			ArrowTraits.recordMiss(arrow);
			check("死亡之眼落空清层", ArrowTraits.stacks(player.getUUID()) == 0,
					"stacks=" + ArrowTraits.stacks(player.getUUID()));
			target.discard();
			arrow.discard();
		}
		player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
	}

	/**
	 * 用官方 Farmer's Delight 1.3.4 与 Twilight Dusk 1.2fix1 做兼容行为断言。
	 *
	 * <p>版本号固定检查可以避免测试目录里残留旧 JAR 时“看似通过、实际没测到目标版本”。
	 */
	private static void checkCompatibilityFixtures(ServerPlayer player) {
		if (isLoaded("farmersdelight")) {
			String version = modVersion("farmersdelight");
			check("Farmer's Delight 官方测试版本", "1.20.1-1.3.4".equals(version), "version=" + version);
			checkBigStomachWithNourishment(player);
		}
		if (isLoaded("twilight_dusk")) {
			String version = modVersion("twilight_dusk");
			check("Twilight Dusk 官方测试版本", "1.2fix1".equals(version), "version=" + version);
			checkTwilightGravityCoexistence(player);
		}
	}

	/** 大胃袋的 40 点容量必须真实生效，且 Farmer's Delight 的滋养仍可让满值玩家进食。 */
	private static void checkBigStomachWithNourishment(ServerPlayer player) {
		player.setItemSlot(EquipmentSlot.CHEST,
				enchanted(Items.IRON_CHESTPLATE, ModEnchantments.BIG_STOMACH, 5));
		FoodData food = player.getFoodData();
		food.setFoodLevel(0);
		food.setSaturation(0.0F);
		food.eat(30, 0.0F);
		check("大胃袋与饥饿值上限共存：第一段进食", food.getFoodLevel() == 30,
				"food=" + food.getFoodLevel() + " cap=" + EnchantmentEffects.foodLevelCap(player));
		food.eat(20, 0.0F);
		check("大胃袋与饥饿值上限共存：到达 40", food.getFoodLevel() == 40,
				"food=" + food.getFoodLevel() + " cap=" + EnchantmentEffects.foodLevelCap(player));
		check("大胃袋与饥饿值上限共存：满值时 needsFood=false", !food.needsFood(),
				"food=" + food.getFoodLevel());

		Holder<MobEffect> nourishment = player.serverLevel().registryAccess()
				.registryOrThrow(Registries.MOB_EFFECT)
				.getHolder(ResourceKey.create(Registries.MOB_EFFECT,
						ResourceLocation.fromNamespaceAndPath("farmersdelight", "nourishment")))
				.orElse(null);
		if (nourishment == null) {
			check("Farmer's Delight 滋养效果已注册", false, "farmersdelight:nourishment=null");
		} else {
			player.addEffect(new MobEffectInstance(nourishment, 200, 0));
			check("Farmer's Delight 滋养在满值时仍允许进食", player.canEat(false),
					"food=" + food.getFoodLevel() + " nourishment=" + player.hasEffect(nourishment));
			player.removeEffect(nourishment);
		}

		player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
		food.setFoodLevel(20);
		food.setSaturation(5.0F);
	}

	/**
	 * Twilight Dusk 只把幻影箭的原版重力常量减半；SE 应在此基础上再乘轻盈倍率。
	 *
	 * <p>不依赖私有字段或硬编码 0.05：直接让四支箭各 tick 一次，比较实际纵向速度。
	 */
	private static void checkTwilightGravityCoexistence(ServerPlayer player) {
		try {
			float normal = sampleArrowGravity(player, "github.gold_block.entity.KnightArrow", 0);
			float phantom = sampleArrowGravity(player, "github.gold_block.entity.PhantomKnightArrow", 0);
			float lightNormal = sampleArrowGravity(
					player, "github.gold_block.entity.KnightArrow", 2);
			float lightPhantom = sampleArrowGravity(
					player, "github.gold_block.entity.PhantomKnightArrow", 2);
			float twilightRatio = phantom / normal;
			float seOnNormalRatio = lightNormal / normal;
			float combinedOnPhantomRatio = lightPhantom / phantom;
			float expectedSeRatio = 1.0F - Math.min(0.9F,
					2.0F * com.summy.enchantment.config.SummyEnchantmentConfig.get()
							.lightnessGravityReductionPerLevel);

			check("Twilight Dusk 幻影箭重力为普通箭的一半",
					Math.abs(twilightRatio - 0.5F) < 0.01F,
					"normal=" + normal + " phantom=" + phantom + " ratio=" + twilightRatio);
			check("SE 轻盈与 Twilight Dusk 重力乘算（普通箭）",
					Math.abs(seOnNormalRatio - expectedSeRatio) < 0.01F,
					"ratio=" + seOnNormalRatio + " expected=" + expectedSeRatio);
			check("SE 轻盈与 Twilight Dusk 重力乘算（幻影箭）",
					Math.abs(combinedOnPhantomRatio - expectedSeRatio) < 0.01F,
					"ratio=" + combinedOnPhantomRatio + " expected=" + expectedSeRatio);
		} catch (ReflectiveOperationException exception) {
			check("Twilight Dusk 测试箭构造", false, exception.toString());
		}
	}

	private static float sampleArrowGravity(ServerPlayer player, String className, int lightness)
			throws ReflectiveOperationException {
		Class<?> arrowClass = Class.forName(className);
		Constructor<?> constructor = arrowClass.getConstructor(
				net.minecraft.world.level.Level.class, LivingEntity.class);
		AbstractArrow arrow = (AbstractArrow) constructor.newInstance(player.serverLevel(), player);
		arrow.moveTo(player.getX() + 4.0D, 160.0D, player.getZ(), 0.0F, 0.0F);
		arrow.setDeltaMovement(Vec3.ZERO);
		ArrowTraits.setServerLightness(arrow, lightness);
		player.serverLevel().addFreshEntity(arrow);
		arrow.tick();
		float velocityY = (float) arrow.getDeltaMovement().y;
		ArrowTraits.setServerLightness(arrow, 0);
		arrow.discard();
		return velocityY;
	}

	private static void checkLifeShield(ServerPlayer player) {
		Holder<Enchantment> celestial = external(player, "celestial_enchantments", "life_shield");
		ItemStack both = enchanted(Items.IRON_CHESTPLATE, ModEnchantments.LIFE_SHIELD, 5);
		if (celestial != null) {
			both.enchant(celestial, 5);
		}
		player.setItemSlot(EquipmentSlot.CHEST, both);
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

		player.setItemSlot(EquipmentSlot.CHEST,
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

	private static void checkLifeMending(ServerPlayer player) {
		Holder<Enchantment> apotheosis = external(player, "apotheosis", "life_mending");
		ItemStack chest = damaged(Items.IRON_CHESTPLATE, ModEnchantments.LIFE_MENDING, 3, 20);
		ItemStack boots = apotheosis == null
				? ItemStack.EMPTY
				: damaged(Items.IRON_BOOTS, apotheosis, 1, 20);
		player.getInventory().clearContent();
		player.setItemSlot(EquipmentSlot.CHEST, chest);
		player.setItemSlot(EquipmentSlot.FEET, boots);
		player.setAbsorptionAmount(0.0F);
		EnchantmentEffects.clearLifeShield(player);
		player.setHealth(10.0F);
		player.heal(4.0F);
		check("生命修补：SE 优先", Math.abs(player.getHealth() - 10.0F) < 0.01F,
				"health=" + player.getHealth());
		check("生命修补：SE 修到 12 点", chest.getDamageValue() == 8,
				"damage=" + chest.getDamageValue());
		if (!boots.isEmpty()) {
			check("生命修补：Apoth 被跳过", boots.getDamageValue() == 20,
					"damage=" + boots.getDamageValue());
			player.setItemSlot(EquipmentSlot.CHEST,
					new ItemStack(Items.IRON_CHESTPLATE));
			boots.setDamageValue(2);
			player.setHealth(10.0F);
			player.heal(4.0F);
			check("生命修补：SE 无目标后 Apoth 生效", boots.getDamageValue() == 0,
					"damage=" + boots.getDamageValue());
			check("生命修补：Apoth 保留剩余治疗", Math.abs(player.getHealth() - 12.0F) < 0.01F,
					"health=" + player.getHealth());
		}
		player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
		player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
	}

	private static void checkDeathsBlessing(ServerPlayer player) {
		Holder<Enchantment> majrusz = external(player, "majruszsenchantments", "death_wish");
		player.getInventory().clearContent();
		player.setItemSlot(EquipmentSlot.CHEST,
				enchanted(Items.IRON_CHESTPLATE, ModEnchantments.DEATHS_BLESSING, 1));
		if (majrusz != null) {
			player.setItemSlot(EquipmentSlot.MAINHAND,
					enchanted(Items.IRON_SWORD, majrusz, 1));
		}
		player.setHealth(10.0F);
		player.setAbsorptionAmount(0.0F);

		Zombie target = spawnTarget(player, 100.0F, 100.0F);
		target.hurt(player.damageSources().playerAttack(player), 10.0F);
		float outgoingLost = 100.0F - target.getHealth();
		check("死神祝福：造成侧只乘一层", Math.abs(outgoingLost - 15.0F) < 0.05F,
				"lost=" + outgoingLost + " Majrusz=" + (majrusz != null));
		target.discard();

		player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
		player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		Zombie victim = spawnTarget(player, 100.0F, 50.0F);
		victim.setItemSlot(EquipmentSlot.CHEST,
				enchanted(Items.IRON_CHESTPLATE, ModEnchantments.DEATHS_BLESSING, 1));
		if (majrusz != null) {
			victim.setItemSlot(EquipmentSlot.MAINHAND,
					enchanted(Items.IRON_SWORD, majrusz, 1));
		}
		victim.hurt(player.damageSources().playerAttack(player), 10.0F);
		float incomingLost = 50.0F - victim.getHealth();
		check("死神祝福：受伤侧只乘一层", Math.abs(incomingLost - 9.5F) < 0.05F,
				"lost=" + incomingLost + " Majrusz=" + (majrusz != null));
		victim.discard();

		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
	}

	private static void checkSoulGrace(ServerPlayer player) {
		player.getInventory().clearContent();
		player.removeAllEffects();
		player.setItemSlot(EquipmentSlot.CHEST,
				enchanted(Items.IRON_CHESTPLATE, ModEnchantments.SOUL_GRACE, 1));
		player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
		player.setHealth(1.0F);
		player.setAbsorptionAmount(0.0F);
		player.invulnerableTime = 0;
		boolean damaged = player.hurt(player.damageSources().generic(), 100.0F);
		check("灵魂加护拦截致命伤害",
				damaged && player.isAlive()
						&& Math.abs(player.getHealth() - player.getMaxHealth()) < 0.01F,
				"damaged=" + damaged + " alive=" + player.isAlive() + " health=" + player.getHealth());
		check("灵魂加护清除负面并挂抗性",
				!player.hasEffect(MobEffects.POISON) && player.hasEffect(MobEffects.DAMAGE_RESISTANCE),
				"poison=" + player.hasEffect(MobEffects.POISON)
						+ " resistance=" + player.hasEffect(MobEffects.DAMAGE_RESISTANCE));
		check("灵魂加护冷却为内置计时显示", player.hasEffect(ModStatusEffects.SOUL_GRACE),
				"display=" + player.hasEffect(ModStatusEffects.SOUL_GRACE));

		EnchantmentEffects.clearSoulGraceCooldown(player.getUUID());
		player.removeAllEffects();
		player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
		player.setHealth(player.getMaxHealth());
	}

	private static void checkMeleeSettlement(ServerPlayer player) {
		player.getInventory().clearContent();
		player.removeAllEffects();
		player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
		player.setHealth(10.0F);
		player.setAbsorptionAmount(0.0F);
		ItemStack weapon = enchanted(Items.IRON_SWORD, ModEnchantments.SPELLBLADE, 5);
		weapon.enchant(ModEnchantments.LIFESTEAL.entry(), 3);
		check("魔剑/嗜血公式",
				Math.abs(EnchantmentEffects.spellbladeMagic(weapon, 10.0F) - 4.0F) < 0.001F
						&& Math.abs(EnchantmentEffects.lifestealHeal(weapon, 10.0F) - 2.4F) < 0.001F,
				"magic=" + EnchantmentEffects.spellbladeMagic(weapon, 10.0F)
						+ " heal=" + EnchantmentEffects.lifestealHeal(weapon, 10.0F));

		Zombie target = spawnTarget(player, 100.0F, 100.0F);
		SummyReliquaryCompat.settleHit("devcheck", player, weapon, target,
				player.damageSources().playerAttack(player), 10.0F, 10.0F,
				true, false, false, true);
		float lost = 100.0F - target.getHealth();
		float healed = player.getHealth() - 10.0F;
		check("魔剑追加伤害落地", Math.abs(lost - 4.0F) < 0.05F,
				"lost=" + lost);
		check("嗜血按实际伤害回复", Math.abs(healed - 2.4F) < 0.05F,
				"healed=" + healed);
		target.discard();

		Zombie executable = spawnTarget(player, 100.0F, 10.0F);
		ItemStack execution = enchanted(Items.IRON_SWORD, ModEnchantments.EXECUTION, 1);
		boolean executed = EnchantmentEffects.tryExecuteReport(player, executable, execution);
		check("斩杀阈值内补刀",
				executed && (executable.isDeadOrDying() || executable.getHealth() <= 0.0F),
				"executed=" + executed + " health=" + executable.getHealth());
		executable.discard();

		startElectricBlade(player);
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
	}

	private static void startElectricBlade(ServerPlayer player) {
		electricPrimary = spawnTarget(player, 100.0F, 100.0F);
		electricSecondary = spawnTarget(player, 100.0F, 100.0F);
		electricTertiary = spawnTarget(player, 100.0F, 100.0F);
		electricPrimary.moveTo(player.getX() + 3.0D, player.getY(), player.getZ(), 0.0F, 0.0F);
		electricSecondary.moveTo(player.getX() + 4.0D, player.getY(), player.getZ(), 0.0F, 0.0F);
		electricTertiary.moveTo(player.getX() + 5.0D, player.getY(), player.getZ(), 0.0F, 0.0F);
		ItemStack weapon = enchanted(Items.IRON_SWORD, ModEnchantments.ELECTRIC_BLADE, 4);
		ElectricArc.start(player, electricPrimary, weapon, 10.0F, Set.of(electricPrimary.getId()));
	}

	private static void checkElectricBladeResult(ServerPlayer player) {
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

	/** 只触发全部 KubeJS 只读事件；无 KubeJS 时核心桥自动短路。 */
	private static void checkKubeJSEvents(ServerPlayer player) {
		Zombie target = spawnTarget(player, 100.0F, 100.0F);
		ItemStack weapon = enchanted(Items.IRON_SWORD, ModEnchantments.SPELLBLADE, 1);
		SummyEnchantmentScriptHooks.fireLifeShieldChanged(player, "selftest", 2.0F, 3.0F, 20.0F);
		SummyEnchantmentScriptHooks.fireDeathEyeChanged(player, null, target, "reset", 2, 0);
		SummyEnchantmentScriptHooks.fireHiddenBladeConsumed(player, target, true, 20, 0.4F, 0.1F);
		SummyEnchantmentScriptHooks.fireMeleeSettled(
				"selftest", player, target, weapon, player.damageSources().playerAttack(player),
				10.0F, 12.0F, true, true, 1.0F, 2.0F, 0.5F, 0.25F, false, false, false);
		SummyEnchantmentScriptHooks.fireProjectileSettled(
				"selftest", player, target, target, weapon, player.damageSources().playerAttack(player),
				8.0F, 10.0F, true, false, 1.25F, 0.5F, 1.0F, false);
		SummyEnchantmentScriptHooks.fireElectricArcSettled(
				1L, 1, 2, 1, player, target, weapon, player.damageSources().playerAttack(player),
				12.0F, 9.6F, true, 0.5F, 1.0F);
		SummyEnchantmentScriptHooks.fireElectricArcFinished(1L, player, weapon, 1, 2, "no_target");
		log("KubeJS 事件桥已触发");
		target.discard();
	}

	private static Zombie spawnTarget(ServerPlayer player, float maxHealth, float health) {
		ServerLevel level = player.serverLevel();
		Zombie zombie = EntityType.ZOMBIE.create(level);
		if (zombie == null) {
			throw new IllegalStateException("无法创建测试僵尸");
		}
		zombie.moveTo(player.getX() + 3.0D, player.getY(), player.getZ(), 0.0F, 0.0F);
		zombie.setNoAi(true);
		zombie.setPersistenceRequired();
		zombie.setInvulnerable(false);
		zombie.addTag(TEST_TAG);
		zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
		zombie.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
		zombie.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0.0D);
		zombie.setHealth(health);
		zombie.setDeltaMovement(Vec3.ZERO);
		zombie.invulnerableTime = 0;
		zombie.addEffect(new MobEffectInstance(
				MobEffects.FIRE_RESISTANCE, 1200, 0, false, false, false));
		// 让测试僵尸具备正常战斗实体应有的“已完成首次 tick”状态；
		// SE 在 tickCount=0 时会跳过装备附魔读取，避免构造阶段装备栏尚未建立导致空指针。
		zombie.tickCount = 1;
		level.addFreshEntity(zombie);
		return zombie;
	}

	private static boolean areMutuallyIncompatible(EnchantmentRef first, EnchantmentRef second) {
		return first.entry() != null && second.entry() != null
				&& !compatible(first, second) && !compatible(second, first);
	}

	private static boolean compatible(EnchantmentRef first, EnchantmentRef second) {
		Holder<Enchantment> firstEntry = first.entry();
		Holder<Enchantment> secondEntry = second.entry();
		return firstEntry == null || secondEntry == null
				|| Enchantment.areCompatible(firstEntry, secondEntry);
	}

	private static ItemStack enchanted(Item item, EnchantmentRef enchantment, int level) {
		return enchanted(item, enchantment.entry(), level);
	}

	private static ItemStack enchanted(Item item, Holder<Enchantment> enchantment, int level) {
		ItemStack stack = new ItemStack(item);
		if (enchantment != null) {
			stack.enchant(enchantment, level);
		}
		return stack;
	}

	private static ItemStack damaged(Item item, EnchantmentRef enchantment, int level, int damage) {
		return damaged(item, enchantment.entry(), level, damage);
	}

	private static ItemStack damaged(Item item, Holder<Enchantment> enchantment, int level, int damage) {
		ItemStack stack = enchanted(item, enchantment, level);
		stack.setDamageValue(Math.min(damage, stack.getMaxDamage() - 1));
		return stack;
	}

	private static Holder<Enchantment> external(ServerPlayer player, String namespace, String path) {
		return player.serverLevel().registryAccess()
				.registryOrThrow(Registries.ENCHANTMENT)
				.getHolder(ResourceKey.create(Registries.ENCHANTMENT,
						ResourceLocation.fromNamespaceAndPath(namespace, path)))
				.orElse(null);
	}

	private static boolean isLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}

	private static String modVersion(String modId) {
		return ModList.get().getModContainerById(modId)
				.map(container -> container.getModInfo().getVersion().toString())
				.orElse("missing");
	}

	private static void check(String label, boolean ok, String detail) {
		checks++;
		if (!ok) {
			FAILURES.add(label + "｜" + detail);
		}
		log((ok ? "OK  " : "FAIL") + " " + label + "｜" + detail);
	}

	private static void finish(String reason) {
		if (finished) {
			return;
		}
		finished = true;
		log("===== " + reason + "：检查 " + checks + " 项，失败 " + FAILURES.size() + " 项 =====");
		for (String failure : FAILURES) {
			log("FAILED｜" + failure);
		}
		Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
	}

	private static void log(String message) {
		SummyEnchantment.LOGGER.info("[SE-DEVCHECK] {}", message);
	}
}
