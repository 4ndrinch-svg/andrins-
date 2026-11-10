package ch.andrin.packs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

/**
 * Andrins Pack-Menü: Suche, Kategorien, Pack-Bilder und Scrollliste.
 * Änderungen werden beim Schliessen übernommen und einmal neu geladen.
 */
public class PackToggleScreen extends Screen {
	private static final int ROW_H = 24;
	private static final int ICON = 20;
	private static final int LIST_W = 320;

	private final Screen parent;
	private final Set<String> wanted = new LinkedHashSet<>();
	private Set<String> original;

	private EditBox search;
	private String query = "";
	private String category = "Alle";
	private double scroll = 0;

	/** sichtbare Zeilen: {ordner, name, kategorie, packId} */
	private final List<String[]> visible = new ArrayList<>();
	private int listTop, listBottom, listLeft;

	public PackToggleScreen(Screen parent) {
		super(Component.literal("Andrins Packs"));
		this.parent = parent;
	}

	private static String findId(PackRepository repo, String folder) {
		String suffix = AndrinsPacksClient.MOD_ID + ":" + folder;
		for (Pack pack : repo.getAvailablePacks()) {
			String id = pack.getId();
			if (id.equals(suffix) || id.endsWith(suffix) || id.endsWith("/" + folder)) return id;
		}
		return null;
	}

	private void filter(PackRepository repo) {
		visible.clear();
		String q = query.toLowerCase(Locale.ROOT).trim();
		for (String[] p : AndrinsPacksClient.PACKS) {
			if (!category.equals("Alle") && !category.equals(p[2])) continue;
			if (!q.isEmpty() && !p[1].toLowerCase(Locale.ROOT).contains(q)) continue;
			visible.add(new String[] {p[0], p[1], p[2], findId(repo, p[0])});
		}
	}

	private int maxScroll() {
		return Math.max(0, visible.size() * ROW_H - (listBottom - listTop));
	}

	@Override
	protected void init() {
		PackRepository repo = this.minecraft.getResourcePackRepository();
		if (original == null) {
			original = new LinkedHashSet<>(repo.getSelectedIds());
			wanted.addAll(original);
		}
		int cx = this.width / 2;
		listLeft = cx - LIST_W / 2;

		// Suche
		if (search == null) {
			search = new EditBox(this.font, 0, 0, 200, 18, Component.literal("Suche"));
			search.setHint(Component.literal("§7Pack suchen..."));
			search.setResponder(text -> {
				query = text;
				scroll = 0;
				rebuildWidgets();
			});
		}
		search.setX(cx - 100);
		search.setY(20);
		this.addRenderableWidget(search);

		// Taste ändern
		this.addRenderableWidget(Button.builder(
				Component.literal("§eTaste: §f").append(AndrinsPacksClient.openMenuKey.getTranslatedKeyMessage()),
				b -> this.minecraft.gui.setScreen(new KeyBindsScreen(this, this.minecraft.options)))
				.bounds(this.width - 105, 4, 100, 18).build());

		// Sicht-Einstellungen (was G anzeigt)
		this.addRenderableWidget(Button.builder(Component.literal("§dSicht-Liste"),
				b -> this.minecraft.gui.setScreen(new EspSettingsScreen(this)))
				.bounds(5, 4, 80, 18).build());

		// Kategorien
		String[] cats = AndrinsPacksClient.CATEGORIES;
		int tabW = 64, gap = 2;
		int tabsX = cx - (cats.length * (tabW + gap) - gap) / 2;
		for (int i = 0; i < cats.length; i++) {
			String c = cats[i];
			int total = 0, active = 0;
			for (String[] p : AndrinsPacksClient.PACKS) {
				if (!c.equals("Alle") && !c.equals(p[2])) continue;
				total++;
				String pid = findId(repo, p[0]);
				if (pid != null && wanted.contains(pid)) active++;
			}
			String tabText = (c.equals(category) ? "§d§l" : "§f") + c + " §7" + active + "/" + total;
			Button tab = Button.builder(Component.literal(tabText), b -> {
				category = c;
				scroll = 0;
				rebuildWidgets();
			}).bounds(tabsX + i * (tabW + gap), 42, tabW, 18).build();
			this.addRenderableWidget(tab);
		}

		listTop = 66;
		listBottom = this.height - 44;
		filter(repo);
		scroll = Math.max(0, Math.min(scroll, maxScroll()));

		// Zeilen (nur sichtbare als Buttons)
		for (int i = 0; i < visible.size(); i++) {
			int y = listTop + i * ROW_H - (int) scroll;
			if (y < listTop || y + ROW_H - 4 > listBottom) continue;
			String[] p = visible.get(i);
			String id = p[3];
			Button row = Button.builder(rowLabel(p), b -> {
				if (id == null) return;
				if (wanted.contains(id)) wanted.remove(id); else wanted.add(id);
				b.setMessage(rowLabel(p));
			}).bounds(listLeft + ICON + 6, y, LIST_W - ICON - 6 - 34, ROW_H - 4).build();
			row.active = id != null;
			this.addRenderableWidget(row);
		}

		// Fusszeile
		int by = this.height - 26;
		this.addRenderableWidget(Button.builder(Component.literal("§aAlle an"), b -> setAll(true))
				.bounds(cx - 210, by, 60, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("§cAlle aus"), b -> setAll(false))
				.bounds(cx - 148, by, 60, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal((InfoHud.hudEnabled ? "§a" : "§7") + "HUD"), b -> {
			InfoHud.hudEnabled = !InfoHud.hudEnabled;
			rebuildWidgets();
		}).bounds(cx - 84, by, 50, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal((TrollVision.enabled ? "§a" : "§7") + "Sicht"), b -> {
			TrollVision.enabled = !TrollVision.enabled;
			rebuildWidgets();
		}).bounds(cx - 32, by, 50, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("§lFertig"), b -> this.onClose())
				.bounds(cx + 22, by, 188, 20).build());

		this.setInitialFocus(search);
	}

	private Component rowLabel(String[] p) {
		if (p[3] == null) return Component.literal("§8" + p[1] + " (fehlt)");
		boolean on = wanted.contains(p[3]);
		return Component.literal((on ? "§f" : "§7") + p[1]);
	}

	/** Schaltet alle aktuell angezeigten (gefilterten) Packs. */
	private void setAll(boolean on) {
		for (String[] p : visible) {
			if (p[3] == null) continue;
			if (on) wanted.add(p[3]); else wanted.remove(p[3]);
		}
		rebuildWidgets();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (mouseY >= listTop && mouseY <= listBottom) {
			double old = scroll;
			scroll = Math.max(0, Math.min(scroll - scrollY * ROW_H, maxScroll()));
			if (scroll != old) rebuildWidgets();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);

		// Schalter rechts in jeder Zeile + Beschreibung des Packs unter der Maus
		String hoverDesc = null;
		for (int i = 0; i < visible.size(); i++) {
			int y = listTop + i * ROW_H - (int) scroll;
			if (y < listTop || y + ROW_H - 4 > listBottom) continue;
			String[] p = visible.get(i);
			boolean on = p[3] != null && wanted.contains(p[3]);
			int sx = listLeft + LIST_W - 30, sy = y + 4;
			g.fill(sx, sy, sx + 26, sy + 12, on ? 0xFF2E8B3A : 0xFF5A2A2A);
			g.fill(sx + (on ? 14 : 1), sy + 1, sx + (on ? 25 : 12), sy + 11, on ? 0xFFB6FFB0 : 0xFFAAAAAA);
			if (mouseX >= listLeft && mouseX <= listLeft + LIST_W && mouseY >= y && mouseY < y + ROW_H - 4) {
				hoverDesc = "§d" + p[1] + " §8[" + p[2] + "]";
				if (p[3] != null) {
					var pack = this.minecraft.getResourcePackRepository().getPack(p[3]);
					if (pack != null) hoverDesc += "  §7" + pack.getDescription().getString().replace("\n", " ");
				}
			}
		}
		if (hoverDesc != null) {
			String d = this.font.plainSubstrByWidth(hoverDesc, this.width - 20);
			g.fill(0, listBottom + 1, this.width, listBottom + 12, 0xC0000000);
			g.centeredText(this.font, Component.literal(d), this.width / 2, listBottom + 3, 0xFFFFFFFF);
		}

		g.centeredText(this.font, Component.literal("§d§lAndrins Packs §7(" + wanted.stream().filter(id -> id.contains(AndrinsPacksClient.MOD_ID)).count() + " aktiv)"), this.width / 2, 7, 0xFFFFFFFF);

		// Pack-Bilder neben den Zeilen
		for (int i = 0; i < visible.size(); i++) {
			int y = listTop + i * ROW_H - (int) scroll;
			if (y < listTop || y + ROW_H - 4 > listBottom) continue;
			String[] p = visible.get(i);
			Identifier tex = Identifier.fromNamespaceAndPath(AndrinsPacksClient.MOD_ID, "textures/gui/icons/" + p[0] + ".png");
			g.blit(RenderPipelines.GUI_TEXTURED, tex, listLeft, y, 0f, 0f, ICON, ICON, ICON, ICON);
			if (p[3] != null && wanted.contains(p[3])) {
				g.fill(listLeft - 3, y, listLeft - 1, y + ICON, 0xFF55FF55);
			}
		}

		// Scrollbalken
		int max = maxScroll();
		if (max > 0) {
			int x = listLeft + LIST_W + 4;
			int h = listBottom - listTop;
			int barH = Math.max(16, h * h / (h + max));
			int barY = listTop + (int) ((h - barH) * (scroll / max));
			g.fill(x, listTop, x + 4, listBottom, 0x66000000);
			g.fill(x, barY, x + 4, barY + barH, 0xFFB050FF);
		}

		if (visible.isEmpty()) {
			g.centeredText(this.font, Component.literal("§7Keine Packs gefunden"), this.width / 2, listTop + 20, 0xFFFFFFFF);
		}
	}

	@Override
	public void onClose() {
		if (original != null && !wanted.equals(original)) {
			PackRepository repo = this.minecraft.getResourcePackRepository();
			List<String> order = new ArrayList<>();
			for (String id : original) if (wanted.contains(id)) order.add(id);
			for (String id : wanted) if (!order.contains(id)) order.add(id);
			repo.setSelected(order);
			this.minecraft.options.updateResourcePacks(repo);
		}
		PackMemory.save(this.minecraft);
		this.minecraft.gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}
}
