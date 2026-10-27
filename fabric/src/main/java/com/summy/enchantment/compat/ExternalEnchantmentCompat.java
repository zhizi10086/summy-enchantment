package com.summy.enchantment.compat;

import com.summy.enchantment.SummyEnchantment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * 对可选附魔模组做无编译依赖的注册表查询。
 *
 * <p>这里只读取公开注册 ID，不引用对方类，也不读取对方配置。这样未安装对方模组时
 * 不会触发类加载；若本模组较早初始化、对方附魔尚未注册，则下次调用会继续尝试解析。
 */
public final class ExternalEnchantmentCompat {
	private static final String CELESTIAL_ENCHANTMENTS = "celestial_enchantments";
	private static final String MAJRUSZ_ENCHANTMENTS = "majruszsenchantments";
	private static final String APOTHEOSIS = "apotheosis";

	private static final Identifier CELESTIAL_LIFE_SHIELD =
			new Identifier(CELESTIAL_ENCHANTMENTS, "life_shield");
	private static final Identifier MAJRUSZ_DEATH_WISH =
			new Identifier(MAJRUSZ_ENCHANTMENTS, "death_wish");
	private static final Identifier APOTHEOSIS_LIFE_MENDING =
			new Identifier(APOTHEOSIS, "life_mending");
	private static final boolean CELESTIAL_LOADED =
			FabricLoader.getInstance().isModLoaded(CELESTIAL_ENCHANTMENTS);
	private static final boolean MAJRUSZ_LOADED =
			FabricLoader.getInstance().isModLoaded(MAJRUSZ_ENCHANTMENTS);
	private static final boolean APOTHEOSIS_LOADED =
			FabricLoader.getInstance().isModLoaded(APOTHEOSIS);

	@Nullable
	private static Enchantment celestialLifeShield;
	@Nullable
	private static Enchantment majruszDeathWish;
	@Nullable
	private static Enchantment apotheosisLifeMending;
	private static boolean compatibilityPolicyLogged;

	private ExternalEnchantmentCompat() {
	}

	/** 当前胸甲是否带有 Celestial 的生命护盾。 */
	public static boolean hasCelestialLifeShield(LivingEntity entity) {
		Enchantment enchantment = celestialLifeShield();
		return entity != null && enchantment != null
				&& EnchantmentHelper.getLevel(enchantment,
						entity.getEquippedStack(EquipmentSlot.CHEST)) > 0;
	}

	/** 当前实体是否带有 Majrusz 的死神之愿；原模组限定主手。 */
	public static boolean hasMajruszDeathWish(LivingEntity entity) {
		Enchantment enchantment = majruszDeathWish();
		if (entity == null || enchantment == null) {
			return false;
		}
		for (ItemStack stack : enchantment.getEquipment(entity).values()) {
			if (EnchantmentHelper.getLevel(enchantment, stack) > 0) {
				return true;
			}
		}
		return false;
	}

	/** 当前实体装备栏是否存在 Apotheosis 生命修补，仅用于调试诊断。 */
	public static boolean hasApotheosisLifeMending(LivingEntity entity) {
		Enchantment enchantment = apotheosisLifeMending();
		if (entity == null || enchantment == null) {
			return false;
		}
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = entity.getEquippedStack(slot);
			if (EnchantmentHelper.getLevel(enchantment, stack) > 0) {
				return true;
			}
		}
		return false;
	}

	/** 启动时只打印一次实际兼容优先级，避免战斗期间刷屏。 */
	public static void logCompatibilityPolicies() {
		if (compatibilityPolicyLogged) {
			return;
		}
		compatibilityPolicyLogged = true;
		if (CELESTIAL_LOADED) {
			SummyEnchantment.LOGGER.info(
					"[SE] 兼容策略：检测到 Celestial 生命护盾时，SE 生命护盾退让");
		}
		if (MAJRUSZ_LOADED) {
			SummyEnchantment.LOGGER.info(
					"[SE] 兼容策略：检测到 Majrusz 死神之愿时，SE 死神祝福退让");
		}
		if (APOTHEOSIS_LOADED) {
			SummyEnchantment.LOGGER.info(
					"[SE] 兼容策略：生命修补由 SE 严格优先，Apotheosis 仅在 SE 无可修装备时生效");
		}
	}

	@Nullable
	private static Enchantment celestialLifeShield() {
		if (celestialLifeShield != null) {
			return celestialLifeShield;
		}
		if (!CELESTIAL_LOADED) {
			return null;
		}
		celestialLifeShield = Registries.ENCHANTMENT.get(CELESTIAL_LIFE_SHIELD);
		return celestialLifeShield;
	}

	@Nullable
	private static Enchantment majruszDeathWish() {
		if (majruszDeathWish != null) {
			return majruszDeathWish;
		}
		if (!MAJRUSZ_LOADED) {
			return null;
		}
		majruszDeathWish = Registries.ENCHANTMENT.get(MAJRUSZ_DEATH_WISH);
		return majruszDeathWish;
	}

	@Nullable
	private static Enchantment apotheosisLifeMending() {
		if (apotheosisLifeMending != null) {
			return apotheosisLifeMending;
		}
		if (!APOTHEOSIS_LOADED) {
			return null;
		}
		apotheosisLifeMending = Registries.ENCHANTMENT.get(APOTHEOSIS_LIFE_MENDING);
		return apotheosisLifeMending;
	}
}
