package com.summy.enchantment.compat.kubejs;

import com.summy.enchantment.SummyEnchantment;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;

/**
 * KubeJS 可选适配插件。
 *
 * <p>KubeJS 通过 jar 内的 {@code kubejs.plugins.txt} 发现本类；未安装 KubeJS 时该文件不会被读取，
 * 因此不会触发任何 KubeJS 类加载，也不会形成硬依赖。
 */
public final class SummyEnchantmentKubeJSPlugin extends KubeJSPlugin {
	private static boolean logged;

	@Override
	public void init() {
		SummyEnchantmentKubeJSEvents.installHooks();
		if (!logged) {
			logged = true;
			SummyEnchantment.LOGGER.info("[Summy Enchantment] KubeJS API 已绑定：SummyEnchantment");
		}
	}

	@Override
	public void registerEvents() {
		SummyEnchantmentKubeJSEvents.GROUP.register();
	}

	@Override
	public void registerBindings(BindingsEvent event) {
		if (event.getType() == ScriptType.SERVER) {
			event.add("SummyEnchantment", SummyEnchantmentBindings.class);
		} else if (event.getType() == ScriptType.CLIENT) {
			event.add("SummyEnchantment", SummyEnchantmentClientBindings.class);
		}
	}

	@Override
	public void registerClasses(ScriptType type, ClassFilter filter) {
		// 默认封锁本模组所有内部类，只给服务端脚本精确放行公共 API 类。
		// KubeJS 的 ClassFilter 默认允许，因此不能只写"客户端 deny"。
		filter.deny("com.summy.enchantment.");
		if (type == ScriptType.SERVER) {
			filter.allow("com.summy.enchantment.api.SummyEnchantmentApi");
		}
	}
}
