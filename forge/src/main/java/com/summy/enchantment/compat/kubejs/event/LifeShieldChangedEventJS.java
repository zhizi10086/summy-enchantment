package com.summy.enchantment.compat.kubejs.event;

import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.world.entity.LivingEntity;

/** KubeJS 只读事件：SE 生命护盾值发生变化。 */
public final class LifeShieldChangedEventJS extends EventJS {
	private final LivingEntity entity;
	private final String action;
	private final float before;
	private final float after;
	private final float cap;

	public LifeShieldChangedEventJS(LivingEntity entity, String action, float before, float after, float cap) {
		this.entity = entity;
		this.action = action;
		this.before = before;
		this.after = after;
		this.cap = cap;
	}

	public LivingEntity getEntity() {
		return entity;
	}

	public String getAction() {
		return action;
	}

	public float getBefore() {
		return before;
	}

	public float getAfter() {
		return after;
	}

	public float getDelta() {
		return after - before;
	}

	public float getCap() {
		return cap;
	}
}
