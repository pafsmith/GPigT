package g.pig.t.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Base for pig-derived creatures that belong to HOGZILLA's <b>Aura</b> and are
 * therefore immune to the Aura's own hazards, so HOGZILLA never harms itself or
 * its brood:
 *
 * <ul>
 *   <li>lightning — both the damage and the vanilla {@link Pig} transform into a
 *       Zombified Piglin ("pigman");</li>
 *   <li>fire (the Aura's lightning ignites the ground);</li>
 *   <li>TNT / explosions (the Salvo);</li>
 *   <li>fall damage.</li>
 * </ul>
 *
 * They are also {@link Enemy}. Subclasses layer on their own behaviour —
 * HOGZILLA's boss mechanics, the Porkzillary Forces' melee swarm, and so on.
 *
 * <p>Ordinary {@code GpigTEntity} pigs deliberately do <em>not</em> extend this:
 * they are not part of the Aura and keep the vanilla pigman transform.
 */
public abstract class AuraImmunePig extends Pig implements Enemy {

    protected AuraImmunePig(EntityType<? extends Pig> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    /** No lightning transform into a Zombified Piglin, and no lightning damage. */
    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        // creatures of the Aura are untouched by its lightning
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isAuraDamage(source)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    /** Damage sources the Aura produces, which never harm its own creatures. */
    protected static boolean isAuraDamage(DamageSource source) {
        return source.is(DamageTypes.LIGHTNING_BOLT)
                || source.is(DamageTypes.EXPLOSION)
                || source.is(DamageTypes.PLAYER_EXPLOSION)
                || source.is(DamageTypes.FALL);
    }
}
