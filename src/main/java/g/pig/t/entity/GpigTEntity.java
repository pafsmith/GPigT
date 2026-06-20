package g.pig.t.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.Level;

public class GpigTEntity extends Pig {
    public GpigTEntity(EntityType<? extends Pig> entityType, Level level) {
        super(entityType, level);
    }
}
