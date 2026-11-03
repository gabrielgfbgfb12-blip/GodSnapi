package com.godsnap;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Servidor -> Cliente: "esta entidade comecou a virar poeira, e o vento sopra para (windX, windZ)". */
public record DisintegratePayload(int entityId, int duration, float windX, float windZ)
        implements CustomPacketPayload {

    public static final Type<DisintegratePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GodSnap.MODID, "disintegrate"));

    public static final StreamCodec<ByteBuf, DisintegratePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DisintegratePayload::entityId,
            ByteBufCodecs.VAR_INT, DisintegratePayload::duration,
            ByteBufCodecs.FLOAT, DisintegratePayload::windX,
            ByteBufCodecs.FLOAT, DisintegratePayload::windZ,
            DisintegratePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DisintegratePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientState.start(
                payload.entityId(), payload.duration(), ctx.player().level().getGameTime(),
                payload.windX(), payload.windZ()));
    }
}
