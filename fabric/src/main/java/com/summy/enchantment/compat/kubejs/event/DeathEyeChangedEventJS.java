package com.summy.enchantment.compat.kubejs.event;

import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/** KubeJS 只读事件：死亡之眼层数变化。 */
public final class DeathEyeChangedEventJS extends EventJS {
	private final PlayerEntity player;
	private final Entity arrow;
	private final Entity target;
	private final String action;
	private final int before;
	private final int after;

	public DeathEyeChangedEventJS(PlayerEntity player, Entity arrow, Entity target,
			String action, int before, int after) {
		this.player = player;
		this.arrow = arrow;
		this.target = target;
		this.action = action;
		this.before = before;
		this.after = after;
	}

	public PlayerEntity getPlayer() {
		return player;
	}

	public String getPlayerId() {
		return player.getUuidAsString();
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
