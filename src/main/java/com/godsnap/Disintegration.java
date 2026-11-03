package com.godsnap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Logica do servidor: congela o mob, avisa os clientes (que desenham a poeira) e remove o mob no fim. */
@EventBusSubscriber(modid = GodSnap.MODID)
public class Disintegration {

    /** Duracao da animacao em ticks (50 ticks = 2,5 segundos). */
    public static final int DURATION = 50;
    private static final double MAX_RANGE = 80.0;
    /** Velocidade do vento que sopra a poeira (blocos por tick). */
    private static final double WIND_SPEED = 0.14;

    private static final List<Task> TASKS = new ArrayList<>();
    private static final Set<UUID> ACTIVE = new HashSet<>();

    private static final class Task {
        final Entity entity;
        final ServerLevel level;
        int age = 0;

        Task(Entity entity, ServerLevel level) {
            this.entity = entity;
            this.level = level;
        }
    }

    /** Escala vertical do mob (1 = inteiro, ~0 = sumiu). A parte de cima some primeiro. */
    public static float scaleFor(float progress) {
        return 1f - Mth.clamp(progress, 0f, 1f);
    }

    public static void start(ServerPlayer player, int entityId) {
        Entity target = player.level().getEntity(entityId);
        if (!(target instanceof LivingEntity living) || target instanceof Player) return;
        if (!(target.level() instanceof ServerLevel level)) return;
        if (player.distanceToSqr(target) > MAX_RANGE * MAX_RANGE) return;
        if (!ACTIVE.add(target.getUUID())) return;

        // Vento: sopra para a direita de quem estalou (como no video).
        Vec3 look = player.getLookAngle();
        double rx = -look.z;
        double rz = look.x;
        double rl = Math.sqrt(rx * rx + rz * rz);
        if (rl < 0.01) {
            rx = 1.0;
            rz = 0.0;
            rl = 1.0;
        }
        float windX = (float) (rx / rl * WIND_SPEED);
        float windZ = (float) (rz / rl * WIND_SPEED);

        if (living instanceof Mob mob) mob.setNoAi(true);
        living.setInvulnerable(true);
        living.setSilent(true);
        living.setDeltaMovement(Vec3.ZERO);

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.6f);

        PacketDistributor.sendToPlayersTrackingEntity(target,
                new DisintegratePayload(target.getId(), DURATION, windX, windZ));
        TASKS.add(new Task(target, level));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            Entity e = task.entity;
            if (e.isRemoved()) {
                ACTIVE.remove(e.getUUID());
                it.remove();
                continue;
            }

            task.age++;
            e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);

            if (task.age >= DURATION) {
                task.level.playSound(null, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(),
                        SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.8f, 0.7f);
                e.discard(); // some sem drops, sem animacao de morte: existencia apagada
                ACTIVE.remove(e.getUUID());
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TASKS.clear();
        ACTIVE.clear();
    }
}
