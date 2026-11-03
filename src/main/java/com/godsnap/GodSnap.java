package com.godsnap;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(GodSnap.MODID)
public class GodSnap {
    public static final String MODID = "godsnap";

    public GodSnap(IEventBus modBus) {
        modBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SnapPayload.TYPE, SnapPayload.CODEC, SnapPayload::handle);
        registrar.playToClient(DisintegratePayload.TYPE, DisintegratePayload.CODEC, DisintegratePayload::handle);
    }
}
