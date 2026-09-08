package net.blacksmithzstudios.wasteland.net;

import io.netty.buffer.ByteBuf;
import net.blacksmithzstudios.wasteland.WastelandMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Carries the player's detection state to their client, plus how close the nearest threat is.
 *
 * Whether a mob has noticed you is server-side knowledge - {@code Mob.getTarget()} is never
 * sent to the client - so the readout cannot be worked out where it is drawn.
 *
 * @param state     one of the constants on {@link DetectionTracker}
 * @param closeness 0 when the nearest hostile is at the edge of the radius, 100 when on top
 *                  of the player. Drives how tightly the brackets close in.
 */
public record DetectionPayload(int state, int closeness) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DetectionPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(WastelandMod.MOD_ID, "detection"));

    public static final StreamCodec<ByteBuf, DetectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DetectionPayload::state,
            ByteBufCodecs.VAR_INT, DetectionPayload::closeness,
            DetectionPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
