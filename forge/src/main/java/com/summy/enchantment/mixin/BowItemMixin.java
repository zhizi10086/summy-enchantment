package com.summy.enchantment.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.summy.enchantment.combat.EnchantmentEffects;
import com.summy.enchantment.compat.BowShotContext;
import com.summy.enchantment.config.SummyEnchantmentConfig;
import com.summy.enchantment.registry.ModEnchantments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 弓类附魔在 1.21.1 松手与发射管线上的适配。
 */
@Mixin(BowItem.class)
public abstract class BowItemMixin {
	@Inject(method = "releaseUsing", at = @At("HEAD"))
	private void summyEnchantment$captureBow(ItemStack stack, Level world, LivingEntity user,
			int remainingUseTicks, CallbackInfo ci) {
		int drawTicks = Math.max(0, stack.getUseDuration(user) - remainingUseTicks);
		BowShotContext.begin(stack, drawTicks);
	}

	@Inject(method = "releaseUsing", at = @At("RETURN"))
	private void summyEnchantment$clearBow(ItemStack stack, Level world, LivingEntity user,
			int remainingUseTicks, CallbackInfo ci) {
		BowShotContext.end();
	}

	/** 速射：缩放传给原版拉弓进度计算的实际 tick 数。 */
	@ModifyArg(
			method = "releaseUsing",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"),
			index = 0
	)
	private int summyEnchantment$speedUpDraw(int useTicks) {
		if (!SummyEnchantmentConfig.get().enableQuickDraw) {
			return useTicks;
		}
		ItemStack bow = BowShotContext.currentBow();
		if (bow == null) {
			return useTicks;
		}
		int required = EnchantmentEffects.requiredDrawTicks(bow);
		if (required >= 20) {
			return useTicks;
		}
		return Math.round(useTicks * (20.0F / required));
	}

	/** 无弹药时，无限或无尽箭袋仍提供普通箭作为本次射击弹药。 */
	@WrapOperation(
			method = "releaseUsing",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;getProjectile(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"
			)
	)
	private ItemStack summyEnchantment$emptyAmmoFallback(Player player, ItemStack bow,
			Operation<ItemStack> original) {
		ItemStack ammo = original.call(player, bow);
		if (!ammo.isEmpty() || !EnchantmentEffects.allowsEmptyAmmo(player, bow)) {
			return ammo;
		}
		return new ItemStack(Items.ARROW);
	}

	/** 疾矢：箭矢初速度乘以附魔倍率。 */
	@ModifyArg(
			method = "shootProjectile",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/projectile/Projectile;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V"
			),
			index = 4
	)
	private float summyEnchantment$arrowVelocity(float speed) {
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		if (!config.enableArrowVelocity || ModEnchantments.ARROW_VELOCITY == null) {
			return speed;
		}
		ItemStack bow = BowShotContext.currentBow();
		if (bow == null) {
			return speed;
		}
		int level = ModEnchantments.ARROW_VELOCITY.level(bow);
		return level <= 0 ? speed : speed * (1.0F + config.arrowVelocityBonusPerLevel * level);
	}

	/** 弓的自然散布改为配置值。 */
	@ModifyArg(
			method = "shootProjectile",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/projectile/Projectile;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V"
			),
			index = 5
	)
	private float summyEnchantment$bowInaccuracy(float divergence) {
		return SummyEnchantmentConfig.get().bowInaccuracy;
	}
}
