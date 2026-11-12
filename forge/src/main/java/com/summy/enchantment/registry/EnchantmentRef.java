package com.summy.enchantment.registry;

import com.summy.enchantment.SummyEnchantment;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * 1.21 动态附魔注册表中的稳定键引用。
 */
public final class EnchantmentRef {
	/** 集成服务器中客户端与服务端共享静态字段，因此必须分开缓存注册表实例。 */
	private static volatile Registry<Enchantment> serverRegistry;
	private static volatile Registry<Enchantment> clientRegistry;
	private final ResourceKey<Enchantment> key;

	private EnchantmentRef(ResourceKey<Enchantment> key) {
		this.key = key;
	}

	public static EnchantmentRef of(String path) {
		return new EnchantmentRef(ResourceKey.create(Registries.ENCHANTMENT, SummyEnchantment.id(path)));
	}

	public ResourceKey<Enchantment> key() {
		return this.key;
	}

	public ResourceLocation id() {
		return this.key.location();
	}

	public int level(ItemStack stack) {
		return level(stack, this.key);
	}

	public boolean matches(Holder<Enchantment> entry) {
		return entry != null && entry.is(this.key);
	}

	public Holder<Enchantment> entry() {
		Registry<Enchantment> registry = serverRegistry != null ? serverRegistry : clientRegistry;
		return registry == null ? null : registry.getHolder(this.key).orElse(null);
	}

	public Enchantment value() {
		Holder<Enchantment> entry = entry();
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

	public static boolean valueMatches(Enchantment value, ResourceKey<Enchantment> key) {
		return valueMatchesIn(value, key, serverRegistry)
				|| valueMatchesIn(value, key, clientRegistry);
	}

	private static boolean valueMatchesIn(Enchantment value, ResourceKey<Enchantment> key,
			Registry<Enchantment> registry) {
		return value != null && key != null && registry != null
				&& registry.getResourceKey(value).map(key::equals).orElse(false);
	}

	public static int level(ItemStack stack, ResourceKey<Enchantment> key) {
		if (stack == null || stack.isEmpty() || key == null) {
			return 0;
		}
		for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
			if (entry.getKey().is(key)) {
				return entry.getIntValue();
			}
		}
		return 0;
	}
}
