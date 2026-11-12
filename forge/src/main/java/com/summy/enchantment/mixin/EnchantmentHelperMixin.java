package com.summy.enchantment.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.summy.enchantment.config.SummyEnchantmentConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;

/**
 * 1.21.1 附魔兼容和随机可选范围的配置补丁。
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
		List<EnchantmentInstance> filtered = original.stream()
				.filter(entry -> !entry.enchantment.is(Enchantments.SHARPNESS))
				.toList();
		return filtered.size() == original.size() ? original : filtered;
	}

	@Inject(
			method = "isEnchantmentCompatible(Ljava/util/Collection;Lnet/minecraft/core/Holder;)Z",
			at = @At("HEAD"),
			cancellable = true
	)
	private static void summyEnchantment$allowInfinityMending(
			Collection<Holder<Enchantment>> existing,
			Holder<Enchantment> candidate,
			CallbackInfoReturnable<Boolean> cir) {
		if (!SummyEnchantmentConfig.get().enableInfinityMending) {
			return;
		}
		boolean candidateInfinity = candidate.is(Enchantments.INFINITY);
		boolean candidateMending = candidate.is(Enchantments.MENDING);
		boolean hasInfinity = existing.stream().anyMatch(entry -> entry.is(Enchantments.INFINITY));
		boolean hasMending = existing.stream().anyMatch(entry -> entry.is(Enchantments.MENDING));
		if ((candidateInfinity && hasMending) || (candidateMending && hasInfinity)) {
			cir.setReturnValue(true);
		}
	}
}
