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

		// Kategorien
		String[] cats = AndrinsPacksClient.CATEGORIES;
		int tabW = 52, gap = 2;
		int tabsX = cx - (cats.length * (tabW + gap) - gap) / 2;
		for (int i = 0; i < cats.length; i++) {
			String c = cats[i];
			Button tab = Button.builder(Component.literal(c.equals(category) ? "§d§l" + c : c), b -> {
				category = c;
				scroll = 0;
				rebuildWidgets();
			}).bounds(tabsX + i * (tabW + gap), 42, tabW, 18).build();
			this.addRenderableWidget(tab);
		}

		listTop = 66;
		listBottom = this.height - 34;
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
			}).bounds(listLeft + ICON + 6, y, LIST_W - ICON - 6, ROW_H - 4).build();
			row.active = id != null;
			this.addRenderableWidget(row);
		}

		// Fusszeile
		int by = this.height - 26;
		this.addRenderableWidget(Button.builder(Component.literal("§aAlle an"), b -> setAll(true))
				.bounds(cx - 160, by, 75, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("§cAlle aus"), b -> setAll(false))
				.bounds(cx - 80, by, 75, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("§lFertig"), b -> this.onClose())
				.bounds(cx + 5, by, 155, 20).build());

		this.setInitialFocus(search);
	}

	private Component rowLabel(String[] p) {
		if (p[3] == null) return Component.literal("§8" + p[1] + " (fehlt)");
		boolean on = wanted.contains(p[3]);
		return Component.literal((on ? "§a● AN   §f" : "§c○ AUS  §7") + p[1] + " §8[" + p[2] + "]");
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
		this.minecraft.gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}
}
