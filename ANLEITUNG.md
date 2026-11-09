# Andrins Packs – Fabric-Mod für Minecraft 26.3 (NoRisk Client)

36 Ressourcenpakete in einer Mod, jedes einzeln an/aus schaltbar.

## .jar bauen – Variante A: GitHub (ohne Installation)
1. Neues Repository auf github.com anlegen.
2. Inhalt dieses Ordners hochladen (am einfachsten mit GitHub Desktop: Ordner hinzufügen → Publish).
3. Reiter **Actions** → Lauf "build" abwarten (~3 Min) → unten bei *Artifacts* **AndrinsPacks-jar** herunterladen.
4. Darin die Datei `andrins-packs-1.0.0.jar` nehmen.

## Variante B: lokal
JDK 25 installieren, dann in diesem Ordner: `gradlew build` (Windows) bzw. `./gradlew build`.
Die Mod liegt danach in `build/libs/andrins-packs-1.0.0.jar`.

## Installieren
- NoRisk Client → Profil 26.3 → Mods → .jar hinzufügen. **Fabric API** muss ebenfalls installiert sein.
- Im Spiel: **Optionen → Ressourcenpakete**. Alle Packs erscheinen mit "[Andrin]" davor
  und lassen sich einzeln nach rechts (an) oder links (aus) schieben. Die Reihenfolge bestimmt, welches Pack gewinnt.

## Pack-Menü (Taste K)
- Im Spiel **K** drücken → Menü mit allen Packs (2 Seiten, mit < > blättern).
- Klick auf ein Pack schaltet es **AN/AUS**, dazu gibt es "Alle an" / "Alle aus".
- Mit **Fertig** oder ESC wird übernommen und einmal neu geladen.
- Die Taste lässt sich unter Optionen → Steuerung → Tastenbelegung → "Andrins Packs" ändern.
