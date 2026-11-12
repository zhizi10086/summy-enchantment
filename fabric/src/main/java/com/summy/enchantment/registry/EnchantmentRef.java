package com.summy.enchantment.registry;

import com.summy.enchantment.SummyEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

/**
 * 1.21 动态附魔注册表中的稳定键引用。
 *
 * <p>附魔本体由数据包加载，核心代码只持有 {@link RegistryKey}，避免在模组初始化阶段
 * 依赖尚未创建的世界注册表。等级查询直接匹配物品组件中的 RegistryEntry。
 */
public final class EnchantmentRef {
	/** 集成服务器中客户端与服务端共享静态字段，因此必须分开缓存注册表实例。 */
	private static volatile Registry<Enchantment> serverRegistry;
	private static volatile Registry<Enchantment> clientRegistry;
	private final RegistryKey<Enchantment> key;

	private EnchantmentRef(RegistryKey<Enchantment> key) {
		this.key = key;
	}

	public static EnchantmentRef of(String path) {
		return new EnchantmentRef(RegistryKey.of(RegistryKeys.ENCHANTMENT, SummyEnchantment.id(path)));
	}

	public RegistryKey<Enchantment> key() {
		return this.key;
	}

	public Identifier id() {
		return this.key.getValue();
	}

	public int level(ItemStack stack) {
		return level(stack, this.key);
	}

	public boolean matches(net.minecraft.registry.entry.RegistryEntry<Enchantment> entry) {
		return entry != null && entry.matchesKey(this.key);
	}

	public RegistryEntry<Enchantment> entry() {
		Registry<Enchantment> registry = serverRegistry != null ? serverRegistry : clientRegistry;
		return registry == null ? null : registry.getEntry(this.key).orElse(null);
	}

	public Enchantment value() {
		RegistryEntry<Enchantment> entry = entry();
		return entry == null ? null : entry.value();
	}

	public boolean matchesValue(Enchantment value) {
		return valueMatches(value, this.key);
	}

	public static void bindServerRegistry(Registry<Enchantment> registry) {
		serverRegistry = registry;
	}

	public static void bindClientRegistry(Registry<Enchantment> registry) {
		clientRegistry = registry;
	}

	public static boolean valueMatches(Enchantment value, RegistryKey<Enchantment> key) {
		return valueMatchesIn(value, key, serverRegistry)
				|| valueMatchesIn(value, key, clientRegistry);
	}

	private static boolean valueMatchesIn(Enchantment value, RegistryKey<Enchantment> key,
			Registry<Enchantment> registry) {
		return value != null && key != null && registry != null
				&& registry.getKey(value).map(key::equals).orElse(false);
	}

	public static int level(ItemStack stack, RegistryKey<Enchantment> key) {
		if (stack == null || stack.isEmpty() || key == null) {
			return 0;
		}
		for (var entry : EnchantmentHelper.getEnchantments(stack).getEnchantmentEntries()) {
			if (entry.getKey().matchesKey(key)) {
				return entry.getIntValue();
			}
		}
		return 0;
	}
}
