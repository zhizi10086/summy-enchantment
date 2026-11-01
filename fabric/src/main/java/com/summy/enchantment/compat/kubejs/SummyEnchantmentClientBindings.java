package com.summy.enchantment.compat.kubejs;

import com.summy.enchantment.api.SummyEnchantmentApi;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import java.util.List;

/** KubeJS 客户端安全查询表：不提供任何写入或服务端权威状态接口。 */
public final class SummyEnchantmentClientBindings {
	private SummyEnchantmentClientBindings() {
	}

	public static int apiVersion() {
		return SummyEnchantmentApi.apiVersion();
	}

	public static int level(ItemStack stack, String id) {
		return SummyEnchantmentApi.level(stack, id);
	}

	public static boolean has(ItemStack stack, String id) {
		return SummyEnchantmentApi.has(stack, id);
	}

	public static float lifeShield(LivingEntity entity) {
		return SummyEnchantmentApi.lifeShield(entity);
	}

	public static float lifeShieldCap(LivingEntity entity) {
		return SummyEnchantmentApi.lifeShieldCap(entity);
	}

	public static float lifestealHeal(ItemStack weapon, float dealtDamage) {
		return SummyEnchantmentApi.lifestealHeal(weapon, dealtDamage);
	}

	public static List<String> enchantmentIds() {
		return SummyEnchantmentApi.enchantmentIds();
	}

	public static int enchantmentMaxLevel(String id) {
		return SummyEnchantmentApi.enchantmentMaxLevel(id);
	}

	public static String enchantmentRarity(String id) {
		return SummyEnchantmentApi.enchantmentRarity(id);
	}

	public static boolean isTreasure(String id) {
		return SummyEnchantmentApi.isTreasure(id);
	}

	public static boolean isCursed(String id) {
		return SummyEnchantmentApi.isCursed(id);
	}

	public static boolean enchantmentCanApplyTo(ItemStack stack, String id) {
		return SummyEnchantmentApi.enchantmentCanApplyTo(stack, id);
	}

	public static boolean enchantmentCanEnchant(ItemStack stack, String id) {
		return SummyEnchantmentApi.enchantmentCanEnchant(stack, id);
	}

	public static List<String> configKeys() {
		return SummyEnchantmentApi.configKeys();
	}

	public static double configValue(String key) {
		return SummyEnchantmentApi.configValue(key);
	}

	public static boolean configBoolean(String key) {
		return SummyEnchantmentApi.configBoolean(key);
	}

	public static double configDefault(String key) {
		return SummyEnchantmentApi.configDefault(key);
	}

	public static double configMin(String key) {
		return SummyEnchantmentApi.configMin(key);
	}

	public static double configMax(String key) {
		return SummyEnchantmentApi.configMax(key);
	}

	public static double configStep(String key) {
		return SummyEnchantmentApi.configStep(key);
	}

	public static boolean hasConfig(String key) {
		return SummyEnchantmentApi.hasConfig(key);
	}

	public static String configScope(String key) {
		return SummyEnchantmentApi.configScope(key);
	}

	public static String configKind(String key) {
		return SummyEnchantmentApi.configKind(key);
	}

	public static String configCategory(String key) {
		return SummyEnchantmentApi.configCategory(key);
	}
}
