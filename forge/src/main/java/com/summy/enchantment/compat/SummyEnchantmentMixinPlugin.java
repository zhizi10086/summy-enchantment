package com.summy.enchantment.compat;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * SE 主 Mixin 配置插件。
 *
 * <p>只处理 Apothic Enchanting 铁砧成本冲突；未安装 Apothic Enchanting 时全部 Mixin 保持
 * 原有 required 行为，不会因为兼容退让而降低目标校验强度。
 */
public final class SummyEnchantmentMixinPlugin implements IMixinConfigPlugin {
	private static final Set<String> APOTHESIS_OWNED_ANVIL_MIXINS = Set.of(
			"com.summy.enchantment.mixin.AnvilScreenHandlerMixin",
			"com.summy.enchantment.mixin.AnvilScreenMixin"
	);

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return !AnvilCostCompat.isApothicEnchantingLoaded()
				|| !APOTHESIS_OWNED_ANVIL_MIXINS.contains(mixinClassName);
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
