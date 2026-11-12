package com.summy.enchantment.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.notunanancyowen.spears.dataholders.SpearUser;
import com.summy.enchantment.compat.SpearCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Backported Spears 1.8.0 的右键蓄力刺击入口。 */
@Mixin(targets = "com.notunanancyowen.spears.components.KineticWeapon")
public abstract class SpearKineticWeaponCompatMixin {
	@WrapOperation(
			method = "usageTick",
			at = @At(
					value = "INVOKE",
					target = "Lcom/notunanancyowen/spears/dataholders/SpearUser;pierce(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/Entity;FZZZ)Z"
			),
			require = 0,
			remap = false
	)
	private boolean summyEnchantment$chargePierce(SpearUser spearUser, EquipmentSlot slot, Entity target,
			float damage, boolean dealDamage, boolean knockback, boolean dismount, Operation<Boolean> original) {
		LivingEntity user = (LivingEntity) spearUser;
		ItemStack spear = user.getItemBySlot(slot);
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
