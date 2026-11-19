package com.summy.enchantment.compat;

import com.summy.enchantment.SummyEnchantment;
import com.summy.enchantment.combat.CombatFormulas;
import com.summy.enchantment.combat.EnchantmentEffects;
import com.summy.enchantment.combat.MeleeSettlement;
import com.summy.enchantment.config.SummyEnchantmentConfig;
import com.summy.enchantment.registry.EnchantmentRef;
import com.summy.enchantment.network.ParticlePreferences;
import com.summy.enchantment.particle.MeleeParticles;
import com.summy.enchantment.registry.ModEnchantments;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/** SR 专用入口及其注入诊断。 */
public final class SummyReliquaryCompat {
	private static boolean loggedFirstHit;

	private SummyReliquaryCompat() {
	}

	public static void settleHit(String entry, Player attacker, ItemStack weapon, Entity target,
			DamageSource source, float rawAmount, float amount, boolean landed, boolean allowExecute,
			boolean allowSurprise, boolean mainHit) {
		MeleeSettlement.Result result = MeleeSettlement.settleHit(entry, attacker, weapon, target, source,
				rawAmount, amount, landed, allowExecute, allowSurprise, mainHit);
		if (result == null || attacker.level().isClientSide()) {
			return;
		}
		if (!loggedFirstHit) {
			loggedFirstHit = true;
			SummyEnchantment.LOGGER.info("[SE] Summy Reliquary 近战兼容已生效：本次命中按近战管线结算");
		}
		logSettlement(entry, attacker, weapon, target, rawAmount, amount, landed, result.magic(), result.healed(),
				result.executed(), allowExecute, allowSurprise, result.targetGone(), result.surprise(),
				result.surpriseLanded());
	}

	// ==================== 诊断日志（debug_logs） ====================

	/** 每 tick 的输出上限：一次斩击打一堆目标时不至于刷屏 */
	private static final int DEBUG_LINES_PER_TICK = 8;
	private static long debugTick = Long.MIN_VALUE;
	private static int debugLinesThisTick;
	private static String debugLastLine;

	/**
	 * `[SE-SR]` 逐次结算日志：把这一击的原始金额、乘区（力量 / 复仇）、魔剑 / 嗜血 / 斩杀 / 出其不意的实际结果、
	 * 目标血量与粒子档位写进一行，用来在实机上一眼判断"某个附魔到底有没有生效"。
	 *
	 * <p>只在 {@code debug_logs} 打开、且本环境真的装了 SR 时输出；每 tick 最多 8 行、同内容去重。
	 */
	private static void logSettlement(String entry, Player attacker, ItemStack weapon, Entity target,
			float rawAmount, float amount, boolean landed, float magic, float healed, boolean executed,
			boolean allowExecute, boolean allowSurprise, boolean targetGone,
			EnchantmentEffects.SurpriseRoll surprise, boolean surpriseLanded) {
		if (!SummyEnchantmentConfig.get().debugLogs) {
			return;
		}
		long tick = attacker.level().getGameTime();
		if (tick != debugTick) {
			debugTick = tick;
			debugLinesThisTick = 0;
			debugLastLine = null;
		}
		if (debugLinesThisTick >= DEBUG_LINES_PER_TICK) {
			return;
		}
		String line = buildSettlementLine(entry, attacker, weapon, target, rawAmount, amount, landed, magic, healed,
				executed, allowExecute, allowSurprise, targetGone, surprise, surpriseLanded);
		if (line.equals(debugLastLine)) {
			return;
		}
		debugLastLine = line;
		debugLinesThisTick++;
		SummyEnchantment.LOGGER.info(line);
	}

	/** 拼一行结算日志（字段顺序固定，便于对照） */
	private static String buildSettlementLine(String entry, Player attacker, ItemStack weapon, Entity target,
			float rawAmount, float amount, boolean landed, float magic, float healed, boolean executed,
			boolean allowExecute, boolean allowSurprise, boolean targetGone,
			EnchantmentEffects.SurpriseRoll surprise, boolean surpriseLanded) {
		int spellblade = levelOf(weapon, ModEnchantments.SPELLBLADE);
		int execution = levelOf(weapon, ModEnchantments.EXECUTION);
		int surpriseLevel = levelOf(weapon, ModEnchantments.SURPRISE);
		// 藏剑：等级 + 当前蓄力（tick 与等效百分比）——这两个值不参与乘区打印，但能一眼看出它有没有在工作
		int hiddenBlade = levelOf(weapon, ModEnchantments.HIDDEN_BLADE);
		int hiddenBladeTicks = hiddenBlade > 0 ? EnchantmentEffects.hiddenBladeChargedTicks(attacker) : 0;
		float hiddenBladeBonus = hiddenBlade > 0 ? EnchantmentEffects.hiddenBladeBonus(attacker) : 0.0F;
		float hiddenBladePierce = hiddenBlade > 0 ? EnchantmentEffects.hiddenBladeChargeProgress(attacker) : 0.0F;
		StringBuilder sb = new StringBuilder(240);
		sb.append("[SE-SR] ").append(entry)
				.append(" | ").append(attacker.getGameProfile().getName())
				.append(" 手持 ").append(BuiltInRegistries.ITEM.getKey(weapon.getItem()))
				.append("（魔剑").append(spellblade)
				.append(" / 斩杀").append(execution)
				.append(" / 出其不意").append(surpriseLevel)
				.append(" / 藏剑").append(hiddenBlade)
				.append("·蓄").append(hiddenBladeTicks).append("tick(+").append(fmt(hiddenBladeBonus))
				.append(",破甲").append(Math.round(hiddenBladePierce * 100.0F)).append("%)").append("）")
				.append(" | ").append(fmt(rawAmount))
				.append(" × 力量").append(fmt(CombatFormulas.strengthMultiplier(attacker)))
				.append(" × 复仇").append(fmt(EnchantmentEffects.revengeMultiplier(attacker)))
				.append(" = ").append(fmt(amount))
				.append(" | hurt=").append(landed);
		if (target instanceof LivingEntity living) {
			sb.append(" | 目标 ").append(living.getName().getString())
					.append(' ').append(fmt(living.getHealth())).append('/').append(fmt(living.getMaxHealth()));
		}
		if (!landed) {
			sb.append(" | 未落地：不结算任何命中后效果");
		} else {
			sb.append(" | 魔剑+").append(fmt(magic));
			sb.append(" 嗜血+").append(fmt(healed));
			sb.append(" 斩杀").append(executionNote(execution, targetGone, allowExecute, executed));
			sb.append(" | ").append(surpriseNote(surpriseLevel, targetGone, allowSurprise, surprise, surpriseLanded));
		}
		return sb.append(" | 粒子 ").append(particleNote(attacker)).toString();
	}

	/** 斩杀这一项的结论 */
	private static String executionNote(int level, boolean targetGone, boolean allowExecute, boolean executed) {
		if (level <= 0) {
			return "-（武器无附魔）";
		}
		if (!allowExecute) {
			return "-（设计不判）";
		}
		if (targetGone) {
			return "-（目标已死）";
		}
		return executed ? "=已补刀" : "=未达门槛";
	}

	/** 出其不意这一项的结论：几率 / 掷值 / 是否命中 / 二次伤害是否落地 */
	private static String surpriseNote(int level, boolean targetGone, boolean allowSurprise,
			EnchantmentEffects.SurpriseRoll surprise, boolean surpriseLanded) {
		if (!allowSurprise) {
			return "出其不意-（设计不掷）";
		}
		if (level <= 0) {
			return "出其不意-（武器无附魔）";
		}
		if (targetGone) {
			return "出其不意-（目标已死）";
		}
		if (surprise == null) {
			return "出其不意-（未判定）";
		}
		String outcome = surprise.hit()
				? (surpriseLanded ? " → 命中且落地" : " → 命中但二次伤害未落地")
				: " → 未命中";
		return "出其不意 " + String.format(Locale.ROOT, "%.1f", surprise.chance() * 100.0F) + "% 掷"
				+ fmt(surprise.roll()) + outcome;
	}

	/** 粒子档位说明（攻击者自己的上报值） */
	private static String particleNote(Player attacker) {
		ParticlePreferences.Preference preference = ParticlePreferences.preferenceOf(attacker.getUUID());
		if (preference == null) {
			return "档位未上报（按原版）";
		}
		return "主命中" + preference.meleeStyle() + " / 横扫" + preference.sweepStyle()
				+ " / 出其不意" + preference.surpriseStyle() + " / 强度" + preference.meleeEffect()
				// SR 的斩击弧线是否由我们接管（与 SR 弧线注入用的是同一个判据）
				+ "（SR 弧线接管=" + (MeleeParticles.takesOverExternalSlashArc(attacker) ? "是" : "否") + "）";
	}

	private static String fmt(float value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	/** 武器上的附魔等级（附魔对象为空时按 0 处理） */
	private static int levelOf(ItemStack stack, EnchantmentRef enchantment) {
		return enchantment == null ? 0 : EnchantmentEffects.levelOf(stack, enchantment);
	}
}
