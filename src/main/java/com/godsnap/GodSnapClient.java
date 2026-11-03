package com.godsnap;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class GodSnapClient {

    /** Alcance maximo do estalo, em blocos. */
    private static final double RANGE = 64.0;

    /** Tecla padrao: G (mude em Controles > God Snap). */
    public static final KeyMapping SNAP_KEY = new KeyMapping(
            "key.godsnap.snap", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.godsnap");

    @EventBusSubscriber(modid = GodSnap.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(SNAP_KEY);
        }
    }

    @EventBusSubscriber(modid = GodSnap.MODID, value = Dist.CLIENT)
    public static class GameEvents {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            DustSpawner.tick(mc);

            while (SNAP_KEY.consumeClick()) {
                LivingEntity target = findTarget(mc.player);
                if (target != null) {
                    PacketDistributor.sendToServer(new SnapPayload(target.getId()));
                }
            }
        }

        /** O mob vai sumindo de cima para baixo enquanto vira poeira. */
        @SubscribeEvent
        public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
            LivingEntity e = event.getEntity();
            float progress = ClientState.progress(e.getId(), e.level().getGameTime(), event.getPartialTick());
            if (progress < 0f) return;
            float s = Math.max(0.001f, Disintegration.scaleFor(progress));
            event.getPoseStack().scale(1f, s, 1f);
        }
    }

    private static LivingEntity findTarget(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(RANGE));
        AABB box = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, eye, end, box,
                e -> e instanceof LivingEntity && !(e instanceof Player) && !e.isSpectator() && e.isPickable(),
                RANGE * RANGE);
        if (hit == null) return null;

        // Se tem um bloco no caminho, nao pega o mob atras da parede.
        BlockHitResult block = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS
                && block.getLocation().distanceToSqr(eye) < hit.getLocation().distanceToSqr(eye)) {
            return null;
        }
        return (LivingEntity) hit.getEntity();
    }
}
