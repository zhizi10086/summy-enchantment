package com.summy.enchantment.compat.kubejs.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;

/** KubeJS 只读事件：箭矢或投掷三叉戟的一次伤害尝试已完成。 */
public final class ProjectileSettledEventJS implements KubeEvent {
	private final String kind;
	private final LivingEntity attacker;
	private final Entity target;
	private final Entity projectile;
	private final ItemStack weapon;
	private final DamageSource damageSource;
	private final float rawAmount;
	private final float amount;
	private final boolean landed;
	private final boolean extraProjectile;
	private final float deathEyeMultiplier;
	private final float spellbladeDamage;
	private final float lifestealHeal;
	private final boolean surprise;

	public ProjectileSettledEventJS(String kind, LivingEntity attacker, Entity target, Entity projectile,
			ItemStack weapon, DamageSource damageSource, float rawAmount, float amount, boolean landed,
			boolean extraProjectile, float deathEyeMultiplier,
			float spellbladeDamage, float lifestealHeal, boolean surprise) {
		this.kind = kind;
		this.attacker = attacker;
		this.target = target;
		this.projectile = projectile;
		this.weapon = weapon == null ? ItemStack.EMPTY : weapon.copy();
		this.damageSource = damageSource;
		this.rawAmount = rawAmount;
		this.amount = amount;
		this.landed = landed;
		this.extraProjectile = extraProjectile;
		this.deathEyeMultiplier = deathEyeMultiplier;
		this.spellbladeDamage = spellbladeDamage;
		this.lifestealHeal = lifestealHeal;
		this.surprise = surprise;
	}

	public String getKind() {
		return kind;
	}

	public LivingEntity getAttacker() {
		return attacker;
	}

	public Entity getTarget() {
		return target;
	}

	public Entity getProjectile() {
		return projectile;
	}

	public ItemStack getWeapon() {
		return weapon;
	}

	public DamageSource getDamageSource() {
		return damageSource;
	}

	public float getRawAmount() {
		return rawAmount;
	}

	/** 伤害调用前的攻击侧金额，不包含目标护甲、保护、吸收或第三方减伤。 */
	public float getAmount() {
		return amount;
	}

	public float getAttackAmount() {
		return amount;
	}

	public boolean isLanded() {
		return landed;
	}

	public boolean isExtraProjectile() {
		return extraProjectile;
	}

	public float getDeathEyeMultiplier() {
		return deathEyeMultiplier;
	}

	public float getSpellbladeDamage() {
		return spellbladeDamage;
	}

	public float getLifestealHeal() {
		return lifestealHeal;
	}

	public boolean isSurprise() {
		return surprise;
	}
}
