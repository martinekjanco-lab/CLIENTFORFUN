package dev.vapelike;

import dev.vapelike.gui.ClickGui;
import dev.vapelike.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class VapeLikeClient implements ClientModInitializer {
	private boolean prevKey;

	@Override
	public void onInitializeClient() {
		ModuleManager.init();
		// Polled with GLFW so it keeps working across input API changes. Right Shift toggles the GUI.
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private void tick(MinecraftClient mc) {
		if (mc.getWindow() != null) {
			boolean down = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
			if (down && !prevKey) {
				if (mc.currentScreen instanceof ClickGui) mc.setScreen(null);
				else if (mc.currentScreen == null && mc.player != null) mc.setScreen(new ClickGui());
			}
			prevKey = down;
		}
		ModuleManager.tick(mc);
	}
}
