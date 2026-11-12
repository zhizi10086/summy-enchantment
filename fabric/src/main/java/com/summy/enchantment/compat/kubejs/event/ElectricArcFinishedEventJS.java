package com.summy.enchantment.compat.kubejs.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/** KubeJS 只读事件：一条电刀弹射链结束。 */
public final class ElectricArcFinishedEventJS implements KubeEvent {
	private final long chainId;
	private final PlayerEntity attacker;
	private final ItemStack weapon;
	private final int completedSegments;
	private final int totalSegments;
	private final String reason;

	public ElectricArcFinishedEventJS(long chainId, PlayerEntity attacker, ItemStack weapon,
			int completedSegments, int totalSegments, String reason) {
		this.chainId = chainId;
		this.attacker = attacker;
		this.weapon = weapon == null ? ItemStack.EMPTY : weapon.copy();
		this.completedSegments = completedSegments;
		this.totalSegments = totalSegments;
		this.reason = reason;
	}

	public long getChainId() {
		return chainId;
	}

	public PlayerEntity getAttacker() {
		return attacker;
	}

	public ItemStack getWeapon() {
		return weapon;
	}

	public int getCompletedSegments() {
		return completedSegments;
	}

	public int getTotalSegments() {
		return totalSegments;
	}

	public String getReason() {
		return reason;
	}
}
