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

public class GpigTEntity extends Pig {
    public GpigTEntity(EntityType<? extends Pig> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnGroupData);
        if (GpigTNames.totalWeight() <= 0) {
            return data;
        }
        String name = GpigTNames.pick(this.getRandom().nextInt(GpigTNames.totalWeight()));
        if (name == null) {
            return data;
        }
        this.setCustomName(Component.literal(name));
        this.setCustomNameVisible(true);
        return data;
    }
}
