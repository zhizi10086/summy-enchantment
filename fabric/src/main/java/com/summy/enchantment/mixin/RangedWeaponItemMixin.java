package com.summy.enchantment.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.summy.enchantment.combat.BowShotEffects;
import com.summy.enchantment.combat.EnchantmentEffects;
import com.summy.enchantment.compat.BowShotContext;
import com.summy.enchantment.config.SummyEnchantmentConfig;
import com.summy.enchantment.registry.EnchantmentRef;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.21.1 将弓箭弹药结算和生成集中到 RangedWeaponItem 后的兼容入口。
 */
@Mixin(RangedWeaponItem.class)
public abstract class RangedWeaponItemMixin {
	@ModifyExpressionValue(
			method = "getProjectile",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/enchantment/EnchantmentHelper;getAmmoUse(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;I)I"
			)
	)
	private static int summyEnchantment$spareBowAmmo(int original,
			@Local(argsOnly = true, index = 0) ItemStack weapon,
			@Local(argsOnly = true, index = 1) ItemStack ammo) {
		return weapon.getItem() instanceof BowItem && EnchantmentEffects.sparesAmmo(weapon, ammo)
				|| weapon.getItem() instanceof CrossbowItem
				&& SummyEnchantmentConfig.get().enableCrossbowCompat
				&& EnchantmentEffects.sparesAmmo(weapon, ammo)
						? 0
						: original;
	}

	@ModifyExpressionValue(
			method = "shootAll",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/enchantment/EnchantmentHelper;getProjectileSpread(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/Entity;F)F"
			)
	)
	private static float summyEnchantment$crossbowSpread(float original,
			@Local(argsOnly = true) ItemStack weapon) {
		if (weapon.getItem() instanceof CrossbowItem
				&& EnchantmentRef.level(weapon, Enchantments.MULTISHOT) > 0) {
			return SummyEnchantmentConfig.get().multishotSpreadDegrees;
		}
		return original;
	}

	@WrapOperation(
			method = "shootAll",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/world/ServerWorld;spawnEntity(Lnet/minecraft/entity/Entity;)Z"
			)
	)
	private boolean summyEnchantment$afterArrowSpawned(ServerWorld world, Entity entity,
			Operation<Boolean> original,
			@Local(argsOnly = true) ItemStack weapon) {
		boolean spawned = original.call(world, entity);
		if (spawned && weapon.getItem() instanceof BowItem
				&& entity instanceof PersistentProjectileEntity arrow) {
			BowShotEffects.applyAfterArrowSpawn(weapon, arrow, BowShotContext.currentDrawTicks());
		} else if (spawned && weapon.getItem() instanceof CrossbowItem
				&& entity instanceof PersistentProjectileEntity arrow) {
			EnchantmentEffects.markMultishotArrow(weapon, arrow);
			if (EnchantmentEffects.rollPhantomArrow(weapon)) {
				EnchantmentEffects.markExtraHitArrow(arrow);
				EnchantmentEffects.spawnExtraArrow(arrow);
			}
		}
		return spawned;
	}
}
