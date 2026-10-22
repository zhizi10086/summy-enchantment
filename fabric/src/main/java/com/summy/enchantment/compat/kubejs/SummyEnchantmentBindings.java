package com.summy.enchantment.compat.kubejs;

import com.summy.enchantment.api.SummyEnchantmentApi;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/** KubeJS 全局 {@code SummyEnchantment} 对象的方法表。 */
public final class SummyEnchantmentBindings {
	private SummyEnchantmentBindings() {
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

	public static float meleeMultiplier(PlayerEntity player) {
		return SummyEnchantmentApi.meleeMultiplier(player);
	}
}
