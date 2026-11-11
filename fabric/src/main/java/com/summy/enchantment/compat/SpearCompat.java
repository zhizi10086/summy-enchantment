package com.summy.enchantment.compat;

import com.summy.enchantment.combat.CombatFormulas;
import com.summy.enchantment.combat.EnchantmentEffects;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 与矛模组 Backported Spears（mod id: spears）的兼容层。
 *
 * <p>这里刻意**不引用对方的任何类**：物品判定靠命名空间/类名前缀，攻击逻辑靠独立的
 * 可选 Mixin（{@code SpearItemCompatMixin}）。因此没装矛模组时本类恒为"未生效"，
 * 对原版行为零影响。
 *
 * <p>矛模组 1.4.7 的 {@code pierce} 在玩家分支里把基础伤害算了两遍，
 * 因此该版本仍需按旧公式反解参数。1.5.0 起上游只叠加一次基础伤害，
 * 但 SE 的力量Plus仍需通过参数反解进入同一条结算路径。
 */
public final class SpearCompat {
	/** 反解后的矛伤害参数，以及事件里应公开的攻击侧金额。 */
	public record PierceAdjustment(float parameter, float attackAmount) {
	}

	private static final String MOD_ID = "spears";
	private static final String PACKAGE_PREFIX = "com.notunanancyowen.spears.";
	private static final Pattern VERSION_PREFIX = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)");
	private static final long LEGACY_VERSION = versionKey(1, 4, 7);
	private static final long FIXED_VERSION = versionKey(1, 5, 0);
	private static final float MIN_COOLDOWN = 1.0E-4F;

	private enum CompatibilityMode {
		LEGACY_DOUBLE_BASE,
		FIXED_SINGLE_BASE,
		UNSUPPORTED
	}

	private static volatile CompatibilityMode mode;

	private SpearCompat() {
	}

	/** 是否是矛模组的矛（未安装矛模组时恒为 false） */
	public static boolean isSpear(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		if (stack.getItem().getClass().getName().startsWith(PACKAGE_PREFIX)) {
			return true;
		}
		Identifier id = Registries.ITEM.getId(stack.getItem());
		return id != null && MOD_ID.equals(id.getNamespace());
	}

	/** 兼容是否生效（1.4.7 或 1.5.0+） */
	public static boolean isActive() {
		return compatibilityMode() != CompatibilityMode.UNSUPPORTED;
	}

	private static CompatibilityMode compatibilityMode() {
		CompatibilityMode current = mode;
		if (current == null) {
			synchronized (SpearCompat.class) {
				current = mode;
				if (current == null) {
					current = FabricLoader.getInstance().getModContainer(MOD_ID)
							.map(container -> classifyVersion(
									container.getMetadata().getVersion().getFriendlyString()))
							.orElse(CompatibilityMode.UNSUPPORTED);
					mode = current;
				}
			}
		}
		return current;
	}

	private static CompatibilityMode classifyVersion(String rawVersion) {
		Matcher matcher = VERSION_PREFIX.matcher(rawVersion == null ? "" : rawVersion.trim());
		if (!matcher.find()) {
			return CompatibilityMode.UNSUPPORTED;
		}
		try {
			long key = versionKey(
					Integer.parseInt(matcher.group(1)),
					Integer.parseInt(matcher.group(2)),
					Integer.parseInt(matcher.group(3))
			);
			if (key == LEGACY_VERSION) {
				return CompatibilityMode.LEGACY_DOUBLE_BASE;
			}
			if (key >= FIXED_VERSION) {
				return CompatibilityMode.FIXED_SINGLE_BASE;
			}
		} catch (NumberFormatException ignored) {
			// 版本号非法时保持不支持状态，避免按错误公式修改伤害。
		}
		return CompatibilityMode.UNSUPPORTED;
	}

	private static long versionKey(int major, int minor, int patch) {
		return ((long) major << 32) ^ ((long) minor << 16) ^ patch;
	}

	/**
	 * 修正矛模组传给 {@code pierce} 的伤害参数。
	 *
	 * @param attacker       使用矛的生物
	 * @param spear          矛本体（附魔等级取自它）
	 * @param target         本次目标
	 * @param originalDamage 矛模组原本要传入的参数
	 * @param leftClick      true = 左键刺击（stab），false = 右键充能刺击
	 * @return 反解后的参数，使矛模组算出的最终伤害等于我们期望的值
	 */
	public static PierceAdjustment adjustPierceDamage(LivingEntity attacker, ItemStack spear, @Nullable Entity target,
			float originalDamage, boolean leftClick) {
		// 只有玩家分支存在"基础伤害算两遍"的问题，其它生物原样放行
		CompatibilityMode currentMode = compatibilityMode();
		if (!(attacker instanceof PlayerEntity player)
				|| currentMode == CompatibilityMode.UNSUPPORTED
				|| !Float.isFinite(originalDamage)
				|| originalDamage <= 0.0F) {
			return new PierceAdjustment(originalDamage, originalDamage);
		}

		EntityGroup group = target instanceof LivingEntity living ? living.getGroup() : EntityGroup.DEFAULT;
		float enchant = EnchantmentHelper.getAttackDamage(spear, group);

		// 复刻矛模组自己的口径：充能中按满蓄力算，否则取攻击冷却进度
		boolean usingItem = attacker.isUsingItem();
		float cooldown = usingItem ? 1.0F : MathHelper.clamp(player.getAttackCooldownProgress(0.5F), 0.0F, 1.0F);
		// 期望的"不再翻倍"的伤害：左键 = 基础 + 附魔×蓄力；右键 = 原参数（已含基础+充能奖励）
		float wanted = leftClick ? originalDamage + cooldown * enchant : originalDamage;
		if (currentMode == CompatibilityMode.FIXED_SINGLE_BASE && !leftClick) {
			// 1.5.0+ 的 usageTick 只传基础+速度，附魔伤害由 pierce 随后补入一次。
			wanted = originalDamage + enchant;
		}
		// 我们的倍率：力量Plus 仅左键。
		// （1.3.7 起"死神祝福·造成侧"统一在受伤侧按攻击者结算，这里不再乘，避免重复叠加）
		float multiplier = 1.0F;
		if (leftClick) {
			multiplier = CombatFormulas.strengthMultiplier(attacker);
		}
		wanted *= multiplier;

		if (!Float.isFinite(wanted)) {
			return new PierceAdjustment(originalDamage, originalDamage);
		}

		float parameter;
		if (currentMode == CompatibilityMode.LEGACY_DOUBLE_BASE) {
			// 1.4.7 最终值 = 参数·(0.2 + 0.8·cd² + cd) + cd·附魔。
			float factor = (usingItem ? 1.0F : 0.2F + 0.8F * cooldown * cooldown) + cooldown;
			parameter = (wanted - cooldown * enchant) / Math.max(factor, MIN_COOLDOWN);
		} else if (leftClick) {
			// 1.5.0+ 最终值 = cd·(参数 + 附魔)。
			if (cooldown <= MIN_COOLDOWN) {
				return new PierceAdjustment(0.0F, 0.0F);
			}
			parameter = wanted / cooldown - enchant;
		} else {
			// 1.5.0+ 右键参数只含基础+速度；附魔伤害由 pierce 随后叠加一次。
			parameter = originalDamage;
		}

		if (!Float.isFinite(parameter)) {
			return new PierceAdjustment(originalDamage, originalDamage);
		}
		parameter = Math.max(0.0F, parameter);
		return new PierceAdjustment(parameter, wanted);
	}

	/** 结算矛命中的魔剑、嗜血，并发送与近战同结构的只读事件。 */
	public static void settleHit(LivingEntity attacker, ItemStack spear, Entity target,
			float rawAmount, PierceAdjustment adjustment, boolean landed, String entry) {
		if (!(attacker instanceof PlayerEntity player) || !isActive()) {
			return;
		}
		float magic = 0.0F;
		float healed = 0.0F;
		if (landed) {
			magic = EnchantmentEffects.applySpellbladeReport(player, spear, target, adjustment.attackAmount());
			healed = EnchantmentEffects.applyLifestealReport(player, spear, adjustment.attackAmount());
		}
		SummyEnchantmentScriptHooks.fireMeleeSettled(
				entry, player, target, spear, null, rawAmount, adjustment.attackAmount(), landed, true,
				magic, healed, 0.0F, 0.0F, false, false, false);
	}
}
