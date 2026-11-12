package com.summy.enchantment.combat;

import com.summy.enchantment.config.SummyEnchantmentConfig;
import com.summy.enchantment.network.ConfigSync;
import com.summy.enchantment.registry.EnchantmentRef;
import com.summy.enchantment.registry.ModEnchantments;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;

/**
 * 1.21.1 弓类武器在箭矢生成后的统一效果入口。
 */
public final class BowShotEffects {
	private BowShotEffects() {
	}

	public static void applyAfterArrowSpawn(ItemStack bow, PersistentProjectileEntity arrow, int drawTicks) {
		applyArrowTraits(bow, arrow, drawTicks);
		applyBowCrossbowEnchantments(bow, arrow);
		if (EnchantmentEffects.rollPhantomArrow(bow)) {
			EnchantmentEffects.markExtraHitArrow(arrow);
			EnchantmentEffects.spawnExtraArrow(arrow);
		}
	}

	private static void applyArrowTraits(ItemStack bow, PersistentProjectileEntity arrow, int drawTicks) {
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		if (config.enableLightness && ModEnchantments.LIGHTNESS != null) {
			int level = ModEnchantments.LIGHTNESS.level(bow);
			if (level > 0) {
				ArrowTraits.setServerLightness(arrow, level);
				ConfigSync.sendArrowLightness(arrow, level);
			}
		}
		if (config.enableSkyWish && ModEnchantments.SKY_WISH != null
				&& ModEnchantments.SKY_WISH.level(bow) > 0) {
			float bonus = (float) Math.min(config.skyWishBonusPerTick * drawTicks, config.skyWishMaxBonus);
			if (bonus > 0.0F) {
				arrow.setDamage(arrow.getDamage() * (1.0D + bonus));
			}
		}
		if (config.enableDeathEye && ModEnchantments.DEATH_EYE != null
				&& ModEnchantments.DEATH_EYE.level(bow) > 0
				&& arrow.getOwner() instanceof LivingEntity shooter) {
			ArrowTraits.markDeathEye(arrow, shooter);
		}
	}

	private static void applyBowCrossbowEnchantments(ItemStack bow, PersistentProjectileEntity arrow) {
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		if (!config.enableCrossbowCompat) {
			return;
		}
		int multishot = EnchantmentRef.level(bow, Enchantments.MULTISHOT);
		if (multishot <= 0) {
			return;
		}
		EnchantmentEffects.markMultishotArrow(bow, arrow);
		if (!(arrow.getOwner() instanceof LivingEntity shooter)) {
			return;
		}
		double spread = config.multishotSpreadDegrees;
		float inaccuracy = config.bowInaccuracy;
		EnchantmentEffects.spawnMultishotArrow(arrow, bow, shooter, -spread, inaccuracy);
		EnchantmentEffects.spawnMultishotArrow(arrow, bow, shooter, spread, inaccuracy);
	}
}
