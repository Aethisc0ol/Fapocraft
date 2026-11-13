package com.fapcraftport.sexmod.entity;

/**
 * Jenny's subset of the original action-state enum (com.trolmastercard.sexmod.fp),
 * plus WALK_TO_BED, which the original handled inside its doggy pathing code.
 */
public enum GirlState {
    NULL,
    PAYMENT,
    STRIP,
    // blowjob
    STARTBLOWJOB,
    SUCKBLOWJOB,
    THRUSTBLOWJOB,
    CUMBLOWJOB,
    // doggy
    WALK_TO_BED,
    STARTDOGGY,
    WAITDOGGY,
    DOGGYSTART,
    DOGGYSLOW,
    DOGGYFAST,
    DOGGYCUM,
    // boobjob
    PAIZURI_START,
    PAIZURI_SLOW,
    PAIZURI_FAST,
    PAIZURI_CUM;

    private static final GirlState[] VALUES = values();

    public static GirlState byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NULL;
    }

    /** States where the partner is frozen and drawn as part of Jenny's model. */
    public boolean isLocked() {
        return switch (this) {
            case STARTBLOWJOB, SUCKBLOWJOB, THRUSTBLOWJOB, CUMBLOWJOB,
                 DOGGYSTART, DOGGYSLOW, DOGGYFAST, DOGGYCUM,
                 PAIZURI_START, PAIZURI_SLOW, PAIZURI_FAST, PAIZURI_CUM -> true;
            default -> false;
        };
    }

    /** States where the "faster" input does something. */
    public boolean acceptsSpeed() {
        return this == SUCKBLOWJOB || this == DOGGYSLOW || this == PAIZURI_SLOW;
    }

    /** States where the "finish" input does something. */
    public boolean acceptsFinish() {
        return switch (this) {
            case SUCKBLOWJOB, THRUSTBLOWJOB, DOGGYSLOW, DOGGYFAST, PAIZURI_SLOW, PAIZURI_FAST -> true;
            default -> false;
        };
    }

    /** States where Jenny's AI (walking, looking around) is paused. */
    public boolean pausesAi() {
        return this != NULL && this != WALK_TO_BED;
    }
}
