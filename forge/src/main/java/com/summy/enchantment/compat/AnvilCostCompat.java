package com.summy.enchantment.compat;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 铁砧成本所有权检测。
 *
 * <p>Apothic Enchanting 的 AnvilMenuMixin 与 SE 的铁砧 Mixin 对同一个扣费调用和
 * createResult 常量进行不兼容的改造。只要 Apothic Enchanting 存在，SE 就整体停用
 * 经验点成本、过于昂贵修改和客户端点数显示，由 Apothic Enchanting 独占该流程。
 */
public final class AnvilCostCompat {
	private static final String APOTHIC_ENCHANTING_MOD_ID = "apothic_enchanting";
	private static final Logger LOGGER = LoggerFactory.getLogger("summy_enchantment_anvil_compat");

	private static volatile Boolean apothicEnchantingLoaded;
	private static boolean takeoverLogged;

	private AnvilCostCompat() {
	}

	/** Apothic Enchanting 是否加载；结果缓存，后续查询不会反复扫描 mod 列表。 */
	public static boolean isApothicEnchantingLoaded() {
		Boolean current = apothicEnchantingLoaded;
		if (current == null) {
			synchronized (AnvilCostCompat.class) {
				current = apothicEnchantingLoaded;
				if (current == null) {
					current = detectApothicEnchanting();
					apothicEnchantingLoaded = current;
					if (current) {
						logTakeover();
					}
				}
			}
		}
		return current;
	}

	private static boolean detectApothicEnchanting() {
		try {
			if (FMLLoader.getLoadingModList().getModFileById(APOTHIC_ENCHANTING_MOD_ID) != null) {
				return true;
			}
		} catch (Throwable ignored) {
			// 加载早期可能尚未建立列表，继续尝试 ModList。
		}
		try {
			ModList modList = ModList.get();
			return modList != null && modList.isLoaded(APOTHIC_ENCHANTING_MOD_ID);
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static void logTakeover() {
		if (!takeoverLogged) {
			takeoverLogged = true;
			LOGGER.info("[SE] 检测到 Apothic Enchanting，已停用 SE 铁砧经验点成本与客户端点数显示；铁砧由 Apothic Enchanting 接管。");
		}
	}
}
