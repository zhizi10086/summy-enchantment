package com.summy.enchantment.network;

import com.summy.enchantment.SummyEnchantment;
import com.summy.enchantment.config.DodgeSoundStyle;
import com.summy.enchantment.config.SummyEnchantmentConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端与客户端之间同步配置、粒子偏好和箭矢轻盈状态。
 */
public final class ConfigSync {
	public static final int PARTICLE_PROTOCOL_VERSION = 9;
	public static final int LONG_DISTANCE_PROTOCOL_VERSION = 2;
	private static final int REQUEST_INTERVAL_TICKS = 100;
	private static final int MAX_REQUEST_ATTEMPTS = 6;
	private static final Map<UUID, int[]> REQUEST_STATE = new ConcurrentHashMap<>();
	private static boolean payloadsRegistered;

	private ConfigSync() {
	}

	public static void registerPayloads() {
		if (payloadsRegistered) {
			return;
		}
		payloadsRegistered = true;
		PayloadTypeRegistry.playC2S().register(ParticleStylePayload.ID, ParticleStylePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ConfigPayload.ID, ConfigPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ParticleRequestPayload.ID, ParticleRequestPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ArrowLightnessPayload.ID, ArrowLightnessPayload.CODEC);
	}

	public static void registerServerReceivers() {
		registerPayloads();
		ServerPlayNetworking.registerGlobalReceiver(ParticleStylePayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ParticlePreferences.Preference preference = ParticlePreferences.set(
					player.getUuid(), payload.style(), payload.longDistance(), payload.distance(),
					payload.meleeStyle(), payload.sweepStyle(), payload.meleeEffect(),
					payload.surpriseStyle(), payload.deathStyle(), payload.dodgeSound());
			REQUEST_STATE.remove(player.getUuid());
			if (preference != null && SummyEnchantmentConfig.get().debugLogs) {
				SummyEnchantment.LOGGER.info(
						"[Summy Enchantment] 玩家 {} 的粒子档位 = 箭矢 {} / 近战 {} / 横扫 {} / 强度 {}"
								+ " / 出其不意 {} / 死亡 {} / 闪避音效 {}（远距离={}，半径={}）",
						player.getGameProfile().getName(), preference.style(), preference.meleeStyle(),
						preference.sweepStyle(), preference.meleeEffect(), preference.surpriseStyle(),
						preference.deathStyle(), preference.dodgeSound(),
						preference.longDistance() ? "开" : "关", preference.distance());
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID playerId = handler.getPlayer().getUuid();
			ParticlePreferences.clear(playerId);
			REQUEST_STATE.remove(playerId);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ParticlePreferences.clearAll());
	}

	public static boolean requestParticlePreference(ServerPlayerEntity player) {
		if (!ServerPlayNetworking.canSend(player, ParticleRequestPayload.ID)) {
			return false;
		}
		ServerPlayNetworking.send(player, new ParticleRequestPayload(PARTICLE_PROTOCOL_VERSION));
		return true;
	}

	public static void tickParticlePreferenceRequests(ServerPlayerEntity player, int serverTicks) {
		UUID playerId = player.getUuid();
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
		if (requestParticlePreference(player)) {
			state[0] = serverTicks;
			++state[1];
			if (state[1] == MAX_REQUEST_ATTEMPTS) {
				SummyEnchantment.LOGGER.info(
						"[Summy Enchantment] 未收到玩家 {} 的箭矢粒子偏好，其射出的箭将按观察者设置渲染",
						player.getGameProfile().getName());
			}
		}
	}

	public static void sendTo(ServerPlayerEntity player) {
		ServerPlayNetworking.send(player, new ConfigPayload(SummyEnchantmentConfig.get().toJsonString()));
	}

	public static void sendArrowLightness(PersistentProjectileEntity arrow, int level) {
		if (arrow == null || level <= 0 || !(arrow.getWorld() instanceof ServerWorld world)) {
			return;
		}
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.squaredDistanceTo(arrow) > 64.0D * 64.0D
					|| !ServerPlayNetworking.canSend(player, ArrowLightnessPayload.ID)) {
				continue;
			}
			ServerPlayNetworking.send(player, new ArrowLightnessPayload(arrow.getId(), level));
		}
	}

	public static void broadcast(MinecraftServer server) {
		if (server == null) {
			return;
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			sendTo(player);
		}
	}

	public record ConfigPayload(String json) implements CustomPayload {
		public static final Id<ConfigPayload> ID =
				new Id<>(Identifier.of(SummyEnchantment.MOD_ID, "config_sync"));
		public static final PacketCodec<RegistryByteBuf, ConfigPayload> CODEC = PacketCodec.of(
				(value, buffer) -> buffer.writeString(value.json(), 2_097_152),
				buffer -> new ConfigPayload(buffer.readString(2_097_152)));

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	public record ParticleStylePayload(int style, boolean longDistance, int distance, int meleeStyle,
			int sweepStyle, int meleeEffect, int surpriseStyle, int deathStyle, int dodgeSound)
			implements CustomPayload {
		public static final Id<ParticleStylePayload> ID =
				new Id<>(Identifier.of(SummyEnchantment.MOD_ID, "particle_style"));
		public static final PacketCodec<RegistryByteBuf, ParticleStylePayload> CODEC = PacketCodec.of(
				(value, buffer) -> {
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
				buffer -> new ParticleStylePayload(
						buffer.readVarInt(),
						buffer.readBoolean(),
						buffer.readVarInt(),
						buffer.readVarInt(),
						buffer.readVarInt(),
						buffer.readVarInt(),
						buffer.readVarInt(),
						buffer.readVarInt(),
						buffer.readVarInt()));

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	public record ParticleRequestPayload(int protocolVersion) implements CustomPayload {
		public static final Id<ParticleRequestPayload> ID =
				new Id<>(Identifier.of(SummyEnchantment.MOD_ID, "particle_style_request"));
		public static final PacketCodec<RegistryByteBuf, ParticleRequestPayload> CODEC = PacketCodec.of(
				(value, buffer) -> buffer.writeVarInt(value.protocolVersion()),
				buffer -> new ParticleRequestPayload(buffer.readVarInt()));

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	public record ArrowLightnessPayload(int entityId, int level) implements CustomPayload {
		public static final Id<ArrowLightnessPayload> ID =
				new Id<>(Identifier.of(SummyEnchantment.MOD_ID, "arrow_lightness"));
		public static final PacketCodec<RegistryByteBuf, ArrowLightnessPayload> CODEC = PacketCodec.of(
				(value, buffer) -> {
					buffer.writeVarInt(value.entityId());
					buffer.writeVarInt(value.level());
				},
				buffer -> new ArrowLightnessPayload(buffer.readVarInt(), buffer.readVarInt()));

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}
}
