package com.summy.enchantment.compat.kubejs;

import com.summy.enchantment.compat.SummyEnchantmentScriptHooks;
import com.summy.enchantment.compat.kubejs.event.DeathEyeChangedEventJS;
import com.summy.enchantment.compat.kubejs.event.ElectricArcFinishedEventJS;
import com.summy.enchantment.compat.kubejs.event.ElectricArcSettledEventJS;
import com.summy.enchantment.compat.kubejs.event.HiddenBladeConsumedEventJS;
import com.summy.enchantment.compat.kubejs.event.LifeShieldChangedEventJS;
import com.summy.enchantment.compat.kubejs.event.MeleeSettledEventJS;
import com.summy.enchantment.compat.kubejs.event.ProjectileSettledEventJS;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;

/** Summy Enchantment 的 KubeJS 服务端只读事件定义。 */
public interface SummyEnchantmentKubeJSEvents {
	EventGroup GROUP = EventGroup.of("SummyEnchantmentEvents");
	EventHandler MELEE_SETTLED = GROUP.server("meleeSettled", () -> MeleeSettledEventJS.class);
	EventHandler LIFE_SHIELD_CHANGED = GROUP.server("lifeShieldChanged", () -> LifeShieldChangedEventJS.class);
	EventHandler DEATH_EYE_CHANGED = GROUP.server("deathEyeChanged", () -> DeathEyeChangedEventJS.class);
	EventHandler HIDDEN_BLADE_CONSUMED = GROUP.server("hiddenBladeConsumed", () -> HiddenBladeConsumedEventJS.class);
	EventHandler PROJECTILE_SETTLED = GROUP.server("projectileSettled", () -> ProjectileSettledEventJS.class);
	EventHandler ELECTRIC_ARC_SETTLED = GROUP.server("electricArcSettled", () -> ElectricArcSettledEventJS.class);
	EventHandler ELECTRIC_ARC_FINISHED = GROUP.server("electricArcFinished", () -> ElectricArcFinishedEventJS.class);

	/** 把核心桥接到 KubeJS 事件；重复调用只会替换为等价的当前插件实例回调。 */
	static void installHooks() {
		SummyEnchantmentScriptHooks.setMeleeSettledListener((entry, attacker, target, weapon, source,
				rawAmount, amount, landed, mainHit, primarySpellbladeDamage, primaryLifestealHeal,
				surpriseSpellbladeDamage, surpriseLifestealHeal, executed,
				surpriseTriggered, surpriseLanded) -> {
			if (hasListeners(MELEE_SETTLED)) {
				MELEE_SETTLED.post(ScriptType.SERVER, new MeleeSettledEventJS(entry, attacker, target, weapon,
						source, rawAmount, amount, landed, mainHit, primarySpellbladeDamage,
						primaryLifestealHeal, surpriseSpellbladeDamage, surpriseLifestealHeal,
						executed, surpriseTriggered, surpriseLanded));
			}
		});
		SummyEnchantmentScriptHooks.setLifeShieldChangedListener((entity, action, before, after, cap) -> {
			if (hasListeners(LIFE_SHIELD_CHANGED)) {
				LIFE_SHIELD_CHANGED.post(ScriptType.SERVER,
						new LifeShieldChangedEventJS(entity, action, before, after, cap));
			}
		});
		SummyEnchantmentScriptHooks.setDeathEyeChangedListener((player, arrow, target, action, before, after) -> {
			if (hasListeners(DEATH_EYE_CHANGED)) {
				DEATH_EYE_CHANGED.post(ScriptType.SERVER,
						new DeathEyeChangedEventJS(player, arrow, target, action, before, after));
			}
		});
		SummyEnchantmentScriptHooks.setHiddenBladeConsumedListener((player, target, landed,
				chargedTicks, bonus, progress) -> {
			if (hasListeners(HIDDEN_BLADE_CONSUMED)) {
				HIDDEN_BLADE_CONSUMED.post(ScriptType.SERVER,
						new HiddenBladeConsumedEventJS(player, target, landed, chargedTicks, bonus, progress));
			}
		});
		SummyEnchantmentScriptHooks.setProjectileSettledListener((kind, attacker, target, projectile, weapon,
				source, rawAmount, amount, landed, extraProjectile, deathEyeMultiplier,
				spellbladeDamage, lifestealHeal, surprise) -> {
			if (hasListeners(PROJECTILE_SETTLED)) {
				PROJECTILE_SETTLED.post(ScriptType.SERVER,
						new ProjectileSettledEventJS(kind, attacker, target, projectile, weapon, source,
								rawAmount, amount, landed, extraProjectile, deathEyeMultiplier,
								spellbladeDamage, lifestealHeal, surprise));
			}
		});
		SummyEnchantmentScriptHooks.setElectricArcSettledListener((chainId, segmentIndex, totalSegments,
				remainingSegments, attacker, target, weapon, source, baseAmount, amount, landed,
				spellbladeDamage, lifestealHeal) -> {
			if (hasListeners(ELECTRIC_ARC_SETTLED)) {
				ELECTRIC_ARC_SETTLED.post(ScriptType.SERVER,
						new ElectricArcSettledEventJS(chainId, segmentIndex, totalSegments, remainingSegments,
								attacker, target, weapon, source, baseAmount, amount, landed,
								spellbladeDamage, lifestealHeal));
			}
		});
		SummyEnchantmentScriptHooks.setElectricArcFinishedListener((chainId, attacker, weapon,
				completedSegments, totalSegments, reason) -> {
			if (hasListeners(ELECTRIC_ARC_FINISHED)) {
				ELECTRIC_ARC_FINISHED.post(ScriptType.SERVER,
						new ElectricArcFinishedEventJS(chainId, attacker, weapon,
								completedSegments, totalSegments, reason));
			}
		});
	}

	private static boolean hasListeners(EventHandler handler) {
		return handler.hasListeners();
	}
}
