package ch.andrin.packs;

/** Mögliche ID-Form eines eingebauten Fabric-Packs. */
final class PackIds {
	private PackIds() {}

	static String fabric(String folder) {
		return AndrinsPacksClient.MOD_ID + ":" + folder;
	}
}
