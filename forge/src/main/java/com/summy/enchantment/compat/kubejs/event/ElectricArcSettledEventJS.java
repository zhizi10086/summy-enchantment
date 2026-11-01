package com.summy.enchantment.compat.kubejs.event;

import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** KubeJS 只读事件：电刀弹射的一段伤害尝试已完成。 */
public final class ElectricArcSettledEventJS extends EventJS {
	private final long chainId;
	private final int segmentIndex;
	private final int totalSegments;
	private final int remainingSegments;
	private final Player attacker;
	private final LivingEntity target;
	private final ItemStack weapon;
	private final DamageSource damageSource;
	private final float baseAmount;
	private final float amount;
	private final boolean landed;
	private final float spellbladeDamage;
	private final float lifestealHeal;

	public ElectricArcSettledEventJS(long chainId, int segmentIndex, int totalSegments, int remainingSegments,
			Player attacker, LivingEntity target, ItemStack weapon, DamageSource damageSource,
			float baseAmount, float amount, boolean landed, float spellbladeDamage, float lifestealHeal) {
		this.chainId = chainId;
		this.segmentIndex = segmentIndex;
		this.totalSegments = totalSegments;
		this.remainingSegments = remainingSegments;
		this.attacker = attacker;
		this.target = target;
		this.weapon = weapon == null ? ItemStack.EMPTY : weapon.copy();
		this.damageSource = damageSource;
		this.baseAmount = baseAmount;
		this.amount = amount;
		this.landed = landed;
		this.spellbladeDamage = spellbladeDamage;
		this.lifestealHeal = lifestealHeal;
	}

	public long getChainId() {
		return chainId;
	}

	public int getSegmentIndex() {
		return segmentIndex;
	}

	public int getTotalSegments() {
		return totalSegments;
	}

	public int getRemainingSegments() {
		return remainingSegments;
	}

	public Player getAttacker() {
		return attacker;
	}

	public LivingEntity getTarget() {
		return target;
	}

	public ItemStack getWeapon() {
		return weapon;
	}

	public DamageSource getDamageSource() {
		return damageSource;
	}

	public float getBaseAmount() {
		return baseAmount;
	}

	public float getAmount() {
		return amount;
	}

	public float getAttackAmount() {
		return amount;
	}

	public boolean isLanded() {
		return landed;
	}

	public float getSpellbladeDamage() {
		return spellbladeDamage;
	}

	public float getLifestealHeal() {
		return lifestealHeal;
	}
}
