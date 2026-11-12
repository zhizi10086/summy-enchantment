package com.summy.enchantment.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.notunanancyowen.spears.dataholders.SpearUser;
import com.summy.enchantment.compat.SpearCompat;
import com.summy.enchantment.particle.MeleeParticles;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Backported Spears 1.8.0 的左键刺击入口。 */
@Mixin(targets = "com.notunanancyowen.spears.components.PiercingWeapon")
public abstract class SpearPiercingWeaponCompatMixin {
	@WrapOperation(
			method = "stab",
			at = @At(
					value = "INVOKE",
					target = "Lcom/notunanancyowen/spears/dataholders/SpearUser;pierce(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/Entity;FZZZ)Z"
			),
			require = 0,
			remap = false
	)
	private boolean summyEnchantment$stabPierce(SpearUser spearUser, EquipmentSlot slot, Entity target,
			float damage, boolean dealDamage, boolean knockback, boolean dismount, Operation<Boolean> original) {
		LivingEntity attacker = (LivingEntity) spearUser;
		ItemStack spear = attacker.getItemBySlot(slot);
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
