package net.blacksmithzstudios.wasteland.client;

import net.blacksmithzstudios.wasteland.net.DetectionTracker;

/** The last detection state the server sent. Client-only. */
public final class ClientDetection {

    private static int state = DetectionTracker.HIDDEN;
    private static float closeness;

    private ClientDetection() {
    }

    public static void set(int newState, int newCloseness) {
        state = newState;
        closeness = Math.max(0.0F, Math.min(1.0F, newCloseness / 100.0F));
    }

    public static int state() {
        return state;
    }

    /** 0 at the edge of the detection radius, 1 right on top of the player. */
    public static float closeness() {
        return closeness;
    }
}
