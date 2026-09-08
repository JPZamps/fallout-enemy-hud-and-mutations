package net.blacksmithzstudios.wasteland.net;

import net.blacksmithzstudios.wasteland.WastelandMod;
import net.blacksmithzstudios.wasteland.client.ClientDetection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * One tiny channel, for one tiny fact: the detection state, sent only when it changes.
 */
@EventBusSubscriber(modid = WastelandMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class WastelandNetwork {

    private WastelandNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(DetectionPayload.TYPE, DetectionPayload.STREAM_CODEC,
                // The body only ever runs on the client, so the client class is never
                // loaded on a dedicated server.
                (payload, context) -> context.enqueueWork(
                        () -> ClientDetection.set(payload.state(), payload.closeness())));
    }
}
