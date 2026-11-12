package com.summy.enchantment.devcheck;

import com.notunanancyowen.spears.Spears;
import com.notunanancyowen.spears.components.KineticWeapon;
import com.notunanancyowen.spears.components.PiercingWeapon;
import com.summy.enchantment.SummyEnchantment;
import com.summy.enchantment.compat.SpearCompat;
import com.summy.enchantment.registry.ModEnchantments;
import com.summy.enchantment.registry.ModStatusEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/** Forge 矛兼容专项自检；仅在 Spears 已安装时由主 devcheck 加载。 */
public final class SpearForgeDevCheck {
	private static final String TEST_TAG = "se_devcheck_spear_target";
	private static Husk target;
	private static float chargePlainLoss;

	private SpearForgeDevCheck() {
	}

	public static SpearDevCheckResult tick(ServerPlayer player, int step) {
		if (step == 20) {
			return prepare(player);
		}
		if (step == 60) {
			return checkStab(player, "stab_plain", newSpear(), 5.0F);
		}
		if (step == 100) {
			ItemStack sharpness = newSpear();
			sharpness.enchant(vanillaEnchantment(player, Enchantments.SHARPNESS), 2);
			return checkStab(player, "stab_sharp_2", sharpness, 6.5F);
		}
		if (step == 140) {
			player.removeAllEffects();
			player.addEffect(new MobEffectInstance(ModStatusEffects.STRENGTH, 200, 0, false, false, false));
			return checkStab(player, "stab_strength_1", newSpear(), 7.5F);
		}
		if (step == 180) {
			player.removeAllEffects();
			ItemStack effects = newSpear();
			effects.enchant(ModEnchantments.SPELLBLADE.entry(), 5);
			effects.enchant(ModEnchantments.LIFESTEAL.entry(), 3);
			return checkSettlement(player, effects);
		}
		if (step == 220) {
			player.removeAllEffects();
			return checkCharge(player, "charge_plain", newSpear(), -1.0F);
		}
		if (step == 260) {
			ItemStack sharpness = newSpear();
			sharpness.enchant(vanillaEnchantment(player, Enchantments.SHARPNESS), 2);
			return checkCharge(player, "charge_sharp_2", sharpness, chargePlainLoss + 1.5F);
		}
		return SpearDevCheckResult.idle();
	}

	private static SpearDevCheckResult prepare(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		player.getInventory().clearContent();
		player.removeAllEffects();
		player.setHealth(player.getMaxHealth());
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.setItemSlot(EquipmentSlot.MAINHAND, newSpear());
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -2; dz <= 6; dz++) {
				for (int dy = -1; dy <= 2; dy++) {
					level.setBlockAndUpdate(player.blockPosition().offset(dx, dy, dz),
							dy == -1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
				}
			}
		}
		level.getEntitiesOfClass(Husk.class, player.getBoundingBox().inflate(32.0D),
				husk -> husk.getTags().contains(TEST_TAG)).forEach(Husk::discard);

		Husk husk = EntityType.HUSK.create(level);
		if (husk == null) {
			return fail("target_create", "EntityType.HUSK.create 返回 null");
		}
		husk.moveTo(player.getX(), player.getY(), player.getZ() + 2.0D, 0.0F, 0.0F);
		husk.setNoAi(true);
		husk.setNoGravity(true);
		husk.setSilent(true);
		husk.setPersistenceRequired();
		husk.setInvulnerable(false);
		husk.addTag(TEST_TAG);
		husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0D);
		husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
		husk.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0.0D);
		husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
		husk.setHealth(1000.0F);
		husk.setDeltaMovement(Vec3.ZERO);
		husk.invulnerableTime = 0;
		level.addFreshEntity(husk);
		target = husk;

		String version = ModList.get().getModContainerById("spears")
				.map(container -> container.getModInfo().getVersion().toString())
				.orElse("<missing>");
		return combine(
				check("version", "1.4.7".equals(version) || isFixedOrLater(version), "version=" + version),
				check("active", SpearCompat.isActive(), "active=" + SpearCompat.isActive())
		);
	}

	private static SpearDevCheckResult checkStab(ServerPlayer player, String label,
			ItemStack spear, float expected) {
		prepareAttack(player, spear);
		float cooldown = player.getAttackStrengthScale(0.5F);
		float before = target.getHealth();
		PiercingWeapon piercing = spear.get(Spears.PIERCING_WEAPON);
		boolean hit = piercing != null && piercing.stab(player, EquipmentSlot.MAINHAND);
		float lost = before - target.getHealth();
		player.resetAttackStrengthTicker();
		return check(label, hit && cooldown > 0.99F && Math.abs(lost - expected) < 0.05F,
				"hit=" + hit + " cooldown=" + cooldown + " lost=" + lost + " expected=" + expected);
	}

	private static SpearDevCheckResult checkSettlement(ServerPlayer player, ItemStack spear) {
		prepareAttack(player, spear);
		player.setHealth(player.getMaxHealth() - 6.0F);
		float healthBefore = player.getHealth();
		float targetBefore = target.getHealth();
		PiercingWeapon piercing = spear.get(Spears.PIERCING_WEAPON);
		boolean hit = piercing != null && piercing.stab(player, EquipmentSlot.MAINHAND);
		float targetLost = targetBefore - target.getHealth();
		float healed = player.getHealth() - healthBefore;
		player.resetAttackStrengthTicker();
		return combine(
				check("settlement_primary", hit && Math.abs(targetLost - 7.0F) < 0.05F,
						"hit=" + hit + " lost=" + targetLost + " expected=7.0"),
				check("lifesteal", Math.abs(healed - 1.2F) < 0.05F,
						"healed=" + healed + " expected=1.2")
		);
	}

	private static SpearDevCheckResult checkCharge(ServerPlayer player, String label,
			ItemStack spear, float expected) {
		prepare(player);
		player.setItemSlot(EquipmentSlot.MAINHAND, spear);
		target.moveTo(player.getX(), player.getY(), player.getZ() + 3.0D, 0.0F, 0.0F);
		player.setKnownMovement(player.getViewVector(1.0F));
		Vec3 movementBefore = player.getKnownMovement();
		player.startUsingItem(InteractionHand.MAIN_HAND);
		float before = target.getHealth();
		KineticWeapon kinetic = spear.get(Spears.KINETIC_WEAPON);
		if (kinetic != null) {
			kinetic.usageTick(spear, 71950, player, EquipmentSlot.MAINHAND);
		}
		player.stopUsingItem();
		player.setKnownMovement(Vec3.ZERO);
		float lost = before - target.getHealth();
		player.resetAttackStrengthTicker();
		boolean ok = expected < 0.0F ? lost > 0.0F : Math.abs(lost - expected) < 0.05F;
		if (expected < 0.0F) {
			chargePlainLoss = lost;
		}
		return check(label, ok,
				"lost=" + lost + " expected=" + expected + " kinetic=" + (kinetic != null)
						+ " movement=" + movementBefore);
	}

	private static void prepareAttack(ServerPlayer player, ItemStack spear) {
		player.getInventory().clearContent();
		player.setItemSlot(EquipmentSlot.MAINHAND, spear);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		// 矛会逐目标结算，清掉测试路径上的其它实体，避免多次嗜血或事件污染断言。
		player.serverLevel().getEntitiesOfClass(
				Entity.class,
				player.getBoundingBox().inflate(32.0D),
				entity -> entity != player && entity != target
		).forEach(Entity::discard);
		target.setHealth(1000.0F);
		target.moveTo(player.getX(), player.getY(), player.getZ() + 2.0D, 0.0F, 0.0F);
		target.setDeltaMovement(Vec3.ZERO);
		target.invulnerableTime = 0;
		target.hurtTime = 0;
		player.setHealth(player.getMaxHealth());
	}

	private static ItemStack newSpear() {
		return new ItemStack(Spears.NETHERITE_SPEAR);
	}

	private static Holder<Enchantment> vanillaEnchantment(
			ServerPlayer player, ResourceKey<Enchantment> key) {
		return player.serverLevel().registryAccess()
				.registryOrThrow(Registries.ENCHANTMENT)
				.getHolder(key)
				.orElseThrow(() -> new IllegalStateException("Missing enchantment " + key.location()));
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
