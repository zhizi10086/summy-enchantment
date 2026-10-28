package com.summy.enchantment.devcheck;

import com.summy.enchantment.SummyEnchantment;
import com.summy.enchantment.combat.EnchantmentEffects;
import com.summy.enchantment.registry.ModEnchantments;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * SE 的真实客户端开发自检。
 *
 * <p>只通过 {@code gradlew runClient -Pdevcheck} 启用；测试世界会清理玩家背包与装备，
 * 因此只能使用专用存档 {@code SeDevCheckWorld}。正式 jar 不包含本包。
 */
@Mod.EventBusSubscriber(modid = SummyEnchantment.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class EnchantmentForgeDevCheck {
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

	private EnchantmentForgeDevCheck() {
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (!ENABLED || event.phase != TickEvent.Phase.END) {
			return;
		}
		if (event.getServer().getPlayerList().getPlayers().isEmpty()) {
			serverTick = 0;
			return;
		}
		ServerPlayer player = event.getServer().getPlayerList().getPlayers().get(0);
		serverTick++;
		try {
			switch (serverTick) {
				case 20 -> prepareWorld(player);
				case 40 -> checkLifeShield(player);
				case 60 -> checkLifeMending(player);
				case 80 -> checkDeathsBlessing(player);
				case 100 -> finish("客户端自检结束");
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

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (!ENABLED || event.phase != TickEvent.Phase.END) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		totalClientTicks++;
		if (totalClientTicks > CLIENT_TOTAL_STOP_TICKS) {
			finish("硬停止：客户端总 tick 超限");
			return;
		}
		if (client.player == null || client.level == null) {
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

	private static void prepareWorld(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		player.getInventory().clearContent();
		player.removeAllEffects();
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
		EnchantmentEffects.clearLifeShield(player);
		level.getEntitiesOfClass(Zombie.class, player.getBoundingBox().inflate(32.0D),
				zombie -> zombie.getTags().contains(TEST_TAG)).forEach(Zombie::discard);
		log("测试准备：维度=" + level.dimension().location()
				+ "，Celestial=" + isLoaded("celestial_enchantments")
				+ "，Apotheosis=" + isLoaded("apotheosis")
				+ "，Majrusz=" + isLoaded("majruszsenchantments"));
		check("基础环境", level != null && player.isAlive(), "player=" + player.getName().getString());
	}

	private static void checkLifeShield(ServerPlayer player) {
		Enchantment celestial = external("celestial_enchantments", "life_shield");
		ItemStack both = enchanted(Items.IRON_CHESTPLATE, ModEnchantments.LIFE_SHIELD, 5);
		if (celestial != null) {
			both.enchant(celestial, 5);
		}
		player.setItemSlot(EquipmentSlot.CHEST, both);
		EnchantmentEffects.clearLifeShield(player);
		player.setAbsorptionAmount(0.0F);
		player.setHealth(10.0F);
		player.heal(5.0F);
		float seShield = EnchantmentEffects.lifeShield(player);
		float absorption = player.getAbsorptionAmount();
		if (celestial != null) {
			check("Celestial 生命护盾：SE 退让", Math.abs(seShield) < 0.01F, "SE=" + seShield);
			check("Celestial 生命护盾：对方结算", absorption >= 5.0F,
					"absorption=" + absorption + " health=" + player.getHealth());
		} else {
			check("SE 生命护盾单装：属性累积", Math.abs(seShield - 5.0F) < 0.01F, "SE=" + seShield);
			check("SE 生命护盾单装：吸收池同步", absorption >= 5.0F, "absorption=" + absorption);
		}

		player.setItemSlot(EquipmentSlot.CHEST,
				enchanted(Items.IRON_CHESTPLATE, ModEnchantments.LIFE_SHIELD, 5));
		EnchantmentEffects.clearLifeShield(player);
		player.setAbsorptionAmount(0.0F);
		player.setHealth(10.0F);
		player.heal(5.0F);
		check("移除 Celestial 后 SE 恢复", EnchantmentEffects.lifeShield(player) >= 5.0F,
				"SE=" + EnchantmentEffects.lifeShield(player));
		check("SE 生命护盾上限", Math.abs(EnchantmentEffects.lifeShieldCap(player) - 20.0F) < 0.01F,
				"cap=" + EnchantmentEffects.lifeShieldCap(player));
	}

	private static void checkLifeMending(ServerPlayer player) {
		Enchantment apotheosis = external("apotheosis", "life_mending");
		ItemStack chest = damaged(Items.IRON_CHESTPLATE, ModEnchantments.LIFE_MENDING, 3, 20);
		ItemStack boots = apotheosis == null
				? ItemStack.EMPTY
				: damaged(Items.IRON_BOOTS, apotheosis, 1, 20);
		player.getInventory().clearContent();
		player.setItemSlot(EquipmentSlot.CHEST, chest);
		player.setItemSlot(EquipmentSlot.FEET, boots);
		player.setAbsorptionAmount(0.0F);
		EnchantmentEffects.clearLifeShield(player);
		player.setHealth(10.0F);
		player.heal(4.0F);
		check("生命修补：SE 优先", Math.abs(player.getHealth() - 10.0F) < 0.01F,
				"health=" + player.getHealth());
		check("生命修补：SE 修到 12 点", chest.getDamageValue() == 8,
				"damage=" + chest.getDamageValue());
		if (!boots.isEmpty()) {
			check("生命修补：Apoth 被跳过", boots.getDamageValue() == 20,
					"damage=" + boots.getDamageValue());
			player.setItemSlot(EquipmentSlot.CHEST,
					new ItemStack(Items.IRON_CHESTPLATE));
			boots.setDamageValue(2);
			player.setHealth(10.0F);
			player.heal(4.0F);
			check("生命修补：SE 无目标后 Apoth 生效", boots.getDamageValue() == 0,
					"damage=" + boots.getDamageValue());
			check("生命修补：Apoth 保留剩余治疗", Math.abs(player.getHealth() - 12.0F) < 0.01F,
					"health=" + player.getHealth());
		}
		player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
		player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
	}

	private static void checkDeathsBlessing(ServerPlayer player) {
		Enchantment majrusz = external("majruszsenchantments", "death_wish");
		player.getInventory().clearContent();
		player.setItemSlot(EquipmentSlot.CHEST,
				enchanted(Items.IRON_CHESTPLATE, ModEnchantments.DEATHS_BLESSING, 1));
		if (majrusz != null) {
			player.setItemSlot(EquipmentSlot.MAINHAND,
					enchanted(Items.IRON_SWORD, majrusz, 1));
		}
		player.setHealth(10.0F);
		player.setAbsorptionAmount(0.0F);

		Zombie target = spawnTarget(player, 100.0F, 100.0F);
		target.hurt(player.damageSources().playerAttack(player), 10.0F);
		float outgoingLost = 100.0F - target.getHealth();
		check("死神祝福：造成侧只乘一层", Math.abs(outgoingLost - 15.0F) < 0.05F,
				"lost=" + outgoingLost + " Majrusz=" + (majrusz != null));
		target.discard();

		player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
		player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		Zombie victim = spawnTarget(player, 100.0F, 50.0F);
		victim.setItemSlot(EquipmentSlot.CHEST,
				enchanted(Items.PAPER, ModEnchantments.DEATHS_BLESSING, 1));
		if (majrusz != null) {
			victim.setItemSlot(EquipmentSlot.MAINHAND,
					enchanted(Items.IRON_SWORD, majrusz, 1));
		}
		victim.hurt(player.damageSources().playerAttack(player), 10.0F);
		float incomingLost = 50.0F - victim.getHealth();
		check("死神祝福：受伤侧只乘一层", Math.abs(incomingLost - 9.5F) < 0.05F,
				"lost=" + incomingLost + " Majrusz=" + (majrusz != null));
		victim.discard();

		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0.0F);
	}

	private static Zombie spawnTarget(ServerPlayer player, float maxHealth, float health) {
		ServerLevel level = player.serverLevel();
		Zombie zombie = EntityType.ZOMBIE.create(level);
		if (zombie == null) {
			throw new IllegalStateException("无法创建测试僵尸");
		}
		zombie.moveTo(player.getX() + 3.0D, player.getY(), player.getZ(), 0.0F, 0.0F);
		zombie.setNoAi(true);
		zombie.setPersistenceRequired();
		zombie.setInvulnerable(false);
		zombie.addTag(TEST_TAG);
		zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
		zombie.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
		zombie.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0.0D);
		zombie.setHealth(health);
		zombie.setDeltaMovement(Vec3.ZERO);
		zombie.invulnerableTime = 0;
		level.addFreshEntity(zombie);
		return zombie;
	}

	private static ItemStack enchanted(Item item, Enchantment enchantment, int level) {
		ItemStack stack = new ItemStack(item);
		if (enchantment != null) {
			stack.enchant(enchantment, level);
		}
		return stack;
	}

	private static ItemStack damaged(Item item, Enchantment enchantment, int level, int damage) {
		ItemStack stack = enchanted(item, enchantment, level);
		stack.setDamageValue(Math.min(damage, stack.getMaxDamage() - 1));
		return stack;
	}

	private static Enchantment external(String namespace, String path) {
		return ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(namespace, path));
	}

	private static boolean isLoaded(String modId) {
		return net.minecraftforge.fml.ModList.get().isLoaded(modId);
	}

	private static void check(String label, boolean ok, String detail) {
		checks++;
		if (!ok) {
			FAILURES.add(label + "｜" + detail);
		}
		log((ok ? "OK  " : "FAIL") + " " + label + "｜" + detail);
	}

	private static void finish(String reason) {
		if (finished) {
			return;
		}
		finished = true;
		log("===== " + reason + "：检查 " + checks + " 项，失败 " + FAILURES.size() + " 项 =====");
		for (String failure : FAILURES) {
			log("FAILED｜" + failure);
		}
		Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
	}

	private static void log(String message) {
		SummyEnchantment.LOGGER.info("[SE-DEVCHECK] {}", message);
	}
}
