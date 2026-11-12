package com.summy.enchantment.network;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * 公共代码与客户端网络实现之间的类加载隔离桥。
 */
public final class ClientNetworkCallbacks {
	private static volatile Consumer<String> configHandler = ignored -> {
	};
	private static volatile IntConsumer particleRequestHandler = ignored -> {
	};

	private ClientNetworkCallbacks() {
	}

	public static void setConfigHandler(Consumer<String> handler) {
		configHandler = handler == null ? ignored -> {
		} : handler;
	}

	public static void setParticleRequestHandler(IntConsumer handler) {
		particleRequestHandler = handler == null ? ignored -> {
		} : handler;
	}

	public static void onServerConfig(String json) {
		configHandler.accept(json);
	}

	public static void onParticleRequest(int protocolVersion) {
		particleRequestHandler.accept(protocolVersion);
	}
}
