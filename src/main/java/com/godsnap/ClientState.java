package com.godsnap;

import java.util.HashMap;
import java.util.Map;

/** Guarda, no cliente, quais entidades estao virando poeira (so usado na thread do cliente). */
public final class ClientState {

    public static final class Entry {
        public final int id;
        public final long start;
        public final int duration;
        public final float windX;
        public final float windZ;
        // ultima posicao/tamanho conhecidos (para a explosao final de poeira)
        public double x, y, z;
        public float width = 0.6f;
        public float height = 1.8f;
        public boolean hasPos = false;
        public int[] palette; // cores (RGB) tiradas da textura do mob

        Entry(int id, long start, int duration, float windX, float windZ) {
            this.id = id;
            this.start = start;
            this.duration = duration;
            this.windX = windX;
            this.windZ = windZ;
        }
    }

    private static final Map<Integer, Entry> ACTIVE = new HashMap<>();

    private ClientState() {}

    public static void start(int entityId, int duration, long gameTime, float windX, float windZ) {
        ACTIVE.put(entityId, new Entry(entityId, gameTime, duration, windX, windZ));
    }

    public static Map<Integer, Entry> active() {
        return ACTIVE;
    }

    /** Progresso de 0 a 1, ou -1 se a entidade nao esta virando poeira. */
    public static float progress(int entityId, long gameTime, float partialTick) {
        Entry en = ACTIVE.get(entityId);
        if (en == null) return -1f;
        float p = (gameTime + partialTick - en.start) / (float) en.duration;
        return Math.max(0f, Math.min(1f, p));
    }
}
