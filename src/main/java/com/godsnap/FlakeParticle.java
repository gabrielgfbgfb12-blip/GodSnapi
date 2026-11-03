package com.godsnap;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

/** Uma lasca de poeira: quadradinho colorido que gira, e e levado pelo vento. */
public class FlakeParticle extends TextureSheetParticle {

    private final float spin;

    public FlakeParticle(ClientLevel level, double x, double y, double z,
                         double vx, double vy, double vz, int rgb, TextureAtlasSprite sprite) {
        super(level, x, y, z);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.setSprite(sprite);

        this.lifetime = 45 + this.random.nextInt(45);
        this.quadSize = 0.03f + this.random.nextFloat() * 0.05f;
        this.hasPhysics = false;
        this.friction = 0.98f;

        this.rCol = ((rgb >> 16) & 255) / 255f;
        this.gCol = ((rgb >> 8) & 255) / 255f;
        this.bCol = (rgb & 255) / 255f;
        this.alpha = 1f;

        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.spin = (this.random.nextFloat() - 0.5f) * 0.4f;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.oRoll = this.roll;
        this.roll += this.spin;

        // sobe um pouco no comeco e depois vai caindo devagar
        this.yd += (this.age < this.lifetime * 0.45f) ? 0.0015 : -0.004;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.zd *= this.friction;
        this.yd *= 0.98;

        float t = this.age / (float) this.lifetime;
        this.alpha = t > 0.6f ? 1f - (t - 0.6f) / 0.4f : 1f;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT; // as cores ficam "de verdade", de dia e na caverna
    }
}
