package de.hiveinvasion.client.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

/** Bluttropfen: fällt mit Schwerkraft und bleibt kurz am Boden liegen. */
public class BloodParticle extends SpriteBillboardParticle {
    private final SpriteProvider sprites;
    private final boolean glowing;

    protected BloodParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz,
                            SpriteProvider sprites, float r, float g, float b, boolean glowing, float gravity) {
        super(world, x, y, z, vx, vy, vz);
        this.velocityX = vx; this.velocityY = vy; this.velocityZ = vz;
        this.sprites = sprites;
        this.glowing = glowing;
        this.gravityStrength = gravity;
        this.velocityMultiplier = 0.96f;
        this.maxAge = 25 + random.nextInt(25);
        this.scale = 0.08f + random.nextFloat() * 0.1f;
        float shade = 0.8f + random.nextFloat() * 0.2f;
        setColor(r * shade, g * shade, b * shade);
        setSpriteForAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteForAge(sprites);
        if (onGround) { velocityX *= 0.3; velocityZ *= 0.3; }
        if (age > maxAge - 8) alpha = (maxAge - age) / 8f;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getBrightness(float tint) {
        return glowing ? 0xF000F0 : super.getBrightness(tint);
    }

    public record Factory(SpriteProvider sprites, float r, float g, float b, boolean glowing, float gravity)
            implements ParticleFactory<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world, double x, double y, double z,
                                       double vx, double vy, double vz) {
            BloodParticle p = new BloodParticle(world, x, y, z, vx, vy, vz, sprites, r, g, b, glowing, gravity);
            if (gravity == 0) { p.maxAge = 8; p.scale *= 1.6f; }
            return p;
        }
    }
}
