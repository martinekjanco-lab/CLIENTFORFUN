package dev.vapelike.gui;

import dev.vapelike.module.Module;
import dev.vapelike.module.Module.Category;
import dev.vapelike.module.ModuleManager;
import dev.vapelike.module.Setting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Vape V4 style click GUI: left sidebar + draggable category panels.
 * Mouse input is polled via GLFW (no dependency on the changing Screen input signatures).
 * Left click: toggle module / drag header. Right click or the dots: expand settings.
 */
public class ClickGui extends Screen {
	private static final int ACCENT = 0xFF30D9A4;
	private static final int ACCENT_BG = 0xFF1C8C6A;
	private static final int BG = 0xF0121212;
	private static final int HEADER = 0xF01A1A1A;
	private static final int TEXT = 0xFFB4B4B4;
	private static final int DIM = 0xFF6A6A6A;
	private static final int HOVER = 0x1CFFFFFF;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int PW = 118, ROW = 20, SROW = 18, HEAD = 22;

	private static class Panel {
		final Category cat;
		int x, y;
		boolean open = true, visible = true;
		Panel(Category c, int x, int y) { cat = c; this.x = x; this.y = y; }
	}

	private static final List<Panel> PANELS = new ArrayList<>();
	private Panel dragging;
	private int dragDx, dragDy;
	private Setting activeSlider;
	private int sliderX, sliderW;
	private boolean prevL, prevR;

	public ClickGui() { super(Text.literal("VapeLike")); }

	@Override public boolean shouldPause() { return false; }

	@Override
	protected void init() {
		if (PANELS.isEmpty()) {
			int x = 8 + PW + 14;
			for (Category c : Category.values()) {
				PANELS.add(new Panel(c, x, 8));
				x += PW + 8;
			}
		}
	}

	private static boolean in(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private static void rrect(DrawContext c, int x1, int y1, int x2, int y2, int col) {
		c.fill(x1 + 1, y1, x2 - 1, y2, col);
		c.fill(x1, y1 + 1, x1 + 1, y2 - 1, col);
		c.fill(x2 - 1, y1 + 1, x2, y2 - 1, col);
	}

	@Override
	public void render(DrawContext c, int mx, int my, float delta) {
		super.render(c, mx, my, delta); // vanilla blurred background, like Vape

		long h = client.getWindow().getHandle();
		boolean l = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean r = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		boolean lClick = l && !prevL, rClick = r && !prevR;
		prevL = l; prevR = r;
		if (!l) { dragging = null; activeSlider = null; }

		if (dragging != null) { dragging.x = mx - dragDx; dragging.y = my - dragDy; }
		if (activeSlider != null) {
			double t = Math.max(0, Math.min(1, (mx - sliderX) / (double) sliderW));
			double v = activeSlider.min + t * (activeSlider.max - activeSlider.min);
			activeSlider.value = Math.round(v * 10.0) / 10.0;
		}

		sidebar(c, mx, my, lClick);
		search(c);
		for (Panel p : PANELS) if (p.visible) panel(c, p, mx, my, lClick, rClick);

		if (!client.isInSingleplayer()) {
			c.drawText(textRenderer, "Singleplayer only - modules disabled", 8, height - 14, 0xFFFF5555, true);
		}
	}

	// ------------------------------------------------------------ sidebar
	private void sidebar(DrawContext c, int mx, int my, boolean click) {
		int x = 8, y = 8;
		int total = HEAD + 6 * ROW + 14 + 3 * ROW + 18;
		rrect(c, x, y, x + PW, y + total, BG);
		rrect(c, x, y, x + PW, y + HEAD, HEADER);
		c.drawText(textRenderer, "VAPE", x + 8, y + 7, WHITE, true);
		c.drawText(textRenderer, "V4", x + 8 + textRenderer.getWidth("VAPE") + 3, y + 7, ACCENT, false);
		c.drawText(textRenderer, "*", x + PW - 13, y + 7, DIM, false);

		int cy = y + HEAD;
		String[] names = {"Combat", "Render", "Utility", "World", "Inventory", "Minigames"};
		for (int i = 0; i < names.length; i++) {
			boolean real = i < 4;
			Panel p = real ? PANELS.get(i) : null;
			boolean active = real && p.visible;
			boolean hov = in(mx, my, x, cy, PW, ROW);
			if (hov) c.fill(x, cy, x + PW, cy + ROW, HOVER);
			int col = active ? ACCENT : (real ? TEXT : DIM);
			c.fill(x + 8, cy + 7, x + 14, cy + 13, col);
			c.drawText(textRenderer, names[i], x + 20, cy + 6, active ? ACCENT : TEXT, false);
			c.drawText(textRenderer, ">", x + PW - 12, cy + 6, DIM, false);
			if (real && click && hov) p.visible = !p.visible;
			cy += ROW;
		}
		c.fill(x, cy, x + PW, cy + 14, 0xFF0E0E0E);
		c.drawText(textRenderer, "MISC", x + 8, cy + 3, 0xFF4A4A4A, false);
		cy += 14;
		String[] misc = {"Friends", "Profiles", "Macros"};
		for (String m : misc) {
			if (in(mx, my, x, cy, PW, ROW)) c.fill(x, cy, x + PW, cy + ROW, HOVER);
			c.drawText(textRenderer, m, x + 8, cy + 6, TEXT, false);
			if (m.equals("Profiles")) {
				int pw = textRenderer.getWidth("default") + 8;
				rrect(c, x + PW - pw - 8, cy + 4, x + PW - 8, cy + 16, 0xFF2A2A2A);
				c.drawText(textRenderer, "default", x + PW - pw - 4, cy + 6, DIM, false);
			} else {
				c.drawText(textRenderer, ">", x + PW - 12, cy + 6, DIM, false);
			}
			cy += ROW;
		}
		c.fill(x, cy, x + PW, cy + 18, 0xFF0E0E0E);
		c.fill(x + 8, cy + 6, x + 12, cy + 12, DIM);
		c.fill(x + PW - 22, cy + 6, x + PW - 18, cy + 12, DIM);
		c.fill(x + PW - 12, cy + 6, x + PW - 8, cy + 12, DIM);
	}

	private void search(DrawContext c) {
		int w = Math.max(110, (int) (width * 0.17));
		int x = (int) (width * 0.645);
		rrect(c, x, 0, x + w, 24, BG);
		c.fill(x + 8, 8, x + 22, 16, ACCENT);
		c.drawText(textRenderer, "|", x + 30, 8, DIM, false);
		c.drawText(textRenderer, "o", x + w - 14, 8, DIM, false);
	}

	// ------------------------------------------------------------ panels
	private void panel(DrawContext c, Panel p, int mx, int my, boolean lClick, boolean rClick) {
		List<Module> mods = ModuleManager.byCategory(p.cat);
		int bodyH = 0;
		if (p.open) {
			for (Module m : mods) {
				bodyH += ROW;
				if (m.expanded) bodyH += m.settings.size() * SROW;
			}
		}
		rrect(c, p.x, p.y, p.x + PW, p.y + HEAD + bodyH + (p.open ? 4 : 0), BG);
		rrect(c, p.x, p.y, p.x + PW, p.y + HEAD, HEADER);

		boolean hHov = in(mx, my, p.x, p.y, PW, HEAD);
		c.fill(p.x + 8, p.y + 8, p.x + 14, p.y + 14, TEXT);
		c.drawText(textRenderer, p.cat.label, p.x + 20, p.y + 7, WHITE, false);
		c.drawText(textRenderer, p.open ? "^" : "v", p.x + PW - 12, p.y + 7, DIM, false);
		if (hHov && lClick) {
			if (mx >= p.x + PW - 22) p.open = !p.open;
			else { dragging = p; dragDx = mx - p.x; dragDy = my - p.y; }
		}
		if (hHov && rClick) p.open = !p.open;
		if (!p.open) return;

		int cy = p.y + HEAD;
		for (Module m : mods) {
			boolean hov = in(mx, my, p.x, cy, PW, ROW);
			if (m.enabled) c.fill(p.x, cy, p.x + PW, cy + ROW, ACCENT_BG);
			else if (hov) c.fill(p.x, cy, p.x + PW, cy + ROW, HOVER);
			c.drawText(textRenderer, m.name, p.x + 8, cy + 6, m.enabled ? WHITE : TEXT, false);
			int dc = m.enabled ? WHITE : DIM;
			for (int k = 0; k < 3; k++) c.fill(p.x + PW - 10, cy + 5 + k * 4, p.x + PW - 8, cy + 7 + k * 4, dc);

			if (hov && dragging == null) {
				if (lClick) {
					if (mx >= p.x + PW - 20) m.expanded = !m.expanded;
					else ModuleManager.toggle(m);
				} else if (rClick) m.expanded = !m.expanded;
			}
			cy += ROW;

			if (m.expanded) {
				for (Setting s : m.settings) {
					c.fill(p.x, cy, p.x + PW, cy + SROW, 0x40000000);
					c.drawText(textRenderer, s.name, p.x + 8, cy + 2, DIM, false);
					String val = String.format("%.1f", s.value);
					c.drawText(textRenderer, val, p.x + PW - 8 - textRenderer.getWidth(val), cy + 2, TEXT, false);
					int tx = p.x + 8, tw = PW - 16, ty = cy + SROW - 5;
					c.fill(tx, ty, tx + tw, ty + 2, 0xFF2C2C2C);
					int fw = (int) (tw * (s.value - s.min) / (s.max - s.min));
					c.fill(tx, ty, tx + fw, ty + 2, ACCENT);
					c.fill(tx + fw - 1, ty - 1, tx + fw + 1, ty + 3, WHITE);
					if (lClick && in(mx, my, p.x, cy, PW, SROW) && dragging == null) {
						activeSlider = s; sliderX = tx; sliderW = tw;
					}
					cy += SROW;
				}
			}
		}
	}
}
