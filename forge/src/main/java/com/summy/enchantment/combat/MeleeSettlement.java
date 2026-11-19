package com.summy.enchantment.combat;

import com.summy.enchantment.compat.SummyEnchantmentScriptHooks;
import com.summy.enchantment.particle.MeleeParticles;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class MeleeSettlement {
	private MeleeSettlement() {
	}

	/**
	 * 近战伤害乘区：力量 × 复仇，与 {@code PlayerEntityMixin} 里的算法一致。
	 *
	 * <p>1.3.7 起"死神祝福·造成侧"改为全类型、统一在受伤侧按攻击者结算，因此这里不再乘它
	 * （否则近战会与受伤侧那次重复叠加）。
	 */
	public static float meleeMultiplier(Player attacker) {
		return CombatFormulas.strengthMultiplier(attacker)
				* EnchantmentEffects.revengeMultiplier(attacker);
	}

	/**
	 * 近战命中后的统一结算：SR 技能与普通近战共用同一套规则。
	 *
	 * <p>顺序与原来的实现逐行一致：魔剑 → 嗜血 → 主命中粒子 → 斩杀（仅主命中）→ 出其不意。
	 * 本轮收紧两处口径：
	 * <ul>
	 *     <li>这一击已经把目标打死（或目标本来就已死）时，不再补魔剑 / 斩杀 / 出其不意，也不再发它们的粒子；
	 *     嗜血照常按本次伤害回血（击杀也要回血）；</li>
	 *     <li>出其不意的粒子只在<b>二次伤害确实落地</b>之后才播，避免"只有粒子、没有伤害"的假象。</li>
	 * </ul>
	 *
	 * @param entry         注入点标签（诊断日志用）：strike / heavySlash / thrownSpear / melee
	 * @param rawAmount     未乘乘区的原始金额
	 * @param amount        攻击侧金额（已乘力量 × 复仇；死神祝福·造成侧自 1.3.7 起在受伤侧统一结算）
	 * @param landed        这一击是否真的落地（原版 hurt 的返回值）
	 * @param allowExecute  是否允许斩杀判定
	 * @param allowSurprise 是否允许出其不意（强力斩击按设计传 false）
	 * @param mainHit       是否主命中（横扫的附加目标传 false：不掷斩杀、不出主命中粒子）
	 */
	public static Result settleHit(String entry, Player attacker, ItemStack weapon, Entity target,
			DamageSource source, float rawAmount, float amount, boolean landed, boolean allowExecute,
			boolean allowSurprise, boolean mainHit) {
		if (attacker == null || target == null) {
			return null;
		}
		boolean targetGone = target instanceof LivingEntity living && (living.isDeadOrDying()
				|| living.getHealth() <= 0.0F);

		float primaryMagic = 0.0F;
		float primaryHealed = 0.0F;
		float surpriseMagic = 0.0F;
		float surpriseHealed = 0.0F;
		boolean executed = false;
		EnchantmentEffects.SurpriseRoll surprise = null;
		boolean surpriseLanded = false;

		if (landed) {
			if (!targetGone) {
				primaryMagic = EnchantmentEffects.applySpellbladeReport(attacker, weapon, target, amount);
			}
			// 嗜血与目标是否还活着无关：击杀也要按本次伤害回血
			primaryHealed = EnchantmentEffects.applyLifestealReport(attacker, weapon, amount);
			if (mainHit) {
				MeleeParticles.spawnOnHit(attacker, target);
			}
			if (!targetGone && mainHit && allowExecute) {
				executed = EnchantmentEffects.tryExecuteReport(attacker, target, weapon);
			}
			if (!targetGone && allowSurprise) {
				surprise = EnchantmentEffects.rollSurpriseRoll(attacker, weapon);
				if (surprise.hit()) {
					// 与近战管线保持一致：先清无敌帧，再用同一个伤害源与金额补一次
					if (target instanceof LivingEntity livingTarget) {
						livingTarget.invulnerableTime = 0;
					}
					if (target.hurt(source, amount)) {
						surpriseLanded = true;
						MeleeParticles.spawnSurprise(attacker, target);
						surpriseMagic = EnchantmentEffects.applySpellbladeReport(attacker, weapon, target, amount);
						surpriseHealed = EnchantmentEffects.applyLifestealReport(attacker, weapon, amount);
					}
				}
			}
		}

		if (!attacker.level().isClientSide) {
			SummyEnchantmentScriptHooks.fireMeleeSettled(entry, attacker, target, weapon, source,
					rawAmount, amount, landed, mainHit, primaryMagic, primaryHealed,
					surpriseMagic, surpriseHealed, executed,
					surprise != null && surprise.hit(), surpriseLanded);
		}
		return new Result(targetGone, primaryMagic + surpriseMagic, primaryHealed + surpriseHealed,
				executed, surprise, surpriseLanded);
	}

	/** 兼容诊断使用的实际结算结果。 */
	public record Result(boolean targetGone, float magic, float healed, boolean executed,
			EnchantmentEffects.SurpriseRoll surprise, boolean surpriseLanded) {
	}
}
