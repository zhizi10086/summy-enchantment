package com.summy.enchantment.mixin;

import com.summy.enchantment.SummyEnchantmentRegistration;
import net.minecraft.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 Minecraft 冻结注册表前完成 SE 的静态注册。
 */
@Mixin(Bootstrap.class)
public abstract class BootstrapMixin {
	@Inject(
			method = "initialize",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/registry/Registries;bootstrap()V",
					shift = At.Shift.BEFORE
			)
	)
	private static void summyEnchantment$registerBeforeBootstrap(CallbackInfo ci) {
		SummyEnchantmentRegistration.register();
	}
}
