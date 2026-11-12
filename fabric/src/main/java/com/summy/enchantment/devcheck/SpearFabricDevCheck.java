package com.summy.enchantment.devcheck;

import com.notunanancyowen.spears.Spears;
import com.notunanancyowen.spears.components.KineticWeapon;
import com.notunanancyowen.spears.components.PiercingWeapon;
import com.summy.enchantment.SummyEnchantment;
import com.summy.enchantment.compat.SpearCompat;
import com.summy.enchantment.registry.ModEnchantments;
import com.summy.enchantment.registry.ModStatusEffects;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Fabric 矛兼容专项自检；仅在 Spears 已安装时由主 devcheck 加载。 */
public final class SpearFabricDevCheck {
	private static final String TEST_TAG = "se_devcheck_spear_target";
	private static HuskEntity target;
	private static float chargePlainLoss;

	private SpearFabricDevCheck() {
	}

	public static SpearDevCheckResult tick(ServerPlayerEntity player, int step) {
		if (step == 20) {
			return prepare(player);
		}
		if (step == 60) {
			return checkStab(player, "stab_plain", newSpear(), 5.0F);
		}
		if (step == 100) {
			ItemStack sharpness = newSpear();
			sharpness.addEnchantment(vanillaEnchantment(player, Enchantments.SHARPNESS), 2);
			return checkStab(player, "stab_sharp_2", sharpness, 6.5F);
		}
		if (step == 140) {
			player.clearStatusEffects();
			player.addStatusEffect(new StatusEffectInstance(
					ModStatusEffects.STRENGTH, 200, 0, false, false, false));
			return checkStab(player, "stab_strength_1", newSpear(), 7.5F);
		}
		if (step == 180) {
			player.clearStatusEffects();
			ItemStack effects = newSpear();
			effects.addEnchantment(ModEnchantments.SPELLBLADE.entry(), 5);
			effects.addEnchantment(ModEnchantments.LIFESTEAL.entry(), 3);
			return checkSettlement(player, effects);
		}
		if (step == 220) {
			player.clearStatusEffects();
			return checkCharge(player, "charge_plain", newSpear(), -1.0F);
		}
		if (step == 260) {
			ItemStack sharpness = newSpear();
			sharpness.addEnchantment(vanillaEnchantment(player, Enchantments.SHARPNESS), 2);
			return checkCharge(player, "charge_sharp_2", sharpness, chargePlainLoss + 1.5F);
		}
		return SpearDevCheckResult.idle();
	}

	private static SpearDevCheckResult prepare(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		player.getInventory().clear();
		player.clearStatusEffects();
		player.setHealth(player.getMaxHealth());
		player.setYaw(0.0F);
		player.setPitch(0.0F);
		player.setStackInHand(Hand.MAIN_HAND, newSpear());
		BlockPos center = player.getBlockPos();
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -2; dz <= 6; dz++) {
				for (int dy = -1; dy <= 2; dy++) {
					world.setBlockState(center.add(dx, dy, dz), dy == -1 ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState());
				}
			}
		}
		world.getEntitiesByClass(HuskEntity.class, player.getBoundingBox().expand(32.0D),
				husk -> husk.getCommandTags().contains(TEST_TAG)).forEach(HuskEntity::discard);

		HuskEntity husk = net.minecraft.entity.EntityType.HUSK.create(world);
		if (husk == null) {
			return fail("target_create", "EntityType.HUSK.create 返回 null");
		}
		husk.setPosition(player.getX(), player.getY(), player.getZ() + 2.0D);
		husk.setAiDisabled(true);
		husk.setNoGravity(true);
		husk.setSilent(true);
		husk.setPersistent();
		husk.setInvulnerable(false);
		husk.addCommandTag(TEST_TAG);
		husk.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(1000.0D);
		husk.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(0.0D);
		husk.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).setBaseValue(0.0D);
		husk.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
		husk.setHealth(1000.0F);
		husk.setVelocity(Vec3d.ZERO);
		husk.timeUntilRegen = 0;
		world.spawnEntity(husk);
		target = husk;

		String version = FabricLoader.getInstance().getModContainer("spears")
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("<missing>");
		return combine(
				check("version", "1.4.7".equals(version) || isFixedOrLater(version), "version=" + version),
				check("active", SpearCompat.isActive(), "active=" + SpearCompat.isActive())
		);
	}

	private static SpearDevCheckResult checkStab(ServerPlayerEntity player, String label,
			ItemStack spear, float expected) {
		prepareAttack(player, spear);
		float cooldown = player.getAttackCooldownProgress(0.5F);
		float before = target.getHealth();
		PiercingWeapon piercing = spear.get(Spears.PIERCING_WEAPON);
		boolean hit = piercing != null && piercing.stab(player, EquipmentSlot.MAINHAND);
		float lost = before - target.getHealth();
		player.resetLastAttackedTicks();
		return check(label, hit && cooldown > 0.99F && Math.abs(lost - expected) < 0.05F,
				"hit=" + hit + " cooldown=" + cooldown + " lost=" + lost + " expected=" + expected);
	}

	private static SpearDevCheckResult checkSettlement(ServerPlayerEntity player, ItemStack spear) {
		prepareAttack(player, spear);
		player.setHealth(player.getMaxHealth() - 6.0F);
		float healthBefore = player.getHealth();
		float targetBefore = target.getHealth();
		PiercingWeapon piercing = spear.get(Spears.PIERCING_WEAPON);
		boolean hit = piercing != null && piercing.stab(player, EquipmentSlot.MAINHAND);
		float targetLost = targetBefore - target.getHealth();
		float healed = player.getHealth() - healthBefore;
		player.resetLastAttackedTicks();
		return combine(
				check("settlement_primary", hit && Math.abs(targetLost - 7.0F) < 0.05F,
						"hit=" + hit + " lost=" + targetLost + " expected=7.0"),
				check("lifesteal", Math.abs(healed - 1.2F) < 0.05F,
						"healed=" + healed + " expected=1.2")
		);
	}

	private static SpearDevCheckResult checkCharge(ServerPlayerEntity player, String label,
			ItemStack spear, float expected) {
		prepare(player);
		player.setStackInHand(Hand.MAIN_HAND, spear);
		target.setPosition(player.getX(), player.getY(), player.getZ() + 3.0D);
		player.setOnGround(player.getRotationVector());
		Vec3d movementBefore = player.getMovement();
		player.setCurrentHand(Hand.MAIN_HAND);
		float before = target.getHealth();
		KineticWeapon kinetic = spear.get(Spears.KINETIC_WEAPON);
		if (kinetic != null) {
			kinetic.usageTick(spear, 71950, player, EquipmentSlot.MAINHAND);
		}
		player.clearActiveItem();
		player.setOnGround(Vec3d.ZERO);
		float lost = before - target.getHealth();
		player.resetLastAttackedTicks();
		boolean ok = expected < 0.0F ? lost > 0.0F : Math.abs(lost - expected) < 0.05F;
		if (expected < 0.0F) {
			chargePlainLoss = lost;
		}
		return check(label, ok,
				"lost=" + lost + " expected=" + expected + " kinetic=" + (kinetic != null)
						+ " movement=" + movementBefore);
	}

	private static void prepareAttack(ServerPlayerEntity player, ItemStack spear) {
		player.getInventory().clear();
		player.setStackInHand(Hand.MAIN_HAND, spear);
		player.setYaw(0.0F);
		player.setPitch(0.0F);
		// 矛会逐目标结算，清掉测试路径上的其它实体，避免多次嗜血或事件污染断言。
		player.getServerWorld().getEntitiesByClass(
				Entity.class,
				player.getBoundingBox().expand(32.0D),
				entity -> entity != player && entity != target
		).forEach(Entity::discard);
		target.setHealth(1000.0F);
		target.setPosition(player.getX(), player.getY(), player.getZ() + 2.0D);
		target.setVelocity(Vec3d.ZERO);
		target.timeUntilRegen = 0;
		target.hurtTime = 0;
		player.setHealth(player.getMaxHealth());
	}

	private static ItemStack newSpear() {
		return new ItemStack(Spears.NETHERITE_SPEAR);
	}

	private static RegistryEntry<Enchantment> vanillaEnchantment(
			ServerPlayerEntity player, RegistryKey<Enchantment> key) {
		return player.getServerWorld().getRegistryManager()
				.get(RegistryKeys.ENCHANTMENT)
				.getEntry(key)
				.orElseThrow(() -> new IllegalStateException("Missing enchantment " + key.getValue()));
	}

	private static boolean isFixedOrLater(String version) {
		String[] parts = version.split("\\.");
		if (parts.length < 3) {
			return false;
		}
		try {
			int major = Integer.parseInt(parts[0]);
			int minor = Integer.parseInt(parts[1]);
			int patch = Integer.parseInt(parts[2].replaceAll("\\D.*$", ""));
			return major > 1 || major == 1 && (minor > 5 || minor == 5 && patch >= 0);
		} catch (NumberFormatException ignored) {
			return false;
		}
	}

	private static SpearDevCheckResult check(String key, boolean ok, String detail) {
		SummyEnchantment.LOGGER.info("[SE-DEVCHECK] SPEAR_{} key={} {}",
				ok ? "OK" : "FAIL", key, detail);
		return ok
				? new SpearDevCheckResult(1, List.of())
				: new SpearDevCheckResult(1, List.of(key + "：" + detail));
	}

	private static SpearDevCheckResult fail(String key, String detail) {
		return check(key, false, detail);
	}

	private static SpearDevCheckResult combine(SpearDevCheckResult first, SpearDevCheckResult second) {
		List<String> failures = new ArrayList<>(first.failures());
		failures.addAll(second.failures());
		return new SpearDevCheckResult(first.checks() + second.checks(), List.copyOf(failures));
	}
}
