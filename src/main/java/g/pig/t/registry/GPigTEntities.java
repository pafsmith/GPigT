package g.pig.t.registry;

import g.pig.t.GPigT;
import g.pig.t.entity.GpigTEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.pig.Pig;

public final class GPigTEntities {
    public static final ResourceKey<EntityType<?>> GPIGT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, GPigT.id("gpigt"));

    public static final EntityType<GpigTEntity> GPIGT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            GPIGT_KEY,
            EntityType.Builder.of(GpigTEntity::new, MobCategory.CREATURE)
                    .sized(0.9F, 0.9F)
                    .build(GPIGT_KEY)
    );

    private GPigTEntities() {
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(GPIGT, Pig.createAttributes());
    }
}
