package com.summy.enchantment.api;

import com.summy.enchantment.SummyEnchantment;
import com.summy.enchantment.combat.ArrowTraits;
import com.summy.enchantment.combat.EnchantmentEffects;
import com.summy.enchantment.compat.SummyReliquaryCompat;
import com.summy.enchantment.config.SummyEnchantmentConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Summy Enchantment 的公开只读/安全写入 API。
 *
 * <p>这个类不依赖 KubeJS；KubeJS 插件只是把这里的静态方法绑定为脚本全局对象。
 * 所有附魔 ID 支持完整 id 或省略 {@code summy_enchantment:} 前缀的短名。
 */
public final class SummyEnchantmentApi {
	/** KubeJS/Java 公共接口版本；破坏性变更时才递增。 */
	private static final int API_VERSION = 1;
	/** 配置键只读索引：避免脚本高频查询时反复遍历 99 项枚举。 */
	private static final Map<String, SummyEnchantmentConfig.Option> CONFIG_OPTIONS = buildConfigOptions();
	private static final List<String> CONFIG_KEYS = buildConfigKeys();

	private SummyEnchantmentApi() {
	}

	/** 当前公共接口版本。 */
	public static int apiVersion() {
		return API_VERSION;
	}

	/** 读取附魔等级；未知 ID、空物品或非法 id 返回 0 */
	public static int level(ItemStack stack, String id) {
		return level(stack, parseId(id));
	}

	/** 读取附魔等级；未知附魔返回 0 */
	public static int level(ItemStack stack, ResourceLocation id) {
		if (stack == null || stack.isEmpty() || id == null) {
			return 0;
		}
		Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
		return enchantment == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
	}

	public static boolean has(ItemStack stack, String id) {
		return level(stack, id) > 0;
	}

	public static boolean has(ItemStack stack, ResourceLocation id) {
		return level(stack, id) > 0;
	}

	public static float lifeShield(LivingEntity entity) {
		return entity == null ? 0.0F : EnchantmentEffects.lifeShield(entity);
	}

	public static float lifeShieldCap(LivingEntity entity) {
		// 兼容退让期间有效上限为 0，避免脚本绕过 Celestial 优先规则。
		return EnchantmentEffects.lifeShieldCap(entity);
	}

	/**
	 * 直接增加护盾点数（不是治疗量），仅服务端生效。
	 *
	 * @return 实际增加量；无生命护盾胸甲、Celestial 同功能附魔存在、开关关闭、客户端或 amount 非法时返回 0
	 */
	public static float addLifeShield(LivingEntity entity, float amount) {
		return EnchantmentEffects.addLifeShield(entity, amount);
	}

	public static void clearLifeShield(LivingEntity entity) {
		if (entity != null && !entity.level().isClientSide) {
			EnchantmentEffects.clearLifeShield(entity);
		}
	}

	/** 只预览嗜血应回复量，不执行治疗 */
	public static float lifestealHeal(ItemStack weapon, float dealtDamage) {
		if (weapon == null || weapon.isEmpty()) {
			return 0.0F;
		}
		return EnchantmentEffects.lifestealHeal(weapon, dealtDamage);
	}

	/**
	 * 按现有嗜血管线执行治疗。
	 *
	 * <p>只用于尚未经过 SE 近战/三叉戟/SR 管线的自定义伤害；正常命中不要再调用，
	 * 否则会重复结算。返回值是计算出的治疗量，不等于最终生命值增量（生命修补可能消耗它）。
	 */
	public static float applyLifesteal(LivingEntity attacker, ItemStack weapon, float dealtDamage) {
		if (attacker == null || attacker.level().isClientSide || weapon == null || weapon.isEmpty()) {
			return 0.0F;
		}
		return EnchantmentEffects.applyLifestealReport(attacker, weapon, dealtDamage);
	}

	/** 当前近战乘区：力量 × 复仇；死神祝福造成侧仍在受伤侧结算 */
	public static float meleeMultiplier(Player player) {
		return player == null ? 1.0F : SummyReliquaryCompat.meleeMultiplier(player);
	}

	/** 当前已注册的全部 SE 附魔完整 ID，按字母排序。 */
	public static List<String> enchantmentIds() {
		List<String> ids = new ArrayList<>();
		for (ResourceLocation id : ForgeRegistries.ENCHANTMENTS.getKeys()) {
			if (SummyEnchantment.MOD_ID.equals(id.getNamespace())) {
				ids.add(id.toString());
			}
		}
		ids.sort(Comparator.naturalOrder());
		return List.copyOf(ids);
	}

	public static int enchantmentMaxLevel(String id) {
		Enchantment enchantment = enchantment(id);
		return enchantment == null ? 0 : enchantment.getMaxLevel();
	}

	public static String enchantmentRarity(String id) {
		Enchantment enchantment = enchantment(id);
		return enchantment == null ? "" : enchantment.getRarity().name().toLowerCase(Locale.ROOT);
	}

	public static boolean isTreasure(String id) {
		Enchantment enchantment = enchantment(id);
		return enchantment != null && enchantment.isTreasureOnly();
	}

	public static boolean isCursed(String id) {
		Enchantment enchantment = enchantment(id);
		return enchantment != null && enchantment.isCurse();
	}

	/** 按附魔自身的 isAcceptableItem 判断物品能否附魔；空物品或未知 ID 返回 false。 */
	public static boolean enchantmentCanApplyTo(ItemStack stack, String id) {
		Enchantment enchantment = enchantment(id);
		return stack != null && !stack.isEmpty() && enchantment != null && enchantment.canEnchant(stack);
	}

	/**
	 * 完整检查该附魔能否应用到当前物品：同时检查物品类型和已有附魔的双向互斥。
	 * 不检查等级、经验成本或宝藏获取途径。
	 */
	public static boolean enchantmentCanEnchant(ItemStack stack, String id) {
		Enchantment enchantment = enchantment(id);
		if (stack == null || stack.isEmpty() || enchantment == null || !enchantment.canEnchant(stack)) {
			return false;
		}
		for (Enchantment existing : EnchantmentHelper.getEnchantments(stack).keySet()) {
			if (existing != enchantment
					&& (!enchantment.isCompatibleWith(existing) || !existing.isCompatibleWith(enchantment))) {
				return false;
			}
		}
		return true;
	}

	/** 服务端：死亡之眼当前层数。 */
	public static int deathEyeStacks(Player player) {
		return player == null ? 0 : ArrowTraits.stacks(player.getUUID());
	}

	/** 服务端：玩家当前层数对应的下一次死亡之眼伤害倍率。 */
	public static float deathEyeMultiplier(Player player) {
		if (player == null) {
			return 1.0F;
		}
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		if (!config.enableDeathEye) {
			return 1.0F;
		}
		int stacks = Math.min(deathEyeStacks(player), Math.max(0, config.deathEyeMaxStacks));
		return 1.0F + config.deathEyeDamagePerStack * stacks;
	}

	/** 服务端：箭矢当前轻盈等级。 */
	public static int arrowLightnessLevel(AbstractArrow arrow) {
		return arrow == null ? 0 : ArrowTraits.serverLightness(arrow);
	}

	/** 服务端：这支箭本次命中的死亡之眼倍率。 */
	public static float arrowDeathEyeMultiplier(AbstractArrow arrow) {
		return arrow == null ? 1.0F : ArrowTraits.deathEyeMultiplier(arrow);
	}

	public static int hiddenBladeChargedTicks(LivingEntity entity) {
		return entity == null ? 0 : EnchantmentEffects.hiddenBladeChargedTicks(entity);
	}

	public static float hiddenBladeBonus(LivingEntity entity) {
		return entity == null ? 0.0F : EnchantmentEffects.hiddenBladeBonus(entity);
	}

	public static float hiddenBladeChargeProgress(LivingEntity entity) {
		return entity == null ? 0.0F : EnchantmentEffects.hiddenBladeChargeProgress(entity);
	}

	/** 服务端：藏剑剩余冷却 tick；无冷却、开关关闭或没装时返回 0。 */
	public static int hiddenBladeCooldownRemaining(LivingEntity entity) {
		return entity == null ? 0 : EnchantmentEffects.hiddenBladeCooldownRemaining(entity);
	}

	public static float deathsBlessingOutgoing(LivingEntity entity) {
		return entity == null ? 1.0F : EnchantmentEffects.deathsBlessingOutgoing(entity);
	}

	public static float deathsBlessingIncoming(LivingEntity entity) {
		return entity == null ? 1.0F : EnchantmentEffects.deathsBlessingIncoming(entity);
	}

	public static float revengeMultiplier(LivingEntity entity) {
		return entity == null ? 1.0F : EnchantmentEffects.revengeMultiplier(entity);
	}

	public static float arrowSpeedMultiplier(ItemStack weapon) {
		return weapon == null || weapon.isEmpty() ? 1.0F : EnchantmentEffects.arrowSpeedMultiplier(weapon);
	}

	public static float undyingGraceReduction(LivingEntity entity) {
		return entity == null ? 0.0F : EnchantmentEffects.undyingGraceReduction(entity);
	}

	public static int foodLevelCap(LivingEntity entity) {
		return entity == null ? 20 : EnchantmentEffects.foodLevelCap(entity);
	}

	public static int sturdyCap(ItemStack stack) {
		return stack == null || stack.isEmpty() ? 0 : EnchantmentEffects.sturdyCap(stack);
	}

	/** 当前配置对象中的全部公开配置键，按字母排序。 */
	public static List<String> configKeys() {
		return CONFIG_KEYS;
	}

	private static List<String> buildConfigKeys() {
		List<String> keys = new ArrayList<>();
		for (SummyEnchantmentConfig.Option option : SummyEnchantmentConfig.Option.values()) {
			keys.add(option.key());
		}
		keys.sort(Comparator.naturalOrder());
		return List.copyOf(keys);
	}

	/** 读取当前逻辑端的配置值；布尔项返回 1.0/0.0，未知键返回 NaN。 */
	public static double configValue(String key) {
		SummyEnchantmentConfig.Option option = configOption(key);
		return option == null ? Double.NaN : option.get(SummyEnchantmentConfig.get());
	}

	/** 布尔配置的安全读取；未知键返回 false，true/false 分别映射为 1.0/0.0。 */
	public static boolean configBoolean(String key) {
		SummyEnchantmentConfig.Option option = configOption(key);
		// 只有真正的布尔项才允许转成 boolean；数值项即使恰好大于等于 0.5 也不能误报为 true。
		return option != null
				&& option.kind() == SummyEnchantmentConfig.Option.Kind.BOOLEAN
				&& option.get(SummyEnchantmentConfig.get()) >= 0.5D;
	}

	/** 配置默认值；未知键返回 NaN。 */
	public static double configDefault(String key) {
		SummyEnchantmentConfig.Option option = configOption(key);
		return option == null ? Double.NaN : option.defaultValue();
	}

	public static double configMin(String key) {
		SummyEnchantmentConfig.Option option = configOption(key);
		return option == null ? Double.NaN : option.min();
	}

	public static double configMax(String key) {
		SummyEnchantmentConfig.Option option = configOption(key);
		return option == null ? Double.NaN : option.max();
	}

	public static double configStep(String key) {
		SummyEnchantmentConfig.Option option = configOption(key);
		return option == null ? Double.NaN : option.step();
	}

	public static boolean hasConfig(String key) {
		return configOption(key) != null;
	}

	public static String configScope(String key) {
		SummyEnchantmentConfig.Option option = configOption(key);
		return option == null ? "" : option.clientOnly() ? "client" : "server";
	}

	public static String configKind(String key) {
		SummyEnchantmentConfig.Option option = configOption(key);
		return option == null ? "" : option.kind().name().toLowerCase(Locale.ROOT);
	}

	public static String configCategory(String key) {
		SummyEnchantmentConfig.Option option = configOption(key);
		return option == null ? "" : option.category().name().toLowerCase(Locale.ROOT);
	}

	private static Enchantment enchantment(String id) {
		ResourceLocation parsed = parseId(id);
		return parsed == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(parsed);
	}

	private static SummyEnchantmentConfig.Option configOption(String key) {
		if (key == null || key.isBlank()) {
			return null;
		}
		return CONFIG_OPTIONS.get(key.trim());
	}

	private static Map<String, SummyEnchantmentConfig.Option> buildConfigOptions() {
		Map<String, SummyEnchantmentConfig.Option> options = new HashMap<>();
		for (SummyEnchantmentConfig.Option option : SummyEnchantmentConfig.Option.values()) {
			options.put(option.key(), option);
		}
		return Map.copyOf(options);
	}

	/** 把短名规范成 SE 命名空间；完整 id 原样保留 */
	public static ResourceLocation parseId(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		String normalized = id.trim();
		if (!normalized.contains(":")) {
			normalized = SummyEnchantment.MOD_ID + ":" + normalized;
		}
		return ResourceLocation.tryParse(normalized);
	}
}
