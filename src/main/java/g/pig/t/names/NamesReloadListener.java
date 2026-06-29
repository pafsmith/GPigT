package g.pig.t.names;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import g.pig.t.GPigT;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Loads names from every data pack on (re)load. Reads all JSON under
 * data/&lt;namespace&gt;/gpigt_names/ so multiple packs can contribute, merges
 * them, and hands the result to {@link Names}.
 *
 * <p>Datapack JSON is user-editable, so bad data is logged and skipped rather
 * than crashing the server: a malformed file is dropped whole (can't parse),
 * and an entry with an unknown rarity is dropped on its own.
 *
 * <p>Fires on world load and on /reload. See:
 * https://wiki.fabricmc.net/tutorial:custom_resources
 */
public class NamesReloadListener implements SimpleSynchronousResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "gpigt_names";

    /** Raw JSON shape: {"names": [{"name": "...", "rarity": "COMMON"}, ...]}. */
    private record RawName(String name, String rarity) {
    }

    private record NamesFile(List<RawName> names) {
    }

    @Override
    public Identifier getFabricId() {
        return GPigT.id("names");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<Names.WeightedName> merged = new ArrayList<>();
        var resources = manager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier source = entry.getKey();
            NamesFile file;
            try (Reader reader = entry.getValue().openAsReader()) {
                file = GSON.fromJson(reader, NamesFile.class);
            } catch (Exception e) {
                GPigT.LOGGER.warn("Skipping unreadable GpigT names file {}", source, e);
                continue;
            }
            if (file == null || file.names() == null) {
                continue;
            }
            for (RawName raw : file.names()) {
                Rarity rarity = Rarity.fromString(raw.rarity());
                if (rarity == null) {
                    GPigT.LOGGER.warn("Skipping name '{}' in {}: unknown rarity '{}'",
                            raw.name(), source, raw.rarity());
                    continue;
                }
                merged.add(new Names.WeightedName(raw.name(), rarity));
            }
        }
        Names.setNames(merged);
    }
}
