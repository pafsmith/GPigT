package g.pig.t.registry;

import g.pig.t.GPigT;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
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

    private GPigTItems() {
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(output -> output.accept(GPIGT_SPAWN_EGG));
    }
}
