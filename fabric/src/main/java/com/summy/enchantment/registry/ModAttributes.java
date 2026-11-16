package com.summy.enchantment.registry;

import com.summy.enchantment.SummyEnchantment;
import com.summy.enchantment.mixin.AttributeContainerAccessor;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * 自定义玩家属性。
 *
 * <p>生命护盾的数值存在这里，不再借用状态效果的 amplifier：
 * 属性值随玩家 NBT 持久化、可用 {@code /attribute} 查询，也不会在效果栏占一格图标。
 *
 * <p>刻意<b>不</b>调用 {@code setTracked(true)}：客户端不需要这个值（黄心来自原版同步的
 * 伤害吸收值），不参与属性同步就少一份网络负担。
 */
public final class ModAttributes {
	/** 护盾值上限：最大生命 × 20%/级 × 5 级 加上生命提升/死者之心也远小于这个数 */
	public static final double LIFE_SHIELD_MAX = 2048.0D;

	public static EntityAttribute LIFE_SHIELD;

	private ModAttributes() {
	}

	public static void register() {
		LIFE_SHIELD = Registry.register(
				Registries.ATTRIBUTE,
				SummyEnchantment.id("life_shield"),
				new ClampedEntityAttribute("attribute.name.summy_enchantment.life_shield",
						0.0D, 0.0D, LIFE_SHIELD_MAX)
		);
	}

	/**
	 * 给玩家属性容器追加生命护盾；已有时保持原值，不覆盖任何第三方属性。
	 *
	 * <p>Fabric 的默认属性注册接口会整份替换玩家属性表，因此 SE 改为在玩家构造末尾
	 * 直接补入自己的 custom 属性。构造器 TAIL 早于玩家 NBT 反序列化，已保存的护盾值
	 * 仍会正常写回该属性实例。
	 *
	 * @return 本次是否新增了属性实例
	 */
	public static boolean ensureLifeShieldAttribute(AttributeContainer attributes) {
		if (LIFE_SHIELD == null || attributes == null || attributes.hasAttribute(LIFE_SHIELD)) {
			return false;
		}
		EntityAttributeInstance instance = new EntityAttributeInstance(LIFE_SHIELD, ignored -> {
		});
		((AttributeContainerAccessor) (Object) attributes)
				.summyEnchantment$getCustomAttributes()
				.put(LIFE_SHIELD, instance);
		return true;
	}
}
