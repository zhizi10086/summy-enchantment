package com.summy.enchantment.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.notunanancyowen.spears.dataholders.SpearUser;
import com.summy.enchantment.compat.SpearCompat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Backported Spears 1.8.0 的右键蓄力刺击入口。
 *
 * <p>蓄力伤害已在组件传入前包含基础、附魔和速度口径；SE 只在此补充命中后结算，
 * 不额外应用左键力量Plus。
 */
@Mixin(targets = "com.notunanancyowen.spears.components.KineticWeapon")
public abstract class SpearKineticWeaponCompatMixin {
	@WrapOperation(
			method = "usageTick",
			at = @At(
					value = "INVOKE",
					target = "Lcom/notunanancyowen/spears/dataholders/SpearUser;pierce(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/entity/Entity;FZZZ)Z"
			),
			require = 0
	)
	private boolean summyEnchantment$chargePierce(SpearUser spearUser, EquipmentSlot slot, Entity target,
			float damage, boolean dealDamage, boolean knockback, boolean dismount, Operation<Boolean> original) {
		LivingEntity user = (LivingEntity) spearUser;
		ItemStack spear = user.getEquippedStack(slot);
		SpearCompat.PierceAdjustment adjustment = SpearCompat.adjustPierceDamage(
				user, spear, target, damage, false);
		boolean hit = original.call(
				spearUser, slot, target, adjustment.parameter(), dealDamage, knockback, dismount);
		if (dealDamage) {
			SpearCompat.settleHit(user, spear, target, damage, adjustment, hit, "spearCharge");
		}
		return hit;
	}
}
