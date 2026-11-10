"""Baut Andrins-Packs.mrpack: Fabric API (Download von Modrinth) + Andrins Packs (overrides/mods)."""
import glob, json, os, sys, urllib.request, zipfile

version = sys.argv[1]
jar = [j for j in glob.glob("build/libs/andrins-packs-*.jar") if "sources" not in j][0]
q = 'https://api.modrinth.com/v2/project/fabric-api/version?game_versions=%5B%2226.3%22%5D&loaders=%5B%22fabric%22%5D'
req = urllib.request.Request(q, headers={"User-Agent": "andrin/andrins-packs (github build)"})
fa = json.load(urllib.request.urlopen(req))[0]
f = next(x for x in fa["files"] if x.get("primary")) if any(x.get("primary") for x in fa["files"]) else fa["files"][0]
index = {
    "formatVersion": 1, "game": "minecraft", "versionId": version,
    "name": "Andrins Packs", "summary": "Andrins Packs (36 schaltbare Ressourcenpakete, Taste K) + Fabric API für Minecraft 26.3",
    "files": [{"path": "mods/" + f["filename"], "hashes": {"sha1": f["hashes"]["sha1"], "sha512": f["hashes"]["sha512"]},
               "env": {"client": "required", "server": "optional"}, "downloads": [f["url"]], "fileSize": f["size"]}],
    "dependencies": {"minecraft": "26.3", "fabric-loader": "0.20.1"},
}
out = f"build/libs/Andrins-Packs-{version}.mrpack"
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("modrinth.index.json", json.dumps(index, indent=2, ensure_ascii=False))
    z.write(jar, "overrides/mods/" + os.path.basename(jar))
print("mrpack:", out, "fabric-api", fa["version_number"])
