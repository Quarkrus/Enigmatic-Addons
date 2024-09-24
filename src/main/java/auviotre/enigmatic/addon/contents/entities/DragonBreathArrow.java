package auviotre.enigmatic.addon.contents.entities;

import auviotre.enigmatic.addon.registries.EnigmaticAddonEffects;
import auviotre.enigmatic.addon.registries.EnigmaticAddonEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DragonBreathArrow extends AbstractArrow {
    public DragonBreathArrow(EntityType<? extends DragonBreathArrow> type, Level world) {
        super(type, world);
        this.setBaseDamage(this.getBaseDamage() * 2);
    }

    public DragonBreathArrow(LivingEntity entity, Level world) {
        super(EnigmaticAddonEntities.DRAGON_BREATH_ARROW, entity, world);
    }

    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            Vec3 vec3 = this.getDeltaMovement();
            double dx = vec3.x;
            double dy = vec3.y;
            double dz = vec3.z;
            double length = vec3.length() * 1.25D;
            for (int i = 0; i < length; ++i) {
                this.level().addParticle(ParticleTypes.DRAGON_BREATH, this.getRandomX(0.0F) + dx * (double) i / length, this.getRandomY() + dy * (double) i / length, this.getRandomZ(0.0F) + dz * (double) i / length, -dx * 0.1, -dy * 0.1, -dz * 0.1);
            }
            if (!this.isNoGravity() && !this.isNoPhysics()) {
                Vec3 vec34 = this.getDeltaMovement();
                this.setDeltaMovement(vec34.x, vec34.y + 0.02, vec34.z);
            }
        } else if (this.inGround) this.summonAreaEffect();
    }

    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide && hitResult.getType() == HitResult.Type.ENTITY && !this.ownedBy(((EntityHitResult) hitResult).getEntity())) {
            this.setPos(this.getPosition(0.0F).add(this.getDeltaMovement().scale(0.5F)));
            this.summonAreaEffect();
        }
    }

    private void summonAreaEffect() {
        float speed = (float) this.getDeltaMovement().length();
        float dmg = Mth.ceil(Mth.clamp((double) speed * this.getBaseDamage(), 0.0, 2.147483647E9)) / 4.0F;
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0, 2.0, 4.0));
        AreaEffectCloud effectCloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity livingOwner) effectCloud.setOwner(livingOwner);

        effectCloud.setParticle(ParticleTypes.DRAGON_BREATH);
        effectCloud.setRadius(1.5F);
        effectCloud.setDuration(100);
        effectCloud.setRadiusOnUse(0.1F);
        effectCloud.setDurationOnUse(-1);
        effectCloud.setWaitTime(1);
        effectCloud.addEffect(new MobEffectInstance(EnigmaticAddonEffects.DRAGON_BREATH_EFFECT, 1, Mth.floor(dmg)));
        effectCloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 39, 1));
        if (!entities.isEmpty()) {
            for (LivingEntity entity : entities) {
                if (this.distanceToSqr(entity) < 12.0 && entity != this.getOwner()) {
                    effectCloud.setPos(entity.getX(), entity.getY(), entity.getZ());
                    break;
                }
            }
        }
        this.level().addFreshEntity(effectCloud);
        this.discard();
    }

    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }
}
