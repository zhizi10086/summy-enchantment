package com.summy.enchantment.devcheck;

import com.summy.enchantment.SummyEnchantment;
import com.summy.enchantment.combat.EnchantmentEffects;
import com.summy.enchantment.registry.ModEnchantments;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * SE 的 Fabric 原生真实客户端自检。
 *
 * <p>只通过 {@code gradlew runClient -Pdevcheck -PexternalModsDir=...} 启用。
 * 自检会在专用世界中准备物品与僵尸目标，结束后自动退出客户端；
 * 正式 jar 排除本包，不影响发布行为。
 */
public final class EnchantmentFabricDevCheck {
	private static final boolean ENABLED = Boolean.getBoolean("summy_enchantment.devcheck");
	private static final int CLIENT_IDLE_STOP_TICKS = 1200;
	private static final int CLIENT_TOTAL_STOP_TICKS = 6000;
	private static final String TEST_TAG = "se_devcheck_target";

	private static final List<String> FAILURES = new ArrayList<>();
	private static int serverTick;
	private static int clientTick;
	private static int idleTicks;
	private static int totalClientTicks;
	private static int checks;
	private static boolean finished;
	private static boolean registered;

	private EnchantmentFabricDevCheck() {
	}

	/** 由客户端入口在 devcheck 开关打开时注册；重复调用不会重复挂事件。 */
	public static void register() {
		if (!ENABLED || registered) {
			return;
		}
		registered = true;
		ServerTickEvents.END_SERVER_TICK.register(server -> onServerTick(server));
		ClientTickEvents.END_CLIENT_TICK.register(EnchantmentFabricDevCheck::onClientTick);
	}

	private static void onServerTick(net.minecraft.server.MinecraftServer server) {
		if (!ENABLED) {
			return;
		}
		List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
		if (players.isEmpty()) {
			serverTick = 0;
			return;
		}
		ServerPlayerEntity player = players.get(0);
		serverTick++;
		try {
			switch (serverTick) {
				case 20 -> prepareWorld(player);
				case 40 -> checkMajruszOutgoing(player);
				case 60 -> checkMajruszIncoming(player);
				case 80 -> finish("Fabric 客户端自检结束");
				default -> {
				}
			}
		} catch (Throwable throwable) {
			FAILURES.add("步骤 " + serverTick + " 异常：" + throwable);
			log("步骤 " + serverTick + " 异常：" + throwable);
			throwable.printStackTrace();
			finish("步骤异常");
		}
	}

	private static void onClientTick(MinecraftClient client) {
		if (!ENABLED) {
			return;
		}
		// 测试机窗口失焦时单人游戏会暂停；自检期间关闭该暂停，确保集成服务端继续推进。
		client.options.pauseOnLostFocus = false;
		totalClientTicks++;
		if (totalClientTicks > CLIENT_TOTAL_STOP_TICKS) {
			finish("硬停止：客户端总 tick 超限");
			return;
		}
		if (client.player == null || client.world == null) {
			idleTicks++;
			if (idleTicks > CLIENT_IDLE_STOP_TICKS) {
				finish("硬停止：等待测试世界超时");
			}
			return;
		}
		idleTicks = 0;
		clientTick++;
		if (finished && clientTick > 20) {
			client.stop();
		}
	}

	private static void prepareWorld(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		player.getInventory().clear();
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
		EnchantmentEffects.clearLifeShield(player);
		List<ZombieEntity> stale = world.getEntitiesByClass(
				ZombieEntity.class, player.getBoundingBox().expand(32.0D),
				zombie -> zombie.getCommandTags().contains(TEST_TAG));
		stale.forEach(ZombieEntity::discard);
		log("测试准备：维度=" + world.getRegistryKey().getValue()
				+ "，Majrusz=" + isLoaded("majruszsenchantments")
				+ "，MajruszLibrary=" + isLoaded("majruszlibrary"));
		check("Fabric 原生 Majrusz 前置已加载", isLoaded("majruszsenchantments")
				&& isLoaded("majruszlibrary"), "world=" + world.getRegistryKey().getValue());
		check("测试世界可写", player.isAlive(), "player=" + player.getName().getString());
	}

	private static void checkMajruszOutgoing(ServerPlayerEntity player) {
		Enchantment majrusz = external("majruszsenchantments", "death_wish");
		player.getInventory().clear();
		player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, enchanted(Items.IRON_SWORD, majrusz, 1));
		player.setStackInHand(net.minecraft.util.Hand.OFF_HAND, ItemStack.EMPTY);
		player.equipStack(net.minecraft.entity.EquipmentSlot.CHEST,
				enchanted(Items.IRON_CHESTPLATE, ModEnchantments.DEATHS_BLESSING, 1));
		player.equipStack(net.minecraft.entity.EquipmentSlot.LEGS, ItemStack.EMPTY);
		player.equipStack(net.minecraft.entity.EquipmentSlot.FEET, ItemStack.EMPTY);
		player.equipStack(net.minecraft.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);
		player.setHealth(player.getMaxHealth() * 0.5F);
		player.setAbsorptionAmount(0.0F);

		ZombieEntity target = spawnTarget(player, 100.0F, 100.0F);
		target.damage(player.getDamageSources().playerAttack(player), 10.0F);
		float outgoingLost = 100.0F - target.getHealth();
		check("死神祝福：造成侧只乘一层", Math.abs(outgoingLost - 15.0F) < 0.05F,
				"lost=" + outgoingLost + " Majrusz=" + (majrusz != null));
		target.discard();
	}

	private static void checkMajruszIncoming(ServerPlayerEntity player) {
		Enchantment majrusz = external("majruszsenchantments", "death_wish");
		player.getInventory().clear();
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
		ZombieEntity victim = spawnTarget(player, 100.0F, 50.0F);
		victim.equipStack(EquipmentSlot.CHEST,
				enchanted(Items.PAPER, ModEnchantments.DEATHS_BLESSING, 1));
		victim.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, enchanted(Items.IRON_SWORD, majrusz, 1));
		victim.damage(player.getDamageSources().playerAttack(player), 10.0F);
		float incomingLost = 50.0F - victim.getHealth();
		check("死神祝福：受伤侧只乘一层", Math.abs(incomingLost - 9.5F) < 0.05F,
				"lost=" + incomingLost + " Majrusz=" + (majrusz != null));
		victim.discard();
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
	}

	private static ZombieEntity spawnTarget(ServerPlayerEntity player, float maxHealth, float health) {
		ServerWorld world = player.getServerWorld();
		ZombieEntity zombie = net.minecraft.entity.EntityType.ZOMBIE.create(world);
		if (zombie == null) {
			throw new IllegalStateException("无法创建测试僵尸");
		}
		zombie.setPosition(player.getX() + 3.0D, player.getY(), player.getZ());
		zombie.setAiDisabled(true);
		zombie.setPersistent();
		zombie.setInvulnerable(false);
		zombie.addCommandTag(TEST_TAG);
		zombie.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
		zombie.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(0.0D);
		zombie.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).setBaseValue(0.0D);
		zombie.setHealth(health);
		zombie.setVelocity(Vec3d.ZERO);
		zombie.timeUntilRegen = 0;
		world.spawnEntity(zombie);
		return zombie;
	}

	private static ItemStack enchanted(Item item, Enchantment enchantment, int level) {
		ItemStack stack = new ItemStack(item);
		if (enchantment != null) {
			stack.addEnchantment(enchantment, level);
		}
		return stack;
	}

	private static Enchantment external(String namespace, String path) {
		return Registries.ENCHANTMENT.get(new Identifier(namespace, path));
	}

	private static boolean isLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	private static void check(String label, boolean ok, String detail) {
		checks++;
		if (!ok) {
			FAILURES.add(label + "：" + detail);
		}
		log((ok ? "OK  " : "FAIL") + " " + label + "：" + detail);
	}

	private static void finish(String reason) {
		if (finished) {
			return;
		}
		finished = true;
		log("===== " + reason + "：检查 " + checks + " 项，失败 " + FAILURES.size() + " 项 =====");
		for (String failure : FAILURES) {
			log("FAILED：" + failure);
		}
		MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().stop());
	}

	private static void log(String message) {
		SummyEnchantment.LOGGER.info("[SE-DEVCHECK] {}", message);
	}
}
