package com.summy.enchantment.compat;

import com.summy.enchantment.SummyEnchantment;
import com.summy.enchantment.registry.EnchantmentRef;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * 对可选附魔模组做无编译依赖的动态注册表查询。
 */
public final class ExternalEnchantmentCompat {
	private static final String CELESTIAL_ENCHANTMENTS = "celestial_enchantments";
	private static final String MAJRUSZ_ENCHANTMENTS = "majruszsenchantments";
	private static final String APOTHEOSIS = "apotheosis";
	private static final ResourceLocation CELESTIAL_LIFE_SHIELD =
			ResourceLocation.fromNamespaceAndPath(CELESTIAL_ENCHANTMENTS, "life_shield");
	private static final ResourceLocation MAJRUSZ_DEATH_WISH =
			ResourceLocation.fromNamespaceAndPath(MAJRUSZ_ENCHANTMENTS, "death_wish");
	private static final ResourceLocation APOTHEOSIS_LIFE_MENDING =
			ResourceLocation.fromNamespaceAndPath(APOTHEOSIS, "life_mending");
	private static final boolean CELESTIAL_LOADED = ModList.get().isLoaded(CELESTIAL_ENCHANTMENTS);
	private static final boolean MAJRUSZ_LOADED = ModList.get().isLoaded(MAJRUSZ_ENCHANTMENTS);
	private static final boolean APOTHEOSIS_LOADED = ModList.get().isLoaded(APOTHEOSIS);
	private static boolean compatibilityPolicyLogged;

	private ExternalEnchantmentCompat() {
	}

	public static boolean hasCelestialLifeShield(LivingEntity entity) {
		return entity != null && CELESTIAL_LOADED
				&& level(entity.getItemBySlot(EquipmentSlot.CHEST), CELESTIAL_LIFE_SHIELD) > 0;
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

	private static boolean hasOnEquipment(LivingEntity entity, ResourceLocation id) {
		if (entity == null) {
			return false;
		}
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (level(entity.getItemBySlot(slot), id) > 0) {
				return true;
			}
		}
		return false;
	}

	private static int level(ItemStack stack, ResourceLocation id) {
		return EnchantmentRef.level(stack, ResourceKey.create(Registries.ENCHANTMENT, id));
	}
}
