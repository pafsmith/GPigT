package g.pig.t.entity;

import g.pig.t.entity.ai.SignInteractionGoal;
import g.pig.t.names.Names;
import g.pig.t.names.Rarity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public class GPigTEntity extends Pig {
    private boolean heroswine = false;
    private int fuseTicks = -1;
    private boolean huntRequested = false;

    public GPigTEntity(EntityType<? extends Pig> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Priority 2: float (0) and panic (1) preempt the hunt; the hunt
        // outranks breeding (3) and tempting (4).
        this.goalSelector.addGoal(2, new SignInteractionGoal(this));
    }

    /**
     * Carrot and golden carrot are intercepted before the vanilla food path,
     * so they trigger a sign hunt instead of breeding. Tempting is unaffected
     * (the vanilla tempt goal has its own item predicate), and potato and
     * beetroot fall through to vanilla breeding.
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.CARROT) && !stack.is(Items.GOLDEN_CARROT)) {
            return super.mobInteract(player, hand);
        }
        if (this.level().isClientSide()) {
            return InteractionResult.CONSUME;
        }
        this.usePlayerItem(player, hand, stack);
        this.playEatingSound();
        if (SignInteractionGoal.findNearestValidSign((ServerLevel) this.level(), this) != null) {
            this.huntRequested = true;
        } else {
            // No answerable sign in range: the carrot is wasted.
            this.emitHuntFailureSmoke();
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    public boolean isHuntRequested() {
        return this.huntRequested;
    }

    public void clearHuntRequest() {
        this.huntRequested = false;
    }

    /** Black smoke puff signalling a carrot was eaten but no hunt could start. */
    public void emitHuntFailureSmoke() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + this.getBbHeight(), this.getZ(),
                    8, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnGroupData);
        if (Names.totalWeight() <= 0) {
            return data;
        }
        Names.WeightedName picked = Names.pick(this.getRandom().nextInt(Names.totalWeight()));
        if (picked == null) {
            return data;
        }
        this.setCustomName(Component.literal(picked.name()).withStyle(picked.rarity().color()));
        this.setCustomNameVisible(true);
        this.heroswine = picked.rarity() == Rarity.HEROSWINE;
        if (this.heroswine) {
            this.fuseTicks = 0;
        }
        return data;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.heroswine && this.fuseTicks >= 0) {
            this.fuseTicks++;
            if (this.fuseTicks == 1) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.CREEPER_PRIMED, this.getSoundSource(), 1.0F, 1.0F);
            }
            if (!this.level().isClientSide()) {
                this.spawnFuseParticles();
            }
            if (this.fuseTicks >= 40 && !this.level().isClientSide()) {
                this.level().explode(this, this.getX(), this.getY(), this.getZ(), 4.0F, Level.ExplosionInteraction.MOB);
                this.discard();
            }
        }
    }

    private void spawnFuseParticles() {
        int interval = Math.max(1, 6 - (this.fuseTicks / 8));
        if (this.fuseTicks % interval != 0) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) this.level();
        double offsetX = (this.random.nextDouble() - 0.5) * this.getBbWidth();
        double offsetZ = (this.random.nextDouble() - 0.5) * this.getBbWidth();
        double y = this.getY() + this.random.nextDouble() * this.getBbHeight();
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                this.getX() + offsetX, y, this.getZ() + offsetZ,
                1, 0.0, 0.0, 0.0, 0.0);
    }
}