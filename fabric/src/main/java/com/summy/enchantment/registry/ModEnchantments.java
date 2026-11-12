package com.summy.enchantment.registry;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;

import java.util.List;

/**
 * 32 个 SE 附魔的稳定注册键。
 *
 * <p>1.21.1 的附魔由数据包定义，本类不再实例化 Enchantment，只维护公共 ID 和查询顺序。
 */
public final class ModEnchantments {
	public static final EnchantmentRef SHARPNESS_PLUS = EnchantmentRef.of("sharpness_plus");
	public static final EnchantmentRef SPELLBLADE = EnchantmentRef.of("spellblade");
	public static final EnchantmentRef BEHEADING = EnchantmentRef.of("beheading");
	public static final EnchantmentRef AUTO_SMELT = EnchantmentRef.of("auto_smelt");
	public static final EnchantmentRef ARROW_VELOCITY = EnchantmentRef.of("arrow_velocity");
	public static final EnchantmentRef QUICK_DRAW = EnchantmentRef.of("quick_draw");
	public static final EnchantmentRef INSTINCTIVE_RELEASE = EnchantmentRef.of("instinctive_release");
	public static final EnchantmentRef LIFE_SHIELD = EnchantmentRef.of("life_shield");
	public static final EnchantmentRef HEALTH_BOOST = EnchantmentRef.of("health_boost");
	public static final EnchantmentRef LIFESTEAL = EnchantmentRef.of("lifesteal");
	public static final EnchantmentRef SCHOLAR = EnchantmentRef.of("scholar");
	public static final EnchantmentRef CALAMITY = EnchantmentRef.of("calamity");
	public static final EnchantmentRef DEATHS_BLESSING = EnchantmentRef.of("deaths_blessing");
	public static final EnchantmentRef QUICK_STRIKE = EnchantmentRef.of("quick_strike");
	public static final EnchantmentRef LIFE_MENDING = EnchantmentRef.of("life_mending");
	public static final EnchantmentRef STURDY = EnchantmentRef.of("sturdy");
	public static final EnchantmentRef SURPRISE = EnchantmentRef.of("surprise");
	public static final EnchantmentRef PHANTOM_ARROW = EnchantmentRef.of("phantom_arrow");
	public static final EnchantmentRef ENDLESS_QUIVER = EnchantmentRef.of("endless_quiver");
	public static final EnchantmentRef EXECUTION = EnchantmentRef.of("execution");
	public static final EnchantmentRef QUICK_EAT = EnchantmentRef.of("quick_eat");
	public static final EnchantmentRef BIG_STOMACH = EnchantmentRef.of("big_stomach");
	public static final EnchantmentRef DEAD_MANS_HEART = EnchantmentRef.of("dead_mans_heart");
	public static final EnchantmentRef REVENGE = EnchantmentRef.of("revenge");
	public static final EnchantmentRef SOUL_GRACE = EnchantmentRef.of("soul_grace");
	public static final EnchantmentRef NIMBLE_STEPS = EnchantmentRef.of("nimble_steps");
	public static final EnchantmentRef LIGHTNESS = EnchantmentRef.of("lightness");
	public static final EnchantmentRef SKY_WISH = EnchantmentRef.of("sky_wish");
	public static final EnchantmentRef DEATH_EYE = EnchantmentRef.of("death_eye");
	public static final EnchantmentRef PHASE_STEP = EnchantmentRef.of("phase_step");
	public static final EnchantmentRef HIDDEN_BLADE = EnchantmentRef.of("hidden_blade");
	public static final EnchantmentRef ELECTRIC_BLADE = EnchantmentRef.of("electric_blade");

	private static final List<EnchantmentRef> ALL = List.of(
			SHARPNESS_PLUS, SPELLBLADE, BEHEADING, AUTO_SMELT, ARROW_VELOCITY, QUICK_DRAW,
			INSTINCTIVE_RELEASE, LIFE_SHIELD, HEALTH_BOOST, LIFESTEAL, SCHOLAR, CALAMITY,
			DEATHS_BLESSING, QUICK_STRIKE, LIFE_MENDING, STURDY, SURPRISE, PHANTOM_ARROW,
			ENDLESS_QUIVER, EXECUTION, QUICK_EAT, BIG_STOMACH, DEAD_MANS_HEART, REVENGE,
			SOUL_GRACE, NIMBLE_STEPS, LIGHTNESS, SKY_WISH, DEATH_EYE, PHASE_STEP,
			HIDDEN_BLADE, ELECTRIC_BLADE
	);

	private ModEnchantments() {
	}

	/** 数据包加载由 Minecraft 动态注册表负责，此处保留无操作入口以兼容启动顺序。 */
	public static void register() {
	}

	public static List<EnchantmentRef> all() {
		return ALL;
	}

	public static boolean isSummyKey(RegistryKey<Enchantment> key) {
		return key != null && "summy_enchantment".equals(key.getValue().getNamespace());
	}
}
