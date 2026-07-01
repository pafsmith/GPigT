package g.pig.t.registry;

import g.pig.t.GPigT;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public final class GPigTItems {
    public static final ResourceKey<Item> GPIGT_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, GPigT.id("gpigt_spawn_egg"));

    public static final Item GPIGT_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            GPIGT_SPAWN_EGG_KEY,
            new SpawnEggItem(new Item.Properties()
                    .setId(GPIGT_SPAWN_EGG_KEY)
                    .spawnEgg(GPigTEntities.GPIGT))
    );

    public static final ResourceKey<Item> HOGZILLA_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, GPigT.id("hogzilla_spawn_egg"));

    public static final Item HOGZILLA_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            HOGZILLA_SPAWN_EGG_KEY,
            new SpawnEggItem(new Item.Properties()
                    .setId(HOGZILLA_SPAWN_EGG_KEY)
                    .spawnEgg(GPigTEntities.HOGZILLA))
    );

    public static final ResourceKey<Item> PORKZILLARY_FORCE_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, GPigT.id("porkzillary_force_spawn_egg"));

    public static final Item PORKZILLARY_FORCE_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            PORKZILLARY_FORCE_SPAWN_EGG_KEY,
            new SpawnEggItem(new Item.Properties()
                    .setId(PORKZILLARY_FORCE_SPAWN_EGG_KEY)
                    .spawnEgg(GPigTEntities.PORKZILLARY_FORCE))
    );

    private GPigTItems() {
    }

    public static void initialize() {
        // Loads the class so static registrations run during mod initialization.
    }
}
