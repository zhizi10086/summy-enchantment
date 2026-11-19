package com.summy.enchantment.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.summy.enchantment.config.SummyEnchantmentConfig;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
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
			method = "getPossibleEntries(ILnet/minecraft/item/ItemStack;Ljava/util/stream/Stream;)Ljava/util/List;",
			at = @At("RETURN")
	)
	private static List<EnchantmentLevelEntry> summyEnchantment$hideVanillaSharpness(
			List<EnchantmentLevelEntry> original) {
		if (!SummyEnchantmentConfig.get().enableCustomSharpness) {
			return original;
		}
		List<EnchantmentLevelEntry> filtered = new ArrayList<>(original.stream()
				.filter(entry -> !entry.enchantment.matchesKey(Enchantments.SHARPNESS))
				.toList());
		return filtered.size() == original.size() ? original : filtered;
	}
}
