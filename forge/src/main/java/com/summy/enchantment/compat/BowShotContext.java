package com.summy.enchantment.compat;

import net.minecraft.world.item.ItemStack;

/**
 * 记录一次弓松手流程中的武器和真实蓄力 tick 数。
 */
public final class BowShotContext {
	private static final ThreadLocal<ItemStack> CURRENT_BOW = new ThreadLocal<>();
	private static final ThreadLocal<Integer> CURRENT_DRAW_TICKS = new ThreadLocal<>();

	private BowShotContext() {
	}

	public static void begin(ItemStack bow, int drawTicks) {
		CURRENT_BOW.set(bow);
		CURRENT_DRAW_TICKS.set(Math.max(0, drawTicks));
	}

	public static ItemStack currentBow() {
		return CURRENT_BOW.get();
	}

	public static int currentDrawTicks() {
		Integer ticks = CURRENT_DRAW_TICKS.get();
		return ticks == null ? 0 : ticks;
	}

	public static void end() {
		CURRENT_BOW.remove();
		CURRENT_DRAW_TICKS.remove();
	}
}
