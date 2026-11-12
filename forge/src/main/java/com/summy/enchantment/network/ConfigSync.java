package com.summy.enchantment.network;

import com.summy.enchantment.SummyEnchantment;
import com.summy.enchantment.combat.ArrowTraits;
import com.summy.enchantment.config.DodgeSoundStyle;
import com.summy.enchantment.config.SummyEnchantmentConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NeoForge 1.21.1 强类型 payload 网络层。
 */
public final class ConfigSync {
	public static final int PARTICLE_PROTOCOL_VERSION = 9;
	public static final int LONG_DISTANCE_PROTOCOL_VERSION = 2;
	private static final int REQUEST_INTERVAL_TICKS = 100;
	private static final int MAX_REQUEST_ATTEMPTS = 6;
	private static final Map<UUID, int[]> REQUEST_STATE = new ConcurrentHashMap<>();

	private ConfigSync() {
	}

	public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(Integer.toString(PARTICLE_PROTOCOL_VERSION));
		registrar.playToServer(ParticleStylePayload.TYPE, ParticleStylePayload.CODEC,
				(payload, context) -> context.enqueueWork(() -> handleParticleStyle(payload, context)));
		registrar.playToClient(ConfigPayload.TYPE, ConfigPayload.CODEC,
				(payload, context) -> context.enqueueWork(() ->
						ClientNetworkCallbacks.onServerConfig(payload.json())));
		registrar.playToClient(ParticleRequestPayload.TYPE, ParticleRequestPayload.CODEC,
				(payload, context) -> context.enqueueWork(() ->
						ClientNetworkCallbacks.onParticleRequest(payload.protocolVersion())));
		registrar.playToClient(ArrowLightnessPayload.TYPE, ArrowLightnessPayload.CODEC,
				(payload, context) -> context.enqueueWork(() ->
						ArrowTraits.setClientLightness(payload.entityId(), payload.level())));
	}

	/** 保留原入口名，旧调用无需改动。 */
	public static void registerServerReceivers() {
	}

	private static void handleParticleStyle(ParticleStylePayload payload,
			net.neoforged.neoforge.network.handling.IPayloadContext context) {
		if (!(context.player() instanceof ServerPlayer player)) {
			return;
		}
		ParticlePreferences.Preference preference = ParticlePreferences.set(player.getUUID(),
				payload.style(), payload.longDistance(), payload.distance(), payload.meleeStyle(),
				payload.sweepStyle(), payload.meleeEffect(), payload.surpriseStyle(),
				payload.deathStyle(), payload.dodgeSound());
		REQUEST_STATE.remove(player.getUUID());
		if (preference != null && SummyEnchantmentConfig.get().debugLogs) {
			SummyEnchantment.LOGGER.info(
					"[Summy Enchantment] 玩家 {} 的粒子档位 = 箭矢 {} / 近战 {} / 横扫 {} / 强度 {}"
							+ " / 出其不意 {} / 死亡 {} / 闪避音效 {}（远距离={}，半径={}）",
					player.getGameProfile().getName(), preference.style(), preference.meleeStyle(),
					preference.sweepStyle(), preference.meleeEffect(), preference.surpriseStyle(),
					preference.deathStyle(), preference.dodgeSound(),
					preference.longDistance() ? "开" : "关", preference.distance());
		}
	}

	public static void sendArrowLightness(AbstractArrow arrow, int level) {
		if (arrow == null || level <= 0 || !(arrow.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		for (ServerPlayer player : serverLevel.players()) {
			if (player.distanceToSqr(arrow) <= 64.0D * 64.0D) {
				PacketDistributor.sendToPlayer(player, new ArrowLightnessPayload(arrow.getId(), level));
			}
		}
	}

	public static void requestParticlePreference(ServerPlayer player) {
		PacketDistributor.sendToPlayer(player, new ParticleRequestPayload(PARTICLE_PROTOCOL_VERSION));
	}

	public static void tickParticlePreferenceRequests(ServerPlayer player, int serverTicks) {
		UUID playerId = player.getUUID();
		if (ParticlePreferences.isKnown(playerId)) {
			REQUEST_STATE.remove(playerId);
			return;
		}
		int[] state = REQUEST_STATE.computeIfAbsent(playerId, key -> new int[]{-1, 0});
		if (state[1] >= MAX_REQUEST_ATTEMPTS) {
			return;
		}
		if (state[0] >= 0 && serverTicks - state[0] < REQUEST_INTERVAL_TICKS) {
			return;
		}
		requestParticlePreference(player);
		state[0] = serverTicks;
		++state[1];
		if (state[1] == MAX_REQUEST_ATTEMPTS) {
			SummyEnchantment.LOGGER.info(
					"[Summy Enchantment] 未收到玩家 {} 的箭矢粒子偏好，其射出的箭将按观察者设置渲染",
					player.getGameProfile().getName());
		}
	}

	public static void sendTo(ServerPlayer player) {
		PacketDistributor.sendToPlayer(player, new ConfigPayload(SummyEnchantmentConfig.get().toJsonString()));
	}

	public static void broadcast(MinecraftServer server) {
		if (server == null) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			sendTo(player);
		}
	}

	public static void sendParticlePreferenceToServer(int style, boolean longDistance, int distance,
			int meleeStyle, int sweepStyle, int meleeEffect, int surpriseStyle, int deathStyle, int dodgeSound) {
		PacketDistributor.sendToServer(new ParticleStylePayload(style, longDistance, distance, meleeStyle,
				sweepStyle, meleeEffect, surpriseStyle, deathStyle, DodgeSoundStyle.clamp(dodgeSound)));
	}

	public record ParticleStylePayload(int style, boolean longDistance, int distance, int meleeStyle,
			int sweepStyle, int meleeEffect, int surpriseStyle, int deathStyle, int dodgeSound)
			implements CustomPacketPayload {
		public static final Type<ParticleStylePayload> TYPE =
				new Type<>(ResourceLocation.fromNamespaceAndPath(SummyEnchantment.MOD_ID, "particle_style"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ParticleStylePayload> CODEC = StreamCodec.of(
				(buffer, value) -> {
					buffer.writeVarInt(value.style());
					buffer.writeBoolean(value.longDistance());
					buffer.writeVarInt(value.distance());
					buffer.writeVarInt(value.meleeStyle());
					buffer.writeVarInt(value.sweepStyle());
					buffer.writeVarInt(value.meleeEffect());
					buffer.writeVarInt(value.surpriseStyle());
					buffer.writeVarInt(value.deathStyle());
					buffer.writeVarInt(value.dodgeSound());
				},
				buffer -> new ParticleStylePayload(buffer.readVarInt(), buffer.readBoolean(),
						buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
						buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ConfigPayload(String json) implements CustomPacketPayload {
		public static final Type<ConfigPayload> TYPE =
				new Type<>(ResourceLocation.fromNamespaceAndPath(SummyEnchantment.MOD_ID, "config_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ConfigPayload> CODEC = StreamCodec.of(
				(buffer, value) -> buffer.writeUtf(value.json(), 2_097_152),
				buffer -> new ConfigPayload(buffer.readUtf(2_097_152)));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ParticleRequestPayload(int protocolVersion) implements CustomPacketPayload {
		public static final Type<ParticleRequestPayload> TYPE =
				new Type<>(ResourceLocation.fromNamespaceAndPath(
						SummyEnchantment.MOD_ID, "particle_style_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ParticleRequestPayload> CODEC = StreamCodec.of(
				(buffer, value) -> buffer.writeVarInt(value.protocolVersion()),
				buffer -> new ParticleRequestPayload(buffer.readVarInt()));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ArrowLightnessPayload(int entityId, int level) implements CustomPacketPayload {
		public static final Type<ArrowLightnessPayload> TYPE =
				new Type<>(ResourceLocation.fromNamespaceAndPath(SummyEnchantment.MOD_ID, "arrow_lightness"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ArrowLightnessPayload> CODEC = StreamCodec.of(
				(buffer, value) -> {
					buffer.writeVarInt(value.entityId());
					buffer.writeVarInt(value.level());
				},
				buffer -> new ArrowLightnessPayload(buffer.readVarInt(), buffer.readVarInt()));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
