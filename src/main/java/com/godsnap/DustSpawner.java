package com.godsnap;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Cria, no cliente, a poeira que sai do mob: de cima para baixo, com as cores dele, soprada pelo vento. */
public final class DustSpawner {

    private static final ResourceLocation FLAKE_ID =
            ResourceLocation.fromNamespaceAndPath(GodSnap.MODID, "flake");
    private static final int[] DEFAULT_PALETTE = {0x6B5E52, 0x4A4038, 0x8A8178, 0xB5ADA3};
    private static final int CREAM = 0xF2EADF;
    private static final Map<ResourceLocation, int[]> PALETTE_CACHE = new HashMap<>();

    private DustSpawner() {}

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) return;
        long now = level.getGameTime();
        TextureAtlasSprite sprite = mc.getTextureAtlas(TextureAtlas.LOCATION_PARTICLES).apply(FLAKE_ID);

        Iterator<Map.Entry<Integer, ClientState.Entry>> it = ClientState.active().entrySet().iterator();
        while (it.hasNext()) {
            ClientState.Entry en = it.next().getValue();

            // limpeza de seguranca (mundo trocado ou entrada velha)
            if (now < en.start || now - en.start > en.duration + 40) {
                it.remove();
                continue;
            }

            Entity ent = level.getEntity(en.id);
            float progress = (now - en.start) / (float) en.duration;

            if (!(ent instanceof LivingEntity) || ent.isRemoved() || progress >= 1f) {
                if (en.hasPos) {
                    burst(level, en, sprite);
                }
                it.remove();
                continue;
            }

            LivingEntity e = (LivingEntity) ent;
            en.x = e.getX();
            en.y = e.getY();
            en.z = e.getZ();
            en.width = e.getBbWidth();
            en.height = e.getBbHeight();
            en.hasPos = true;
            if (en.palette == null) en.palette = paletteFor(e);

            emit(mc, level, en, progress, sprite);
        }
    }

    /** Lascas saindo da "frente" de dissolucao, que desce da cabeca ate os pes. */
    private static void emit(Minecraft mc, ClientLevel level, ClientState.Entry en, float progress,
                             TextureAtlasSprite sprite) {
        RandomSource rnd = level.getRandom();
        double w = en.width;
        double h = en.height;

        double front = en.y + h * (1.0 - progress);
        double band = Math.max(0.3, h * 0.22);

        int base = Mth.clamp((int) (w * w * h * 10) + 6, 8, 45);
        double factor = 0.4 + 0.6 * Math.sin(Math.PI * progress);
        int count = Math.max(3, (int) (base * factor));

        for (int i = 0; i < count; i++) {
            double x = en.x + (rnd.nextDouble() - 0.5) * w * 1.1;
            double z = en.z + (rnd.nextDouble() - 0.5) * w * 1.1;
            double y = Math.max(en.y, front - rnd.nextDouble() * band);
            spawnFlake(mc, rnd, en, x, y, z, sprite);
        }
    }

    /** Ultimo punhado de poeira quando o mob termina de sumir. */
    private static void burst(ClientLevel level, ClientState.Entry en, TextureAtlasSprite sprite) {
        Minecraft mc = Minecraft.getInstance();
        RandomSource rnd = level.getRandom();
        for (int i = 0; i < 40; i++) {
            double x = en.x + (rnd.nextDouble() - 0.5) * en.width;
            double z = en.z + (rnd.nextDouble() - 0.5) * en.width;
            double y = en.y + rnd.nextDouble() * Math.min(en.height, 0.6);
            spawnFlake(mc, rnd, en, x, y, z, sprite);
        }
    }

    private static void spawnFlake(Minecraft mc, RandomSource rnd, ClientState.Entry en,
                                   double x, double y, double z, TextureAtlasSprite sprite) {
        int[] palette = en.palette != null ? en.palette : DEFAULT_PALETTE;
        int rgb = rnd.nextInt(5) == 0 ? CREAM : palette[rnd.nextInt(palette.length)];

        double k = 0.6 + rnd.nextDouble() * 0.8;
        double vx = en.windX * k + rnd.nextGaussian() * 0.012;
        double vz = en.windZ * k + rnd.nextGaussian() * 0.012;
        double vy = 0.015 + rnd.nextDouble() * 0.05;

        mc.particleEngine.add(new FlakeParticle(mc.level, x, y, z, vx, vy, vz, rgb, sprite));
    }

    /** Pega algumas cores reais da textura do mob (se nao der, usa um cinza/marrom). */
    private static int[] paletteFor(LivingEntity e) {
        Minecraft mc = Minecraft.getInstance();
        try {
            EntityRenderer<? super LivingEntity> renderer = mc.getEntityRenderDispatcher().getRenderer(e);
            ResourceLocation tex = renderer.getTextureLocation(e);
            int[] cached = PALETTE_CACHE.get(tex);
            if (cached != null) return cached;

            int[] result = DEFAULT_PALETTE;
            Optional<Resource> res = mc.getResourceManager().getResource(tex);
            if (res.isPresent()) {
                try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
                    int[] colors = new int[48];
                    int n = 0;
                    RandomSource rnd = RandomSource.create();
                    for (int tries = 0; tries < 600 && n < colors.length; tries++) {
                        int px = rnd.nextInt(img.getWidth());
                        int py = rnd.nextInt(img.getHeight());
                        int abgr = img.getPixelRGBA(px, py);
                        int a = (abgr >>> 24) & 255;
                        if (a < 60) continue;
                        int r = abgr & 255;
                        int g = (abgr >> 8) & 255;
                        int b = (abgr >> 16) & 255;
                        colors[n++] = (r << 16) | (g << 8) | b;
                    }
                    if (n > 0) {
                        result = new int[n];
                        System.arraycopy(colors, 0, result, 0, n);
                    }
                }
            }
            PALETTE_CACHE.put(tex, result);
            return result;
        } catch (Throwable t) {
            return DEFAULT_PALETTE;
        }
    }
}
