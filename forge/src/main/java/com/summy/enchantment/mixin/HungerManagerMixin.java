package com.summy.enchantment.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.summy.enchantment.combat.EnchantmentEffects;
import com.summy.enchantment.combat.HungerOwner;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 大胃袋：把饥饿值上限从原版写死的 20 换成"20 + 附魔加成"。
 *
 * <p>只改两处真正的上限判定：
 * <ul>
 *     <li>{@code add(int, float)} 里饿值封顶的那个 20（吃超过上限的部分不再被吞掉）；</li>
 *     <li>{@code needsFood()} 里判断"还能不能吃"的 20（吃满了就真的不能再吃）。</li>
 * </ul>
 * 自然回血仍然沿用原版的 {@code foodLevel >= 20}，不在此次范围内。
 */
@Mixin(FoodData.class)
public abstract class HungerManagerMixin implements HungerOwner {
	@Shadow
	private int foodLevel;

	/** 所属玩家（由 Player 构造完成时登记） */
	@Unique
	private Player summyEnchantment$owner;

	@Override
	public void summyEnchantment$setOwner(Player player) {
		this.summyEnchantment$owner = player;
	}

	/** 饥饿值上限：原版 20 + 大胃袋加成 */
	@Unique
	private int summyEnchantment$foodLevelCap() {
		return this.summyEnchantment$owner == null
				? 20
				: EnchantmentEffects.foodLevelCap(this.summyEnchantment$owner);
	}

	/**
	 * 包装饥饿值封顶结果，而不是替换 {@code 20} 常量。
	 *
	 * <p>若其它模组已经提高了上限，保留其原结果；SE 大胃袋只保证结果至少达到
	 * {@code min(当前饥饿值 + 食物营养, SE上限)}。这样双方实际容量自然取较高值。
	 */
	@ModifyExpressionValue(
			method = "add(IF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I")
	)
	private int summyEnchantment$extendFoodCap(
			int original, @Local(argsOnly = true, index = 1) int nutrition) {
		int desired = Math.min(this.foodLevel + nutrition, summyEnchantment$foodLevelCap());
		return Math.max(original, desired);
	}

	/** 只在原判定基础上扩展，不覆盖其它模组已经放宽的"还能吃"结果。 */
	@ModifyReturnValue(method = "needsFood()Z", at = @At("RETURN"))
	private boolean summyEnchantment$extendFullThreshold(boolean original) {
		return original || this.foodLevel < summyEnchantment$foodLevelCap();
	}
}
