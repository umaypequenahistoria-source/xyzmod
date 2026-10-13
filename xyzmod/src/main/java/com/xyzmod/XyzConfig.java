package com.xyzmod;

import net.minecraftforge.common.ForgeConfigSpec;

/** Tunable timeline. Everything is counted in minutes of play time PER PLAYER. */
public final class XyzConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue STAGE1_MIN;
    public static final ForgeConfigSpec.IntValue STAGE2_MIN;
    public static final ForgeConfigSpec.IntValue STAGE3_MIN;
    public static final ForgeConfigSpec.IntValue EVENT_INTERVAL_SEC;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("timeline");
        STAGE1_MIN = b.comment("Stage 1 (Presence): minutes of play until Xyz first shows himself. He is docile.")
                .defineInRange("stage1Minutes", 1, 0, 100000);
        STAGE2_MIN = b.comment("Stage 2 (Stalking): minutes of play until he starts following you, teleporting behind you, darkening the world.")
                .defineInRange("stage2Minutes", 20, 0, 100000);
        STAGE3_MIN = b.comment("Stage 3 (Hunt): minutes of play until he tries to kill you.")
                .defineInRange("stage3Minutes", 40, 0, 100000);
        EVENT_INTERVAL_SEC = b.comment("Base seconds between 'something is wrong' events. Shrinks as the stage rises.")
                .defineInRange("eventIntervalSeconds", 45, 5, 3600);
        b.pop();
        SPEC = b.build();
    }

    private XyzConfig() {}
}
