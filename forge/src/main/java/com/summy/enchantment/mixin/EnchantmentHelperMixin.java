package com.summy.enchantment.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.summy.enchantment.config.SummyEnchantmentConfig;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.21.1 随机可选范围的配置补丁。
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
	@ModifyReturnValue(
			method = "getAvailableEnchantmentResults(ILnet/minecraft/world/item/ItemStack;Ljava/util/stream/Stream;)Ljava/util/List;",
			at = @At("RETURN")
	)
	private static List<EnchantmentInstance> summyEnchantment$hideVanillaSharpness(
			List<EnchantmentInstance> original) {
		if (!SummyEnchantmentConfig.get().enableCustomSharpness) {
			return original;
		}
		List<EnchantmentInstance> filtered = new ArrayList<>(original.stream()
				.filter(entry -> !entry.enchantment.is(Enchantments.SHARPNESS))
				.toList());
		return filtered.size() == original.size() ? original : filtered;
	}
}
