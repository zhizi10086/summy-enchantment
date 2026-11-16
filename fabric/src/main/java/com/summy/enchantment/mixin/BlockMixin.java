package com.summy.enchantment.mixin;

import com.summy.enchantment.config.SummyEnchantmentConfig;
import com.summy.enchantment.registry.ModEnchantments;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动熔炼：把挖掘掉落换成熔炉产物。
 *
 * <p>包装 {@code Block#getDroppedStacks} 的返回列表：命中条件时只替换列表中的物品，
 * 原版 {@code dropStacks} 继续负责生成物品实体和执行方块掉落后处理。
 * 只要掉落物有熔炉配方就替换（没有黑名单），潜行时完全不介入（方便正常采集原矿）。
 */
@Mixin(Block.class)
public abstract class BlockMixin {
	@ModifyReturnValue(
			method = "getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;",
			at = @At("RETURN")
	)
	private static List<ItemStack> summyEnchantment$autoSmelt(
			List<ItemStack> drops,
			@Local(argsOnly = true) ServerWorld world,
			@Local(argsOnly = true) Entity entity,
			@Local(argsOnly = true) ItemStack tool) {
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		if (!config.enableAutoSmelt || ModEnchantments.AUTO_SMELT == null) {
			return drops;
		}
		if (!(entity instanceof PlayerEntity player)) {
			return drops;
		}
		// 潜行时自动关闭，方便正常采集
		if (player.isSneaking()) {
			return drops;
		}
		if (EnchantmentHelper.getLevel(ModEnchantments.AUTO_SMELT, tool) <= 0) {
			return drops;
		}
		if (drops == null || drops.isEmpty()) {
			return drops == null ? List.of() : drops;
		}
		List<ItemStack> transformed = new ArrayList<>(drops.size());
		for (ItemStack drop : drops) {
			ItemStack result = smeltResult(world, drop);
			if (!result.isEmpty()) {
				ItemStack output = result.copy();
				output.setCount(result.getCount() * drop.getCount());
				transformed.add(output);
			} else {
				transformed.add(drop);
			}
		}
		return transformed;
	}

	/** 查询熔炉配方；没有对应配方时返回空 */
	private static ItemStack smeltResult(ServerWorld world, ItemStack input) {
		if (input.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return world.getRecipeManager()
				.getFirstMatch(RecipeType.SMELTING, new SimpleInventory(input), world)
				.filter(recipe -> recipe instanceof AbstractCookingRecipe)
				.map(recipe -> ((AbstractCookingRecipe) recipe).getOutput(world.getRegistryManager()))
				.filter(output -> !output.isEmpty())
				.orElse(ItemStack.EMPTY);
	}
}
