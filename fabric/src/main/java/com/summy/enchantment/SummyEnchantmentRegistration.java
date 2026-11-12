package com.summy.enchantment;

import com.summy.enchantment.registry.ModAttributes;
import com.summy.enchantment.registry.ModEnchantments;
import com.summy.enchantment.registry.ModItemGroups;
import com.summy.enchantment.registry.ModStatusEffects;

/**
 * 在 1.21.1 注册表冻结前执行一次静态注册。
 */
public final class SummyEnchantmentRegistration {
	private static boolean registered;

	private SummyEnchantmentRegistration() {
	}

	public static synchronized void register() {
		if (registered) {
			return;
		}
		registered = true;
		ModStatusEffects.register();
		ModAttributes.registerAttribute();
		ModEnchantments.register();
		ModItemGroups.register();
	}
}
