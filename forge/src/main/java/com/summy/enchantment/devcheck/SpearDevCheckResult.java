package com.summy.enchantment.devcheck;

import java.util.List;

/** 单 tick 矛兼容自检结果，不引用 Spears 类型，便于无矛环境安全加载主自检类。 */
public record SpearDevCheckResult(int checks, List<String> failures) {
	public static SpearDevCheckResult idle() {
		return new SpearDevCheckResult(0, List.of());
	}
}
