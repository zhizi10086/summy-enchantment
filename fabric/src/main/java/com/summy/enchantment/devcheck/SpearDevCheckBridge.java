package com.summy.enchantment.devcheck;

import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 核心自检与可选 Spears 专项之间的隔离桥。
 *
 * <p>coreDevcheck 不编译 Spears 类，因此主自检不能直接引用专项实现。
 * 完整开发环境仍可通过反射加载同一专项类。
 */
final class SpearDevCheckBridge {
	private static Method tickMethod;

	private SpearDevCheckBridge() {
	}

	static SpearDevCheckResult tick(ServerPlayerEntity player, int step) {
		try {
			Method method = resolveTickMethod();
			return (SpearDevCheckResult) method.invoke(null, player, step);
		} catch (ClassNotFoundException exception) {
			return new SpearDevCheckResult(1,
					List.of("矛专项不可用：当前构建未编译 SpearFabricDevCheck"));
		} catch (InvocationTargetException exception) {
			return new SpearDevCheckResult(1,
					List.of("矛专项异常：" + exception.getCause()));
		} catch (ReflectiveOperationException exception) {
			return new SpearDevCheckResult(1,
					List.of("矛专项反射失败：" + exception));
		}
	}

	private static Method resolveTickMethod() throws ReflectiveOperationException {
		Method method = tickMethod;
		if (method != null) {
			return method;
		}
		synchronized (SpearDevCheckBridge.class) {
			if (tickMethod == null) {
				tickMethod = Class.forName("com.summy.enchantment.devcheck.SpearFabricDevCheck")
						.getDeclaredMethod("tick", ServerPlayerEntity.class, int.class);
			}
			return tickMethod;
		}
	}
}
