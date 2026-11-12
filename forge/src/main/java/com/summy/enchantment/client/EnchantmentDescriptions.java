package com.summy.enchantment.client;

import com.summy.enchantment.config.SummyEnchantmentConfig;
import com.summy.enchantment.combat.EnchantmentEffects;
import com.summy.enchantment.registry.ModEnchantments;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 附魔书的说明文本。
 *
 * <p>数值全部取当前配置；多级附魔按"该等级的总效果（每级效果）"显示，
 * 这样不同等级的附魔书描述互不相同，也不会让人误以为"数值就是每级的量"。
 */
public final class EnchantmentDescriptions {
	/** 判断"这件是不是正穿在/拿在身上的"时检查的装备槽 */
	private static final EquipmentSlot[] WORN_SLOTS = {
			EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private EnchantmentDescriptions() {
	}

	public static List<Component> describe(ItemStack stack) {
		// 附魔书：不加 [名称等级] 前缀，但保留"忠诚已修正"那一行
		return collect(stack, false);
	}

	/**
	 * 装备（非附魔书）用的描述：不含"忠诚已修正"那一行。
	 *
	 * <p>那行在装备提示里是常显的，不参与"按住 Shift 展开"，所以这里排除掉避免重复。
	 */
	public static List<Component> describeEquipment(ItemStack stack) {
		// 装备：带 [名称等级] 前缀，忠诚那行单独常显
		return collect(stack, true);
	}

	private static List<Component> collect(ItemStack stack, boolean withPrefix) {
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		List<Component> lines = new ArrayList<>();

		for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
			Component line = describeOne(entry.getKey(), Math.max(1, entry.getIntValue()), stack, config, withPrefix);
			if (line != null) {
				lines.add(line);
			}
		}
		return lines;
	}

	private static Component describeOne(Holder<Enchantment> enchantment, int level, ItemStack stack,
			SummyEnchantmentConfig config, boolean withPrefix) {
		if (ModEnchantments.SHARPNESS_PLUS.matches(enchantment)) {
			float per = config.sharpnessDamagePerLevel;
			return key(enchantment, level, withPrefix, "sharpness_plus", number(per * level));
		}
		if (ModEnchantments.SPELLBLADE.matches(enchantment)) {
			float per = config.spellbladeMagicPercentPerLevel;
			return key(enchantment, level, withPrefix, "spellblade", percent(per * level));
		}
		if (ModEnchantments.BEHEADING.matches(enchantment)) {
			float per = config.beheadingHeadChancePerLevel;
			return key(enchantment, level, withPrefix, "beheading", percent(per * level));
		}
		if (ModEnchantments.AUTO_SMELT.matches(enchantment)) {
			return key(enchantment, level, withPrefix, "auto_smelt");
		}
		if (ModEnchantments.ARROW_VELOCITY.matches(enchantment)) {
			float per = config.arrowVelocityBonusPerLevel;
			return key(enchantment, level, withPrefix, "arrow_velocity", percent(per * level));
		}
		if (ModEnchantments.QUICK_DRAW.matches(enchantment)) {
			float per = config.quickDrawSecondsPerLevel;
			return key(enchantment, level, withPrefix, "quick_draw", number(per * level));
		}
		if (ModEnchantments.INSTINCTIVE_RELEASE.matches(enchantment)) {
			return key(enchantment, level, withPrefix, "instinctive_release");
		}
		if (ModEnchantments.LIFE_SHIELD.matches(enchantment)) {
			float per = config.lifeShieldPercentPerLevel;
			return key(enchantment, level, withPrefix, "life_shield", percent(per * level), percent(per * level));
		}
		if (ModEnchantments.HEALTH_BOOST.matches(enchantment)) {
			float per = config.healthBoostPerLevel;
			return key(enchantment, level, withPrefix, "health_boost", number(per * level));
		}
		if (ModEnchantments.LIFESTEAL.matches(enchantment)) {
			float per = config.lifestealPercentPerLevel;
			return key(enchantment, level, withPrefix, "lifesteal", percent(per * level));
		}
		if (ModEnchantments.SCHOLAR.matches(enchantment)) {
			float per = config.scholarBonusPerLevel;
			return key(enchantment, level, withPrefix, "scholar", percent(per * level));
		}
		if (ModEnchantments.CALAMITY.matches(enchantment)) {
			return key(enchantment, level, withPrefix, "calamity");
		}
		if (ModEnchantments.DEATHS_BLESSING.matches(enchantment)) {
			Player player = Minecraft.getInstance().player;
			if (player != null && isWornBy(player, stack)) {
				// 正穿在身上：显示此刻真正生效的数值
				float loss = EnchantmentEffects.healthLossPercent(player);
				return key(enchantment, level, withPrefix, "deaths_blessing.live",
						percent(loss / 100.0F),
						multiplier(EnchantmentEffects.deathsBlessingOutgoingAt(loss)),
						multiplier(EnchantmentEffects.deathsBlessingIncomingAt(loss)));
			}
			// 附魔书等：照旧显示三个系数
			return key(enchantment, level, withPrefix, "deaths_blessing",
					percent(config.deathsBlessingDamageTakenBonus),
					percent(config.deathsBlessingDamageBonusPerPercent),
					percent(config.deathsBlessingResistPerPercent));
		}
		if (ModEnchantments.QUICK_STRIKE.matches(enchantment)) {
			float per = config.quickStrikeBonusPerLevel;
			// 迅捷打击是"加法攻速"，所以显示绝对数值而不是百分比
			return key(enchantment, level, withPrefix, "quick_strike", number(per * level));
		}
		if (ModEnchantments.LIFE_MENDING.matches(enchantment)) {
			float per = config.lifeMendingDurabilityPerLevel;
			return key(enchantment, level, withPrefix, "life_mending", number(per * level));
		}
		if (ModEnchantments.STURDY.matches(enchantment)) {
			if (!withPrefix) {
				// 附魔书没有"最大耐久"，显示等级系数（百分比）而不是具体点数
				float coefficient = Math.max(0.0F, config.sturdyCoefficient - config.sturdyCoefficientStep * (level - 1));
				return key(enchantment, level, withPrefix, "sturdy.ratio", percent(coefficient));
			}
			// 装备：按这件装备的最大耐久算出的具体上限
			int cap = EnchantmentEffects.sturdyCapFor(stack.getMaxDamage(), level);
			return key(enchantment, level, withPrefix, "sturdy", number(cap));
		}
		if (ModEnchantments.SURPRISE.matches(enchantment)) {
			float per = config.surpriseChancePerLevel;
			return key(enchantment, level, withPrefix, "surprise", percent(per * level));
		}
		if (ModEnchantments.PHANTOM_ARROW.matches(enchantment)) {
			float per = config.phantomArrowChancePerLevel;
			return key(enchantment, level, withPrefix, "phantom_arrow", percent(per * level));
		}
		if (ModEnchantments.ENDLESS_QUIVER.matches(enchantment)) {
			return key(enchantment, level, withPrefix, "endless_quiver");
		}
		if (ModEnchantments.EXECUTION.matches(enchantment)) {
			return key(enchantment, level, withPrefix, "execution", percent(config.executionThreshold));
		}
		if (ModEnchantments.QUICK_EAT.matches(enchantment)) {
			float per = config.quickEatReductionPerLevel;
			return key(enchantment, level, withPrefix, "quick_eat", percent(per * level));
		}
		if (ModEnchantments.BIG_STOMACH.matches(enchantment)) {
			float per = config.bigStomachBonusPerLevel;
			return key(enchantment, level, withPrefix, "big_stomach", number(per * level));
		}
		if (ModEnchantments.DEAD_MANS_HEART.matches(enchantment)) {
			return key(enchantment, level, withPrefix, "dead_mans_heart",
					percent(config.deadMansHeartHealthBonus),
					percent(config.deadMansHeartHealingPenalty));
		}
		if (ModEnchantments.REVENGE.matches(enchantment)) {
			// 与效果共用同一处计算，避免"写出来的"和"算出来的"再次对不上
			return key(enchantment, level, withPrefix, "revenge", percent(EnchantmentEffects.revengeBonus(level)));
		}
		if (ModEnchantments.SOUL_GRACE.matches(enchantment)) {
			return key(enchantment, level, withPrefix, "soul_grace", config.soulGraceCooldownSeconds);
		}
		if (ModEnchantments.NIMBLE_STEPS.matches(enchantment)) {
			float per = config.nimbleStepsChancePerLevel;
			return key(enchantment, level, withPrefix, "nimble_steps", percent(per * level));
		}
		// 1.3.7 三个箭矢附魔
		if (ModEnchantments.LIGHTNESS.matches(enchantment)) {
			double reduction = Math.min(0.9D, config.lightnessGravityReductionPerLevel * level);
			return key(enchantment, level, withPrefix, "lightness", percent((float) reduction));
		}
		if (ModEnchantments.SKY_WISH.matches(enchantment)) {
			return key(enchantment, level, withPrefix, "sky_wish",
					percent(config.skyWishBonusPerTick), percent(config.skyWishMaxBonus));
		}
		if (ModEnchantments.DEATH_EYE.matches(enchantment)) {
			return key(enchantment, level, withPrefix, "death_eye",
					percent(config.deathEyeDamagePerStack), config.deathEyeMaxStacks);
		}
		// 1.3.8 三个"赋能"附魔
		if (ModEnchantments.PHASE_STEP.matches(enchantment)) {
			return key(enchantment, level, withPrefix, "phase_step");
		}
		if (ModEnchantments.HIDDEN_BLADE.matches(enchantment)) {
			return key(enchantment, level, withPrefix, "hidden_blade",
					percent(config.hiddenBladeBonusPerTick), percent(config.hiddenBladeMaxBonus));
		}
		if (ModEnchantments.ELECTRIC_BLADE.matches(enchantment)) {
			return key(enchantment, level, withPrefix, "electric_blade",
					level, percent(config.electricBladeDamageRatio));
		}
		// 原版忠诚被本模组改成了"本体不离开物品栏"：附魔书上补一行说明（装备上那行单独常显）
		if (!withPrefix && enchantment.is(Enchantments.LOYALTY) && config.enableLoyaltyRework) {
			return plain("loyalty_rework");
		}
		return null;
	}

	/**
	 * [附魔名(+罗马等级)] + 描述文本（不带句号）。
	 *
	 * @param withPrefix 只有装备上的描述才加前缀；附魔书不加
	 */
	private static Component key(Holder<Enchantment> enchantment, int level, boolean withPrefix,
			String id, Object... args) {
		MutableComponent text = Component.empty();
		if (withPrefix) {
			text.append(Component.literal("[")).append(enchantment.value().description());
			// 上限只有 1 级的附魔不标等级
			if (enchantment.value().getMaxLevel() > 1) {
				text.append(Component.translatable("enchantment.level." + level));
			}
			text.append(Component.literal("]"));
		}
		return text.append(Component.translatable("tooltip.summy_enchantment.desc." + id, args));
	}

	/** 不带附魔名前缀的说明行（忠诚那行用） */
	private static Component plain(String id, Object... args) {
		return Component.translatable("tooltip.summy_enchantment.desc." + id, args);
	}

	/** 这件物品是否正被玩家穿在身上/拿在手上（用它来决定显示实时值还是系数文案） */
	private static boolean isWornBy(Player player, ItemStack stack) {
		for (EquipmentSlot slot : WORN_SLOTS) {
			ItemStack worn = player.getItemBySlot(slot);
			if (!worn.isEmpty() && ItemStack.matches(worn, stack)) {
				return true;
			}
		}
		return false;
	}

	/** 倍率显示：至少保留一位小数（1.0 / 1.12 / 1.185） */
	private static String multiplier(float value) {
		String text = String.format(Locale.ROOT, "%.3f", value);
		while (text.length() > 3 && text.endsWith("0")) {
			text = text.substring(0, text.length() - 1);
		}
		return text;
	}

	/** 0.08 → "8%"（整数不带小数点） */
	private static String percent(float ratio) {
		double value = ratio * 100.0D;
		if (Math.abs(value - Math.round(value)) < 1.0E-3D) {
			return Math.round(value) + "%";
		}
		return String.format(Locale.ROOT, "%.1f%%", value);
	}

	/** 1.5 → "1.5"（去掉多余的 0） */
	private static String number(float value) {
		String text = String.format(Locale.ROOT, "%.2f", value);
		while (text.endsWith("0")) {
			text = text.substring(0, text.length() - 1);
		}
		if (text.endsWith(".")) {
			text = text.substring(0, text.length() - 1);
		}
		return text;
	}
}
