package com.godsnap;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Cliente -> Servidor: "apague a entidade com este id". */
public record SnapPayload(int entityId) implements CustomPacketPayload {

    public static final Type<SnapPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GodSnap.MODID, "snap"));

    public static final StreamCodec<ByteBuf, SnapPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SnapPayload::entityId, SnapPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SnapPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                Disintegration.start(player, payload.entityId());
            }
        });
    }
}
