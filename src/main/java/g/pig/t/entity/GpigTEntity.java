package g.pig.t.entity;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class GpigTEntity extends Pig {
    private static final List<String> NAMES = List.of(
            "Sir Oinksalot",
            "Hammington",
            "Truffle",
            "Bacon Bit",
            "Snortimer",
            "Lord Squeals",
            "Porky",
            "Mudpuddle",
            "Curly",
            "Princess Trotter"
    );

    public GpigTEntity(EntityType<? extends Pig> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnGroupData);
        String name = NAMES.get(this.getRandom().nextInt(NAMES.size()));
        this.setCustomName(Component.literal(name));
        this.setCustomNameVisible(true);
        return data;
    }
}
