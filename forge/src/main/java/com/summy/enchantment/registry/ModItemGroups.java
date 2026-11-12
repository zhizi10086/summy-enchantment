package com.summy.enchantment.registry;

import com.summy.enchantment.SummyEnchantment;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 本模组的创造模式物品页：只放自定义附魔书。
 */
public final class ModItemGroups {
	public static final CreativeModeTab ENCHANTED_BOOKS = CreativeModeTab.builder()
			.icon(() -> new ItemStack(Items.ENCHANTED_BOOK))
			.title(Component.translatable("itemGroup.summy_enchantment.enchanted_books"))
			.displayItems((params, output) -> {
				var enchantments = params.holders().lookupOrThrow(Registries.ENCHANTMENT);
				for (EnchantmentRef ref : ModEnchantments.all()) {
					enchantments.get(ref.key()).ifPresent(holder ->
							output.accept(EnchantedBookItem.createForEnchantment(
									new EnchantmentInstance(holder, holder.value().getMaxLevel()))));
				}
			})
			.build();

	private ModItemGroups() {
	}

	public static void register(IEventBus bus) {
		bus.addListener(ModItemGroups::onRegister);
	}

	private static void onRegister(RegisterEvent event) {
		if (!Registries.CREATIVE_MODE_TAB.equals(event.getRegistryKey())) {
			return;
		}
		event.register(Registries.CREATIVE_MODE_TAB, SummyEnchantment.id("enchanted_books"),
				() -> ENCHANTED_BOOKS);
	}
}
