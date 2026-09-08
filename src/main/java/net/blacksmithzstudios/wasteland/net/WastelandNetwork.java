package net.blacksmithzstudios.wasteland.net;

import net.blacksmithzstudios.wasteland.WastelandMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * One tiny channel, for one tiny fact.
 *
 * Whether a mob has noticed you is server-side knowledge - {@code Mob.getTarget()} is never
 * sent to the client - so the detection readout cannot be computed where it is drawn. The
 * server works it out and ships a single byte whenever it changes.
 */
public final class WastelandNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(WastelandMod.MOD_ID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private static int nextId = 0;

    private WastelandNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(nextId++, DetectionPacket.class,
                DetectionPacket::encode, DetectionPacket::decode, DetectionPacket::handle);
    }
}
