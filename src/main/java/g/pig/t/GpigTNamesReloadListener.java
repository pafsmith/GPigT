package g.pig.t;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Loads names from every data pack on (re)load. Reads all JSON under
 * data/&lt;namespace&gt;/gpigt_names/ so multiple packs can contribute, merges
 * them, and hands the result to {@link GpigTNames}.
 *
 * <p>Fires on world load and on /reload. See:
 * https://wiki.fabricmc.net/tutorial:custom_resources
 */
public class GpigTNamesReloadListener implements SimpleSynchronousResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "gpigt_names";

    /** JSON shape: {"names": [{"name": "...", "weight": 100}, ...]}. */
    private record NamesFile(List<GpigTNames.WeightedName> names) {
    }

    @Override
    public Identifier getFabricId() {
        return GPigT.id("names");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<GpigTNames.WeightedName> merged = new ArrayList<>();
        var resources = manager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"));
        for (Resource resource : resources.values()) {
            try (Reader reader = resource.openAsReader()) {
                NamesFile file = GSON.fromJson(reader, NamesFile.class);
                if (file != null && file.names() != null) {
                    merged.addAll(file.names());
                }
            } catch (Exception e) {
                // Skip a bad/unreadable file; keep the rest.
            }
        }
        GpigTNames.setNames(merged);
    }
}
