package dev.vapelike.module;

import dev.vapelike.module.Module.Category;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** All modules. They only run in singleplayer (integrated server). */
public final class ModuleManager {
	public static final List<Module> MODULES = new ArrayList<>();
	private static final MinecraftClient mc = MinecraftClient.getInstance();

	private ModuleManager() {}

	public static List<Module> byCategory(Category c) {
		List<Module> out = new ArrayList<>();
		for (Module m : MODULES) if (m.category == c) out.add(m);
		return out;
	}

	public static void toggle(Module m) {
		if (!mc.isInSingleplayer()) return;
		m.toggle();
	}

	public static void tick(MinecraftClient client) {
		if (client.player == null || client.world == null) return;
		if (!client.isInSingleplayer()) {
			for (Module m : MODULES) if (m.enabled) m.toggle();
			return;
		}
		for (Module m : MODULES) if (m.enabled) m.onTick();
	}

	private static void reg(Module m) { MODULES.add(m); }

	private static boolean cooldownReady() {
		return mc.player.getAttackCooldownProgress(0.5f) >= 1.0f;
	}

	private static void attack(Entity e) {
		mc.interactionManager.attackEntity(mc.player, e);
		mc.player.swingHand(Hand.MAIN_HAND);
	}

	private static void setReach(double v) {
		EntityAttributeInstance a = mc.player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
		if (a != null && a.getBaseValue() != v) a.setBaseValue(v);
		IntegratedServer s = mc.getServer();
		if (s != null) {
			UUID id = mc.player.getUuid();
			s.execute(() -> {
				ServerPlayerEntity sp = s.getPlayerManager().getPlayer(id);
				if (sp == null) return;
				EntityAttributeInstance sa = sp.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
				if (sa != null && sa.getBaseValue() != v) sa.setBaseValue(v);
			});
		}
	}

	public static void init() {
		if (!MODULES.isEmpty()) return;

		// ---------------- COMBAT ----------------
		reg(new Module("AimAssist", Category.COMBAT) {
			final Setting range = setting("Range", 3, 8, 5);
			final Setting speed = setting("Speed", 1, 100, 35);
			final Setting fov = setting("FOV", 10, 180, 60);
			@Override protected void onTick() {
				if (mc.currentScreen != null || !mc.options.attackKey.isPressed()) return;
				LivingEntity t = target(range.value, fov.value);
				if (t == null) return;
				float[] r = rotationsTo(t.getBoundingBox().getCenter());
				float k = (float) (speed.value / 100.0 * 0.5);
				mc.player.setYaw(mc.player.getYaw() + MathHelper.wrapDegrees(r[0] - mc.player.getYaw()) * k);
				mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + (r[1] - mc.player.getPitch()) * k, -90f, 90f));
			}
		});

		reg(new Module("AutoClicker", Category.COMBAT) {
			@Override protected void onTick() {
				if (mc.currentScreen != null || !mc.options.attackKey.isPressed()) return;
				if (mc.crosshairTarget instanceof EntityHitResult hit && cooldownReady()) attack(hit.getEntity());
			}
		});

		reg(new Module("Reach", Category.COMBAT) {
			final Setting dist = setting("Distance", 3, 6, 4.5);
			@Override protected void onTick() { setReach(dist.value); }
			@Override protected void onDisable() { if (mc.player != null) setReach(3.0); }
		});

		reg(new Module("RightClicker", Category.COMBAT) {
			int ticks;
			@Override protected void onTick() {
				if (mc.currentScreen != null || !mc.options.useKey.isPressed()) { ticks = 0; return; }
				if (++ticks % 2 == 0) {
					mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
					mc.player.swingHand(Hand.MAIN_HAND);
				}
			}
		});

		reg(new Module("SilentAura", Category.COMBAT) {
			final Setting range = setting("Range", 3, 6, 4);
			@Override protected void onTick() {
				if (mc.currentScreen != null || !cooldownReady()) return;
				LivingEntity t = target(range.value, 360);
				if (t != null) attack(t);
			}
		});

		reg(new Module("Sprint", Category.COMBAT) {
			@Override protected void onTick() {
				if (mc.currentScreen == null && mc.options.forwardKey.isPressed() && !mc.player.isSneaking()
						&& !mc.player.horizontalCollision && mc.player.getHungerManager().getFoodLevel() > 6) {
					mc.player.setSprinting(true);
				}
			}
		});

		// ---------------- RENDER ----------------
		reg(new Module("ESP", Category.RENDER) {
			@Override protected void onTick() {
				for (Entity e : mc.world.getEntities()) {
					if (e instanceof LivingEntity && e != mc.player && !e.isGlowing()) e.setGlowing(true);
				}
			}
			@Override protected void onDisable() {
				if (mc.world == null) return;
				for (Entity e : mc.world.getEntities()) if (e != mc.player && e instanceof LivingEntity) e.setGlowing(false);
			}
		});

		reg(new Module("Fullbright", Category.RENDER) {
			@Override protected void onTick() {
				mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
			}
			@Override protected void onDisable() {
				if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
			}
		});

		// ---------------- UTILITY ----------------
		reg(new Module("Scaffold", Category.UTILITY) {
			@Override protected void onTick() {
				if (mc.currentScreen != null) return;
				BlockPos below = mc.player.getBlockPos().down();
				if (!mc.world.getBlockState(below).isReplaceable()) return;

				if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
					for (int i = 0; i < 9; i++) {
						if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem) {
							mc.player.getInventory().setSelectedSlot(i);
							break;
						}
					}
					if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;
				}
				for (Direction d : Direction.values()) {
					if (d == Direction.UP) continue;
					BlockPos n = below.offset(d);
					if (mc.world.getBlockState(n).isReplaceable()) continue;
					Direction face = d.getOpposite();
					Vec3d hitPos = Vec3d.ofCenter(n).add(Vec3d.of(face.getVector()).multiply(0.5));
					mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hitPos, face, n, false));
					mc.player.swingHand(Hand.MAIN_HAND);
					return;
				}
			}
		});

		reg(new Module("Fly", Category.UTILITY) {
			final Setting speed = setting("Speed", 1, 10, 3);
			private void server(boolean allow) {
				IntegratedServer s = mc.getServer();
				if (s == null || mc.player == null) return;
				UUID id = mc.player.getUuid();
				s.execute(() -> {
					ServerPlayerEntity sp = s.getPlayerManager().getPlayer(id);
					if (sp != null) sp.getAbilities().allowFlying = allow || sp.isCreative() || sp.isSpectator();
				});
			}
			@Override protected void onEnable() { server(true); }
			@Override protected void onTick() {
				mc.player.getAbilities().allowFlying = true;
				mc.player.getAbilities().flying = true;
				mc.player.getAbilities().setFlySpeed((float) (0.05 * speed.value));
			}
			@Override protected void onDisable() {
				if (mc.player == null) return;
				mc.player.getAbilities().setFlySpeed(0.05f);
				if (!mc.player.isCreative() && !mc.player.isSpectator()) {
					mc.player.getAbilities().allowFlying = false;
					mc.player.getAbilities().flying = false;
				}
				server(false);
			}
		});

		// ---------------- WORLD ----------------
		reg(new Module("Anti-AFK", Category.WORLD) {
			int ticks;
			@Override protected void onTick() {
				if (++ticks % 400 != 0) return;
				mc.player.setYaw(mc.player.getYaw() + (ticks % 800 == 0 ? 8f : -8f));
				if (mc.player.isOnGround()) {
					Vec3d v = mc.player.getVelocity();
					mc.player.setVelocity(v.x, 0.42, v.z);
				}
			}
		});

		reg(new Module("Parkour", Category.WORLD) {
			@Override protected void onTick() {
				if (mc.currentScreen != null || !mc.player.isOnGround() || mc.player.isSneaking()) return;
				if (!mc.options.forwardKey.isPressed() && !mc.options.leftKey.isPressed()
						&& !mc.options.rightKey.isPressed() && !mc.options.backKey.isPressed()) return;
				Vec3d v = mc.player.getVelocity();
				if (mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(v.x * 3, -0.5, v.z * 3))) {
					mc.player.setVelocity(v.x, 0.42, v.z);
				}
			}
		});
	}
}
