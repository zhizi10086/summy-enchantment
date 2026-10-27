package com.summy.enchantment.api;

import com.summy.enchantment.SummyEnchantment;
import com.summy.enchantment.combat.EnchantmentEffects;
import com.summy.enchantment.compat.SummyReliquaryCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Summy Enchantment 的公开只读/安全写入 API。
 *
 * <p>这个类不依赖 KubeJS；KubeJS 插件只是把这里的静态方法绑定为脚本全局对象。
 * 所有附魔 ID 支持完整 id 或省略 {@code summy_enchantment:} 前缀的短名。
 */
public final class SummyEnchantmentApi {
	private SummyEnchantmentApi() {
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
		if (entity != null) {
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
		if (attacker == null || weapon == null || weapon.isEmpty()) {
			return 0.0F;
		}
		return EnchantmentEffects.applyLifestealReport(attacker, weapon, dealtDamage);
	}

	/** 当前近战乘区：力量 × 复仇；死神祝福造成侧仍在受伤侧结算 */
	public static float meleeMultiplier(Player player) {
		return player == null ? 1.0F : SummyReliquaryCompat.meleeMultiplier(player);
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
