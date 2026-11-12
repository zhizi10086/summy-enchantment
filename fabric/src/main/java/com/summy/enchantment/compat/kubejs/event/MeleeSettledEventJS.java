package com.summy.enchantment.compat.kubejs.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/** KubeJS 只读事件：一次近战主命中完成结算。 */
public final class MeleeSettledEventJS implements KubeEvent {
	private final String entry;
	private final PlayerEntity attacker;
	private final Entity target;
	private final ItemStack weapon;
	private final DamageSource damageSource;
	private final float rawAmount;
	private final float amount;
	private final boolean landed;
	private final boolean mainHit;
	private final float primarySpellbladeDamage;
	private final float primaryLifestealHeal;
	private final float surpriseSpellbladeDamage;
	private final float surpriseLifestealHeal;
	private final boolean executed;
	private final boolean surpriseTriggered;
	private final boolean surpriseLanded;

	public MeleeSettledEventJS(String entry, PlayerEntity attacker, Entity target, ItemStack weapon,
			DamageSource damageSource, float rawAmount, float amount, boolean landed, boolean mainHit,
			float primarySpellbladeDamage, float primaryLifestealHeal,
			float surpriseSpellbladeDamage, float surpriseLifestealHeal, boolean executed,
			boolean surpriseTriggered, boolean surpriseLanded) {
		this.entry = entry;
		this.attacker = attacker;
		this.target = target;
		this.weapon = weapon == null ? ItemStack.EMPTY : weapon.copy();
		this.damageSource = damageSource;
		this.rawAmount = rawAmount;
		this.amount = amount;
		this.landed = landed;
		this.mainHit = mainHit;
		this.primarySpellbladeDamage = primarySpellbladeDamage;
		this.primaryLifestealHeal = primaryLifestealHeal;
		this.surpriseSpellbladeDamage = surpriseSpellbladeDamage;
		this.surpriseLifestealHeal = surpriseLifestealHeal;
		this.executed = executed;
		this.surpriseTriggered = surpriseTriggered;
		this.surpriseLanded = surpriseLanded;
	}

	public String getEntry() {
		return entry;
	}

	public PlayerEntity getAttacker() {
		return attacker;
	}

	public Entity getTarget() {
		return target;
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

	public float getAmount() {
		return amount;
	}

	/** 兼容旧字段名：amount 表示攻击侧金额，不表示目标最终掉血。 */
	public float getAttackAmount() {
		return amount;
	}

	public boolean isLanded() {
		return landed;
	}

	public boolean isMainHit() {
		return mainHit;
	}

	public float getSpellbladeDamage() {
		return primarySpellbladeDamage + surpriseSpellbladeDamage;
	}

	public float getLifestealHeal() {
		return primaryLifestealHeal + surpriseLifestealHeal;
	}

	public float getPrimarySpellbladeDamage() {
		return primarySpellbladeDamage;
	}

	public float getPrimaryLifestealHeal() {
		return primaryLifestealHeal;
	}

	public float getSurpriseSpellbladeDamage() {
		return surpriseSpellbladeDamage;
	}

	public float getSurpriseLifestealHeal() {
		return surpriseLifestealHeal;
	}

	public boolean isExecuted() {
		return executed;
	}

	public boolean isSurpriseTriggered() {
		return surpriseTriggered;
	}

	public boolean isSurpriseLanded() {
		return surpriseLanded;
	}
}
