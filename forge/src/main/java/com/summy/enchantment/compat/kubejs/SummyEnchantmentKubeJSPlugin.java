package com.summy.enchantment.compat.kubejs;

import com.summy.enchantment.SummyEnchantment;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptType;

/**
 * KubeJS 可选适配插件。
 *
 * <p>KubeJS 通过 jar 内的 {@code kubejs.plugins.txt} 发现本类；未安装 KubeJS 时该文件不会被读取，
 * 因此不会触发任何 KubeJS 类加载，也不会形成硬依赖。
 */
public final class SummyEnchantmentKubeJSPlugin implements KubeJSPlugin {
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
	public void registerEvents(EventGroupRegistry event) {
		event.register(SummyEnchantmentKubeJSEvents.GROUP);
	}

	@Override
	public void registerBindings(BindingRegistry event) {
		if (event.type() == ScriptType.SERVER) {
			event.add("SummyEnchantment", SummyEnchantmentBindings.class);
		} else if (event.type() == ScriptType.CLIENT) {
			event.add("SummyEnchantment", SummyEnchantmentClientBindings.class);
		}
	}

	@Override
	public void registerClasses(ClassFilter filter) {
		// 默认封锁本模组所有内部类，只给服务端脚本精确放行公共 API 类。
		// KubeJS 的 ClassFilter 默认允许，因此不能只写"客户端 deny"。
		filter.deny("com.summy.enchantment.");
		if (filter.scriptType == ScriptType.SERVER) {
			filter.allow("com.summy.enchantment.api.SummyEnchantmentApi");
		}
	}
}
