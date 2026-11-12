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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.1 将弓箭弹药结算和实体生成集中到 ProjectileWeaponItem 后的兼容入口。
 */
@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemMixin {
	@Inject(method = "shoot", at = @At("HEAD"))
	private void summyEnchantment$captureCrossbowContext(ServerLevel level, LivingEntity shooter,
			InteractionHand hand, ItemStack weapon, java.util.List<ItemStack> projectiles,
			float power, float uncertainty, boolean creative, LivingEntity target, CallbackInfo ci) {
		if (weapon.getItem() instanceof CrossbowItem && BowShotContext.currentBow() == null) {
			BowShotContext.begin(weapon, 0);
		}
	}

	@Inject(method = "shoot", at = @At("RETURN"))
	private void summyEnchantment$clearCrossbowContext(ServerLevel level, LivingEntity shooter,
			InteractionHand hand, ItemStack weapon, java.util.List<ItemStack> projectiles,
			float power, float uncertainty, boolean creative, LivingEntity target, CallbackInfo ci) {
		if (weapon.getItem() instanceof CrossbowItem) {
			BowShotContext.end();
		}
	}

	@ModifyExpressionValue(
			method = "useAmmo",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processAmmoUse(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)I"
			)
	)
	private static int summyEnchantment$spareBowAmmo(int original,
			@Local(argsOnly = true, index = 0) ItemStack weapon,
			@Local(argsOnly = true, index = 1) ItemStack ammo) {
		boolean supported = weapon.getItem() instanceof BowItem
				|| weapon.getItem() instanceof CrossbowItem
				&& SummyEnchantmentConfig.get().enableCrossbowCompat;
		return supported && EnchantmentEffects.sparesAmmo(weapon, ammo) ? 0 : original;
	}

	@ModifyExpressionValue(
			method = "shoot",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processProjectileSpread(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;F)F"
			)
	)
	private float summyEnchantment$crossbowSpread(float original,
			@Local(argsOnly = true) ItemStack weapon) {
		if (weapon.getItem() instanceof CrossbowItem
				&& EnchantmentRef.level(weapon, Enchantments.MULTISHOT) > 0) {
			return SummyEnchantmentConfig.get().multishotSpreadDegrees;
		}
		return original;
	}

	@WrapOperation(
			method = "shoot",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
			)
	)
	private boolean summyEnchantment$afterArrowSpawned(ServerLevel level, Entity entity,
			Operation<Boolean> original, @Local(argsOnly = true) ItemStack weapon) {
		boolean spawned = original.call(level, entity);
		if (spawned && weapon.getItem() instanceof BowItem && entity instanceof AbstractArrow arrow) {
			BowShotEffects.applyAfterArrowSpawn(weapon, arrow, BowShotContext.currentDrawTicks());
		} else if (spawned && weapon.getItem() instanceof CrossbowItem && entity instanceof AbstractArrow arrow) {
			EnchantmentEffects.markMultishotArrow(weapon, arrow);
			if (EnchantmentEffects.rollPhantomArrow(weapon)) {
				EnchantmentEffects.markExtraHitArrow(arrow);
				EnchantmentEffects.spawnExtraArrow(arrow);
			}
		}
		return spawned;
	}
}
