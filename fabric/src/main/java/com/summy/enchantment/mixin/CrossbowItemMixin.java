package com.summy.enchantment.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.summy.enchantment.combat.ArrowTraits;
import com.summy.enchantment.combat.EnchantmentEffects;
import com.summy.enchantment.config.SummyEnchantmentConfig;
import com.summy.enchantment.network.ConfigSync;
import com.summy.enchantment.registry.ModEnchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 弩类附魔在 1.21.1 新投射物管线上的适配。
 */
@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
	@Unique
	private static final ThreadLocal<ItemStack> summyEnchantment$currentCrossbow = new ThreadLocal<>();

	@Inject(method = "shootAll", at = @At("HEAD"))
	private void summyEnchantment$captureCrossbow(World world, LivingEntity shooter,
			net.minecraft.util.Hand hand, ItemStack crossbow, float speed, float divergence,
			LivingEntity target, CallbackInfo ci) {
		summyEnchantment$currentCrossbow.set(crossbow);
	}

	@Inject(method = "shootAll", at = @At("RETURN"))
	private void summyEnchantment$clearCrossbow(World world, LivingEntity shooter,
			net.minecraft.util.Hand hand, ItemStack crossbow, float speed, float divergence,
			LivingEntity target, CallbackInfo ci) {
		summyEnchantment$currentCrossbow.remove();
	}

	@Inject(method = "createArrowEntity", at = @At("RETURN"))
	private void summyEnchantment$applyBowEnchantments(World world, LivingEntity shooter,
			ItemStack crossbow, ItemStack ammo, boolean multishot,
			CallbackInfoReturnable<ProjectileEntity> cir) {
		if (cir.getReturnValue() instanceof PersistentProjectileEntity arrow) {
			EnchantmentEffects.applyCrossbowEnchantments(crossbow, arrow);
			summyEnchantment$applyArrowTraits(crossbow, arrow, shooter);
		}
	}

	private void summyEnchantment$applyArrowTraits(ItemStack crossbow, PersistentProjectileEntity arrow,
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
			method = "loadProjectiles",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/entity/LivingEntity;getProjectileType(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"
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
			method = "shoot",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/entity/projectile/ProjectileEntity;setVelocity(DDDFF)V"
			),
			index = 3
	)
	private float summyEnchantment$crossbowArrowSpeed(float speed) {
		ItemStack crossbow = summyEnchantment$currentCrossbow.get();
		return crossbow == null ? speed : speed * EnchantmentEffects.arrowSpeedMultiplier(crossbow);
	}
}
