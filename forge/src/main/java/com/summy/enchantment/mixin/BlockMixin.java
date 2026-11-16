package com.summy.enchantment.mixin;

import com.summy.enchantment.config.SummyEnchantmentConfig;
import com.summy.enchantment.registry.ModEnchantments;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动熔炼：把挖掘掉落换成熔炉产物。
 *
 * <p>包装 {@code Block#getDrops} 的返回列表：命中条件时只替换列表中的物品，
 * 原版 {@code dropResources} 继续负责生成物品实体和执行方块掉落后处理。
 * 只要掉落物有熔炉配方就替换（没有黑名单），潜行时完全不介入（方便正常采集原矿）。
 */
@Mixin(Block.class)
public abstract class BlockMixin {
	@ModifyReturnValue(
			method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
			at = @At("RETURN")
	)
	private static List<ItemStack> summyEnchantment$autoSmelt(
			List<ItemStack> drops,
			@Local(argsOnly = true) ServerLevel world,
			@Local(argsOnly = true) Entity entity,
			@Local(argsOnly = true) ItemStack tool) {
		SummyEnchantmentConfig config = SummyEnchantmentConfig.get();
		if (!config.enableAutoSmelt || ModEnchantments.AUTO_SMELT == null) {
			return drops;
		}
		if (!(entity instanceof Player player)) {
			return drops;
		}
		// 潜行时自动关闭，方便正常采集
		if (player.isShiftKeyDown()) {
			return drops;
		}
		if (ModEnchantments.AUTO_SMELT.level(tool) <= 0) {
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
	private static ItemStack smeltResult(ServerLevel world, ItemStack input) {
		if (input.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return world.getRecipeManager()
				.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), world)
				.map(recipe -> recipe.value().getResultItem(world.registryAccess()))
				.filter(output -> !output.isEmpty())
				.orElse(ItemStack.EMPTY);
	}
}
