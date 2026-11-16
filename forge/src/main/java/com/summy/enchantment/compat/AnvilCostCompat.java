package com.summy.enchantment.compat;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 铁砧成本所有权检测。
 *
 * <p>Apotheosis 的 AnvilMenuMixin 与 SE 的铁砧 Mixin 对同一个扣费调用和
 * createResult 常量进行不兼容的改造。只要 Apotheosis 存在，SE 就整体停用
 * 经验点成本、过于昂贵修改和客户端点数显示，由 Apotheosis 独占该流程。
 */
public final class AnvilCostCompat {
	private static final String APOTHESIS_MOD_ID = "apotheosis";
	private static final Logger LOGGER = LoggerFactory.getLogger("summy_enchantment_anvil_compat");

	private static volatile Boolean apotheosisLoaded;
	private static boolean takeoverLogged;

	private AnvilCostCompat() {
	}

	/** Apotheosis 是否加载；结果缓存，后续查询不会反复扫描 mod 列表。 */
	public static boolean isApotheosisLoaded() {
		Boolean current = apotheosisLoaded;
		if (current == null) {
			synchronized (AnvilCostCompat.class) {
				current = apotheosisLoaded;
				if (current == null) {
					current = detectApotheosis();
					apotheosisLoaded = current;
					if (current) {
						logTakeover();
					}
				}
			}
		}
		return current;
	}

	private static boolean detectApotheosis() {
		try {
			if (FMLLoader.getLoadingModList().getModFileById(APOTHESIS_MOD_ID) != null) {
				return true;
			}
		} catch (Throwable ignored) {
			// 加载早期可能尚未建立列表，继续尝试 ModList。
		}
		try {
			ModList modList = ModList.get();
			return modList != null && modList.isLoaded(APOTHESIS_MOD_ID);
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static void logTakeover() {
		if (!takeoverLogged) {
			takeoverLogged = true;
			LOGGER.info("[SE] 检测到 Apotheosis，已停用 SE 铁砧经验点成本与客户端点数显示；铁砧由 Apotheosis 接管。");
		}
	}
}
