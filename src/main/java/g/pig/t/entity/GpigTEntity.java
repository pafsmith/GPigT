package g.pig.t.entity;

import g.pig.t.GpigTNames;
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
    public GpigTEntity(EntityType<? extends Pig> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnGroupData);
        if (GpigTNames.TOTAL_WEIGHT <= 0) {
            return data;
        }
        String name = GpigTNames.pick(this.getRandom().nextInt(GpigTNames.TOTAL_WEIGHT));
        if (name == null) {
            return data;
        }
        this.setCustomName(Component.literal(name));
        this.setCustomNameVisible(true);
        return data;
    }
}
