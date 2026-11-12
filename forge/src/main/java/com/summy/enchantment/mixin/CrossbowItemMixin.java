package com.summy.enchantment.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.summy.enchantment.combat.ArrowTraits;
import com.summy.enchantment.combat.EnchantmentEffects;
import com.summy.enchantment.compat.BowShotContext;
import com.summy.enchantment.config.SummyEnchantmentConfig;
import com.summy.enchantment.network.ConfigSync;
import com.summy.enchantment.registry.ModEnchantments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 弩类附魔在 1.21.1 新投射物管线上的适配。
 */
@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
	@Inject(method = "createProjectile", at = @At("RETURN"))
	private void summyEnchantment$applyBowEnchantments(Level world, LivingEntity shooter,
			ItemStack crossbow, ItemStack ammo, boolean multishot,
			CallbackInfoReturnable<Projectile> cir) {
		if (cir.getReturnValue() instanceof AbstractArrow arrow) {
			EnchantmentEffects.applyCrossbowEnchantments(crossbow, arrow);
			summyEnchantment$applyArrowTraits(crossbow, arrow, shooter);
		}
	}

	private void summyEnchantment$applyArrowTraits(ItemStack crossbow, AbstractArrow arrow,
			LivingEntity shooter) {
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		if (config.enableLightness && ModEnchantments.LIGHTNESS != null) {
			int level = ModEnchantments.LIGHTNESS.level(crossbow);
			if (level > 0) {
				ArrowTraits.setServerLightness(arrow, level);
				ConfigSync.sendArrowLightness(arrow, level);
			}
		}
		if (config.enableDeathEye && ModEnchantments.DEATH_EYE != null && shooter != null
				&& ModEnchantments.DEATH_EYE.level(crossbow) > 0) {
			ArrowTraits.markDeathEye(arrow, shooter);
		}
	}

	/** 无弹药时，无限或无尽箭袋在弩上仍提供普通箭。 */
	@WrapOperation(
			method = "tryLoadProjectiles",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;getProjectile(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"
			)
	)
	private static ItemStack summyEnchantment$emptyAmmoFallback(LivingEntity shooter, ItemStack crossbow,
			Operation<ItemStack> original) {
		ItemStack ammo = original.call(shooter, crossbow);
		if (!ammo.isEmpty() || !EnchantmentEffects.allowsEmptyAmmoCrossbow(crossbow)) {
			return ammo;
		}
		return new ItemStack(Items.ARROW);
	}

	/** 疾矢：弩箭初速度乘以附魔倍率。 */
	@ModifyArg(
			method = "shootProjectile",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/projectile/Projectile;shoot(DDDFF)V"
			),
			index = 3
	)
	private float summyEnchantment$crossbowArrowSpeed(float speed) {
		ItemStack crossbow = BowShotContext.currentBow();
		return crossbow == null ? speed : speed * EnchantmentEffects.arrowSpeedMultiplier(crossbow);
	}
}
