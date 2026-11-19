package com.summy.enchantment.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.summy.enchantment.config.SummyEnchantmentConfig;
import com.summy.enchantment.registry.EnchantmentRef;
import com.summy.enchantment.registry.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.1 附魔动态定义中的兼容性补丁。
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
	@ModifyReturnValue(method = "areCompatible", at = @At("RETURN"))
	private static boolean summyEnchantment$applyPairCompatibility(boolean original,
			Holder<Enchantment> first, Holder<Enchantment> second) {
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		// 只改变当前这一对，保留同一物品上其余附魔的独立判定。
		if (config.enableInfinityMending
				&& ((first.is(Enchantments.INFINITY) && second.is(Enchantments.MENDING))
						|| (first.is(Enchantments.MENDING) && second.is(Enchantments.INFINITY)))) {
			return true;
		}
		if (config.enableCustomSharpness
				&& ((first.is(Enchantments.SHARPNESS) && ModEnchantments.SHARPNESS_PLUS.matches(second))
						|| (second.is(Enchantments.SHARPNESS) && ModEnchantments.SHARPNESS_PLUS.matches(first)))) {
			return false;
		}
		return original;
	}

	@Inject(method = "canEnchant(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
	private void summyEnchantment$applyItemCompatibility(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		Enchantment self = (Enchantment) (Object) this;
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		boolean customCrossbowOnly = isAnyValue(self, ModEnchantments.ARROW_VELOCITY,
				ModEnchantments.PHANTOM_ARROW, ModEnchantments.ENDLESS_QUIVER);
		if (stack.is(Items.CROSSBOW) && customCrossbowOnly) {
			cir.setReturnValue(config.enableCrossbowCompat);
			return;
		}
		if (!config.enableCrossbowCompat) {
			return;
		}
		if (stack.is(Items.CROSSBOW) && isAnyKey(self, Enchantments.POWER, Enchantments.INFINITY,
				Enchantments.FLAME, Enchantments.PUNCH)) {
			cir.setReturnValue(true);
			return;
		}
		if (stack.is(Items.BOW) && isAnyKey(self, Enchantments.MULTISHOT, Enchantments.PIERCING)) {
			cir.setReturnValue(true);
		}
	}

	@ModifyReturnValue(method = "getMaxLevel()I", at = @At("RETURN"))
	private int summyEnchantment$configuredMaxLevel(int original) {
		Enchantment self = (Enchantment) (Object) this;
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		if (config.enableProtectionLevel5
				&& EnchantmentRef.valueMatches(self, Enchantments.PROTECTION)) {
			return config.protectionMaxLevel;
		}
		if (ModEnchantments.SHARPNESS_PLUS.matchesValue(self)) {
			return config.enableCustomSharpness ? config.sharpnessMaxLevel : original;
		}
		return original;
	}

	private static boolean isAnyValue(Enchantment value, EnchantmentRef... refs) {
		for (EnchantmentRef ref : refs) {
			if (ref.matchesValue(value)) {
				return true;
			}
		}
		return false;
	}

	@SafeVarargs
	private static boolean isAnyKey(Enchantment value, ResourceKey<Enchantment>... keys) {
		for (ResourceKey<Enchantment> key : keys) {
			if (EnchantmentRef.valueMatches(value, key)) {
				return true;
			}
		}
		return false;
	}
}
