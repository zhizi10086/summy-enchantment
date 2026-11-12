package com.summy.enchantment.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.summy.enchantment.config.SummyEnchantmentConfig;
import com.summy.enchantment.registry.EnchantmentRef;
import com.summy.enchantment.registry.ModEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.1 附魔动态定义中的兼容性补丁。
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
	@Inject(method = "isAcceptableItem(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
	private void summyEnchantment$applyItemCompatibility(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		Enchantment self = (Enchantment) (Object) this;
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();

		boolean customCrossbowOnly = isAnyValue(self, ModEnchantments.ARROW_VELOCITY,
				ModEnchantments.PHANTOM_ARROW, ModEnchantments.ENDLESS_QUIVER);
		if (stack.isOf(Items.CROSSBOW) && customCrossbowOnly) {
			cir.setReturnValue(config.enableCrossbowCompat);
			return;
		}
		if (!config.enableCrossbowCompat) {
			return;
		}
		if (stack.isOf(Items.CROSSBOW) && isAnyKey(self, Enchantments.POWER, Enchantments.INFINITY,
				Enchantments.FLAME, Enchantments.PUNCH)) {
			cir.setReturnValue(true);
			return;
		}
		if (stack.isOf(Items.BOW) && isAnyKey(self, Enchantments.MULTISHOT, Enchantments.PIERCING)) {
			cir.setReturnValue(true);
		}
	}

	@ModifyReturnValue(method = "getMaxLevel()I", at = @At("RETURN"))
	private int summyEnchantment$configuredMaxLevel(int original) {
		Enchantment self = (Enchantment) (Object) this;
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		if (config.enableProtectionLevel5
				&& EnchantmentRef.valueMatches(self, Enchantments.PROTECTION)) {
			return 5;
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

	private static boolean isAnyKey(Enchantment value,
			net.minecraft.registry.RegistryKey<Enchantment>... keys) {
		for (var key : keys) {
			if (EnchantmentRef.valueMatches(value, key)) {
				return true;
			}
		}
		return false;
	}
}
