package net.blacksmithzstudios.wasteland.net;

import net.blacksmithzstudios.wasteland.client.ClientDetection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Carries the player's detection state to their client, plus how close the nearest threat is.
 *
 * @param state     one of the constants on {@link DetectionTracker}
 * @param closeness 0 when the nearest hostile is at the edge of the radius, 100 when on top
 *                  of the player. Drives how tightly the brackets close in.
 */
public record DetectionPacket(int state, int closeness) {

    public static void encode(DetectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.state);
        buffer.writeByte(packet.closeness);
    }

    public static DetectionPacket decode(FriendlyByteBuf buffer) {
        return new DetectionPacket(buffer.readByte(), buffer.readByte());
    }

    public static void handle(DetectionPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientDetection.set(packet.state, packet.closeness)));
        context.get().setPacketHandled(true);
    }
}
