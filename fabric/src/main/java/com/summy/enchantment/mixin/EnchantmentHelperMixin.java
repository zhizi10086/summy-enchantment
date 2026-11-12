package com.summy.enchantment.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.summy.enchantment.config.SummyEnchantmentConfig;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * 1.21.1 附魔兼容和随机可选范围的配置补丁。
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
		List<EnchantmentLevelEntry> filtered = original.stream()
				.filter(entry -> !entry.enchantment.matchesKey(Enchantments.SHARPNESS))
				.toList();
		return filtered.size() == original.size() ? original : filtered;
	}

	@Inject(
			method = "isCompatible(Ljava/util/Collection;Lnet/minecraft/registry/entry/RegistryEntry;)Z",
			at = @At("HEAD"),
			cancellable = true
	)
	private static void summyEnchantment$allowInfinityMending(
			Collection<RegistryEntry<Enchantment>> existing,
			RegistryEntry<Enchantment> candidate,
			CallbackInfoReturnable<Boolean> cir) {
		if (!SummyEnchantmentConfig.get().enableInfinityMending) {
			return;
		}
		boolean candidateInfinity = candidate.matchesKey(Enchantments.INFINITY);
		boolean candidateMending = candidate.matchesKey(Enchantments.MENDING);
		boolean hasInfinity = existing.stream().anyMatch(entry -> entry.matchesKey(Enchantments.INFINITY));
		boolean hasMending = existing.stream().anyMatch(entry -> entry.matchesKey(Enchantments.MENDING));
		if ((candidateInfinity && hasMending) || (candidateMending && hasInfinity)) {
			cir.setReturnValue(true);
		}
	}
}
