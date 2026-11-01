package com.summy.enchantment.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

/**
 * 核心代码与可选 KubeJS 事件层之间的无依赖桥。
 *
 * <p>核心类只引用本类，不引用任何 KubeJS 类型。未安装 KubeJS 时监听器保持 null，
 * 所有 fire 方法都会直接返回，不会触发 KubeJS 类加载。
 */
public final class SummyEnchantmentScriptHooks {
	public interface MeleeSettledListener {
		void onMeleeSettled(String entry, Player attacker, Entity target, ItemStack weapon,
				DamageSource source, float rawAmount, float amount, boolean landed, boolean mainHit,
				float primarySpellbladeDamage, float primaryLifestealHeal,
				float surpriseSpellbladeDamage, float surpriseLifestealHeal, boolean executed,
				boolean surpriseTriggered, boolean surpriseLanded);
	}

	public interface LifeShieldChangedListener {
		void onLifeShieldChanged(LivingEntity entity, String action, float before, float after, float cap);
	}

	public interface DeathEyeChangedListener {
		void onDeathEyeChanged(Player player, Entity arrow, Entity target,
				String action, int before, int after);
	}

	public interface HiddenBladeConsumedListener {
		void onHiddenBladeConsumed(Player player, Entity target, boolean landed,
				int chargedTicks, float bonus, float progress);
	}

	public interface ProjectileSettledListener {
		void onProjectileSettled(String kind, LivingEntity attacker, Entity target, Entity projectile,
				ItemStack weapon, DamageSource source, float rawAmount, float amount, boolean landed,
				boolean extraProjectile, float deathEyeMultiplier,
				float spellbladeDamage, float lifestealHeal, boolean surprise);
	}

	public interface ElectricArcSettledListener {
		void onElectricArcSettled(long chainId, int segmentIndex, int totalSegments, int remainingSegments,
				Player attacker, LivingEntity target, ItemStack weapon, DamageSource source,
				float baseAmount, float amount, boolean landed,
				float spellbladeDamage, float lifestealHeal);
	}

	public interface ElectricArcFinishedListener {
		void onElectricArcFinished(long chainId, Player attacker, ItemStack weapon,
				int completedSegments, int totalSegments, String reason);
	}

	private static volatile MeleeSettledListener meleeSettledListener;
	private static volatile LifeShieldChangedListener lifeShieldChangedListener;
	private static volatile DeathEyeChangedListener deathEyeChangedListener;
	private static volatile HiddenBladeConsumedListener hiddenBladeConsumedListener;
	private static volatile ProjectileSettledListener projectileSettledListener;
	private static volatile ElectricArcSettledListener electricArcSettledListener;
	private static volatile ElectricArcFinishedListener electricArcFinishedListener;

	private SummyEnchantmentScriptHooks() {
	}

	public static void setMeleeSettledListener(MeleeSettledListener listener) {
		meleeSettledListener = listener;
	}

	public static void setLifeShieldChangedListener(LifeShieldChangedListener listener) {
		lifeShieldChangedListener = listener;
	}

	public static void setDeathEyeChangedListener(DeathEyeChangedListener listener) {
		deathEyeChangedListener = listener;
	}

	public static void setHiddenBladeConsumedListener(HiddenBladeConsumedListener listener) {
		hiddenBladeConsumedListener = listener;
	}

	public static void setProjectileSettledListener(ProjectileSettledListener listener) {
		projectileSettledListener = listener;
	}

	public static void setElectricArcSettledListener(ElectricArcSettledListener listener) {
		electricArcSettledListener = listener;
	}

	public static void setElectricArcFinishedListener(ElectricArcFinishedListener listener) {
		electricArcFinishedListener = listener;
	}

	public static void fireMeleeSettled(String entry, Player attacker, Entity target, ItemStack weapon,
			DamageSource source, float rawAmount, float amount, boolean landed, boolean mainHit,
			float primarySpellbladeDamage, float primaryLifestealHeal,
			float surpriseSpellbladeDamage, float surpriseLifestealHeal, boolean executed,
			boolean surpriseTriggered, boolean surpriseLanded) {
		MeleeSettledListener listener = meleeSettledListener;
		if (listener != null) {
			listener.onMeleeSettled(entry, attacker, target, weapon, source, rawAmount, amount, landed, mainHit,
					primarySpellbladeDamage, primaryLifestealHeal, surpriseSpellbladeDamage,
					surpriseLifestealHeal, executed, surpriseTriggered, surpriseLanded);
		}
	}

	public static void fireLifeShieldChanged(LivingEntity entity, String action,
			float before, float after, float cap) {
		LifeShieldChangedListener listener = lifeShieldChangedListener;
		if (listener != null) {
			listener.onLifeShieldChanged(entity, action, before, after, cap);
		}
	}

	public static void fireDeathEyeChanged(Player player, Entity arrow, Entity target,
			String action, int before, int after) {
		DeathEyeChangedListener listener = deathEyeChangedListener;
		if (listener != null && player != null) {
			listener.onDeathEyeChanged(player, arrow, target, action, before, after);
		}
	}

	public static void fireHiddenBladeConsumed(Player player, Entity target, boolean landed,
			int chargedTicks, float bonus, float progress) {
		HiddenBladeConsumedListener listener = hiddenBladeConsumedListener;
		if (listener != null && player != null) {
			listener.onHiddenBladeConsumed(player, target, landed, chargedTicks, bonus, progress);
		}
	}

	public static void fireProjectileSettled(String kind, LivingEntity attacker, Entity target, Entity projectile,
			ItemStack weapon, DamageSource source, float rawAmount, float amount, boolean landed,
			boolean extraProjectile, float deathEyeMultiplier,
			float spellbladeDamage, float lifestealHeal, boolean surprise) {
		ProjectileSettledListener listener = projectileSettledListener;
		if (listener != null) {
			listener.onProjectileSettled(kind, attacker, target, projectile, weapon, source,
					rawAmount, amount, landed, extraProjectile, deathEyeMultiplier,
					spellbladeDamage, lifestealHeal, surprise);
		}
	}

	public static void fireElectricArcSettled(long chainId, int segmentIndex, int totalSegments,
			int remainingSegments, Player attacker, LivingEntity target, ItemStack weapon,
			DamageSource source, float baseAmount, float amount, boolean landed,
			float spellbladeDamage, float lifestealHeal) {
		ElectricArcSettledListener listener = electricArcSettledListener;
		if (listener != null) {
			listener.onElectricArcSettled(chainId, segmentIndex, totalSegments, remainingSegments,
					attacker, target, weapon, source, baseAmount, amount, landed,
					spellbladeDamage, lifestealHeal);
		}
	}

	public static void fireElectricArcFinished(long chainId, Player attacker, ItemStack weapon,
			int completedSegments, int totalSegments, String reason) {
		ElectricArcFinishedListener listener = electricArcFinishedListener;
		if (listener != null) {
			listener.onElectricArcFinished(chainId, attacker, weapon, completedSegments, totalSegments, reason);
		}
	}
}
