package com.summy.enchantment.compat.kubejs.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/** KubeJS 只读事件：藏剑带充能的一刀已经挥出并清空。 */
public final class HiddenBladeConsumedEventJS implements KubeEvent {
	private final PlayerEntity player;
	private final Entity target;
	private final boolean landed;
	private final int chargedTicks;
	private final float bonus;
	private final float progress;

	public HiddenBladeConsumedEventJS(PlayerEntity player, Entity target, boolean landed,
			int chargedTicks, float bonus, float progress) {
		this.player = player;
		this.target = target;
		this.landed = landed;
		this.chargedTicks = chargedTicks;
		this.bonus = bonus;
		this.progress = progress;
	}

	public PlayerEntity getPlayer() {
		return player;
	}

	public Entity getTarget() {
		return target;
	}

	public boolean isLanded() {
		return landed;
	}

	public int getChargedTicks() {
		return chargedTicks;
	}

	public float getBonus() {
		return bonus;
	}

	public float getProgress() {
		return progress;
	}
}
