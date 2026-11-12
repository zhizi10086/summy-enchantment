package com.summy.enchantment.registry;

import com.summy.enchantment.SummyEnchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

/**
 * 本模组的创造模式物品页：只放自定义附魔书。
 */
public final class ModItemGroups {
	public static final ItemGroup ENCHANTED_BOOKS = new ItemGroup.Builder(null, -1)
			.icon(() -> new ItemStack(Items.ENCHANTED_BOOK))
			.displayName(Text.translatable("itemGroup.summy_enchantment.enchanted_books"))
			.entries((context, entries) -> {
				var enchantments = context.lookup().getWrapperOrThrow(RegistryKeys.ENCHANTMENT);
				for (EnchantmentRef ref : ModEnchantments.all()) {
					var entry = enchantments.getOptional(ref.key());
					entry.ifPresent(value -> entries.add(EnchantedBookItem.forEnchantment(
							new EnchantmentLevelEntry(value, value.value().getMaxLevel()))));
				}
			})
			.build();

	private ModItemGroups() {
	}

	public static void register() {
		Registry.register(Registries.ITEM_GROUP, SummyEnchantment.id("enchanted_books"), ENCHANTED_BOOKS);
	}
}
