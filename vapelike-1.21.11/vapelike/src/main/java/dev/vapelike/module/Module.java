package dev.vapelike.module;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public abstract class Module {
	public enum Category {
		COMBAT("Combat"), RENDER("Render"), UTILITY("Utility"), WORLD("World");
		public final String label;
		Category(String label) { this.label = label; }
	}

	protected static final MinecraftClient mc = MinecraftClient.getInstance();

	public final String name;
	public final Category category;
	public final List<Setting> settings = new ArrayList<>();
	public boolean enabled;
	public boolean expanded;

	protected Module(String name, Category category) {
		this.name = name;
		this.category = category;
	}

	protected Setting setting(String n, double min, double max, double def) {
		Setting s = new Setting(n, min, max, def);
		settings.add(s);
		return s;
	}

	public void toggle() {
		enabled = !enabled;
		if (enabled) onEnable(); else onDisable();
	}

	protected void onEnable() {}
	protected void onDisable() {}
	protected abstract void onTick();

	// ---- helpers ----
	protected static float[] rotationsTo(Vec3d target) {
		Vec3d eye = mc.player.getEyePos();
		double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		return new float[]{yaw, pitch};
	}

	/** Closest living target in range whose angle from the crosshair is within fov degrees. */
	protected static LivingEntity target(double range, double fov) {
		LivingEntity best = null;
		double bestScore = Double.MAX_VALUE;
		for (Entity e : mc.world.getEntities()) {
			if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive() || e instanceof ArmorStandEntity) continue;
			double dist = mc.player.distanceTo(le);
			if (dist > range) continue;
			float[] rot = rotationsTo(le.getBoundingBox().getCenter());
			double diff = Math.abs(MathHelper.wrapDegrees(rot[0] - mc.player.getYaw()));
			if (diff > fov) continue;
			double score = diff + dist;
			if (score < bestScore) { bestScore = score; best = le; }
		}
		return best;
	}
}
