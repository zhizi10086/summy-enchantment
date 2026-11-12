package com.summy.enchantment.compat.kubejs.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** KubeJS 只读事件：死亡之眼层数变化。 */
public final class DeathEyeChangedEventJS implements KubeEvent {
	private final Player player;
	private final Entity arrow;
	private final Entity target;
	private final String action;
	private final int before;
	private final int after;

	public DeathEyeChangedEventJS(Player player, Entity arrow, Entity target,
			String action, int before, int after) {
		this.player = player;
		this.arrow = arrow;
		this.target = target;
		this.action = action;
		this.before = before;
		this.after = after;
	}

	public Player getPlayer() {
		return player;
	}

	public String getPlayerId() {
		return player.getStringUUID();
	}

	public Entity getArrow() {
		return arrow;
	}

	public Entity getTarget() {
		return target;
	}

	public String getAction() {
		return action;
	}

	public int getBefore() {
		return before;
	}

	public int getAfter() {
		return after;
	}

	public int getDelta() {
		return after - before;
	}
}
