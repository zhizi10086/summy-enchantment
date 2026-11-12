package com.summy.enchantment.compat;

import com.summy.enchantment.SummyEnchantment;
import com.summy.enchantment.registry.EnchantmentRef;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * 对可选附魔模组做无编译依赖的动态注册表查询。
 */
public final class ExternalEnchantmentCompat {
	private static final String CELESTIAL_ENCHANTMENTS = "celestial_enchantments";
	private static final String MAJRUSZ_ENCHANTMENTS = "majruszsenchantments";
	private static final String APOTHEOSIS = "apotheosis";

	private static final Identifier CELESTIAL_LIFE_SHIELD =
			Identifier.of(CELESTIAL_ENCHANTMENTS, "life_shield");
	private static final Identifier MAJRUSZ_DEATH_WISH =
			Identifier.of(MAJRUSZ_ENCHANTMENTS, "death_wish");
	private static final Identifier APOTHEOSIS_LIFE_MENDING =
			Identifier.of(APOTHEOSIS, "life_mending");
	private static final boolean CELESTIAL_LOADED =
			FabricLoader.getInstance().isModLoaded(CELESTIAL_ENCHANTMENTS);
	private static final boolean MAJRUSZ_LOADED =
			FabricLoader.getInstance().isModLoaded(MAJRUSZ_ENCHANTMENTS);
	private static final boolean APOTHEOSIS_LOADED =
			FabricLoader.getInstance().isModLoaded(APOTHEOSIS);
	private static boolean compatibilityPolicyLogged;

	private ExternalEnchantmentCompat() {
	}

	public static boolean hasCelestialLifeShield(LivingEntity entity) {
		return entity != null && CELESTIAL_LOADED
				&& level(entity.getEquippedStack(EquipmentSlot.CHEST), CELESTIAL_LIFE_SHIELD) > 0;
	}

	public static boolean hasMajruszDeathWish(LivingEntity entity) {
		return MAJRUSZ_LOADED && hasOnEquipment(entity, MAJRUSZ_DEATH_WISH);
	}

	public static boolean hasApotheosisLifeMending(LivingEntity entity) {
		return APOTHEOSIS_LOADED && hasOnEquipment(entity, APOTHEOSIS_LIFE_MENDING);
	}

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

	private static boolean hasOnEquipment(LivingEntity entity, Identifier id) {
		if (entity == null) {
			return false;
		}
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (level(entity.getEquippedStack(slot), id) > 0) {
				return true;
			}
		}
		return false;
	}

	private static int level(ItemStack stack, Identifier id) {
		return EnchantmentRef.level(stack, RegistryKey.of(RegistryKeys.ENCHANTMENT, id));
	}
}
