package com.summy.enchantment.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.notunanancyowen.spears.dataholders.SpearUser;
import com.summy.enchantment.compat.SpearCompat;
import com.summy.enchantment.particle.MeleeParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Backported Spears 1.8.0 的左键刺击入口。
 *
 * <p>1.8.0 将旧 {@code SpearItem} 的刺击逻辑迁到 {@code PiercingWeapon} 数据组件，
 * 但最终仍调用 {@link SpearUser#pierce}。这里包装同一条调用，保持力量Plus、
 * 魔剑、嗜血、事件和粒子语义不变。
 */
@Mixin(targets = "com.notunanancyowen.spears.components.PiercingWeapon")
public abstract class SpearPiercingWeaponCompatMixin {
	@WrapOperation(
			method = "stab",
			at = @At(
					value = "INVOKE",
					target = "Lcom/notunanancyowen/spears/dataholders/SpearUser;pierce(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/entity/Entity;FZZZ)Z"
			),
			require = 0
	)
	private boolean summyEnchantment$stabPierce(SpearUser spearUser, EquipmentSlot slot, Entity target,
			float damage, boolean dealDamage, boolean knockback, boolean dismount, Operation<Boolean> original) {
		LivingEntity attacker = (LivingEntity) spearUser;
		ItemStack spear = attacker.getEquippedStack(slot);
		SpearCompat.PierceAdjustment adjustment = SpearCompat.adjustPierceDamage(
				attacker, spear, target, damage, true);
		boolean hit = original.call(
				spearUser, slot, target, adjustment.parameter(), dealDamage, knockback, dismount);
		if (dealDamage) {
			SpearCompat.settleHit(attacker, spear, target, damage, adjustment, hit, "spearStab");
		}
		if (hit && dealDamage) {
			MeleeParticles.spawnOnHit(attacker, target);
		}
		return hit;
	}
}
