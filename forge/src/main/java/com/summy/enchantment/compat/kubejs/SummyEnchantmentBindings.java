package com.summy.enchantment.compat.kubejs;

import com.summy.enchantment.api.SummyEnchantmentApi;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** KubeJS 全局 {@code SummyEnchantment} 对象的方法表。 */
public final class SummyEnchantmentBindings {
	private SummyEnchantmentBindings() {
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

	public static float addLifeShield(LivingEntity entity, float amount) {
		return SummyEnchantmentApi.addLifeShield(entity, amount);
	}

	public static void clearLifeShield(LivingEntity entity) {
		SummyEnchantmentApi.clearLifeShield(entity);
	}

	public static float lifestealHeal(ItemStack weapon, float dealtDamage) {
		return SummyEnchantmentApi.lifestealHeal(weapon, dealtDamage);
	}

	public static float applyLifesteal(LivingEntity attacker, ItemStack weapon, float dealtDamage) {
		return SummyEnchantmentApi.applyLifesteal(attacker, weapon, dealtDamage);
	}

	public static float meleeMultiplier(Player player) {
		return SummyEnchantmentApi.meleeMultiplier(player);
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

	public static int deathEyeStacks(Player player) {
		return SummyEnchantmentApi.deathEyeStacks(player);
	}

	public static float deathEyeMultiplier(Player player) {
		return SummyEnchantmentApi.deathEyeMultiplier(player);
	}

	public static int arrowLightnessLevel(AbstractArrow arrow) {
		return SummyEnchantmentApi.arrowLightnessLevel(arrow);
	}

	public static float arrowDeathEyeMultiplier(AbstractArrow arrow) {
		return SummyEnchantmentApi.arrowDeathEyeMultiplier(arrow);
	}

	public static int hiddenBladeChargedTicks(LivingEntity entity) {
		return SummyEnchantmentApi.hiddenBladeChargedTicks(entity);
	}

	public static float hiddenBladeBonus(LivingEntity entity) {
		return SummyEnchantmentApi.hiddenBladeBonus(entity);
	}

	public static float hiddenBladeChargeProgress(LivingEntity entity) {
		return SummyEnchantmentApi.hiddenBladeChargeProgress(entity);
	}

	public static int hiddenBladeCooldownRemaining(LivingEntity entity) {
		return SummyEnchantmentApi.hiddenBladeCooldownRemaining(entity);
	}

	public static float deathsBlessingOutgoing(LivingEntity entity) {
		return SummyEnchantmentApi.deathsBlessingOutgoing(entity);
	}

	public static float deathsBlessingIncoming(LivingEntity entity) {
		return SummyEnchantmentApi.deathsBlessingIncoming(entity);
	}

	public static float revengeMultiplier(LivingEntity entity) {
		return SummyEnchantmentApi.revengeMultiplier(entity);
	}

	public static float arrowSpeedMultiplier(ItemStack weapon) {
		return SummyEnchantmentApi.arrowSpeedMultiplier(weapon);
	}

	public static float undyingGraceReduction(LivingEntity entity) {
		return SummyEnchantmentApi.undyingGraceReduction(entity);
	}

	public static int foodLevelCap(LivingEntity entity) {
		return SummyEnchantmentApi.foodLevelCap(entity);
	}

	public static int sturdyCap(ItemStack stack) {
		return SummyEnchantmentApi.sturdyCap(stack);
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
