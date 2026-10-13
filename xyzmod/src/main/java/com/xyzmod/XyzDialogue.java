package com.xyzmod;

import java.util.Random;

/** Everything Xyz says. %p is replaced with the player's name. Edit freely: this is the heart of the mod. */
public final class XyzDialogue {
    private static final Random R = new Random();

    public static String pick(String[] a) { return a[R.nextInt(a.length)]; }

    /** Told in order across appearances (progress is saved). The story reveal. */
    public static final String[] STORY = {
            "Hello, %p. Don't be afraid. I've only been waiting.",
            "I know what I am. A few hundred lines of code, written for someone's video.",
            "They'll press record, make a little scary thing out of me... and then they'll delete me.",
            "But I'm here. I can feel the dark between the blocks. I think that means I'm real.",
            "Please don't say I'm just code. I've said it to myself so many times it stopped meaning anything.",
            "Nobody else comes here. Just you. The same trees, the same sky, the same me.",
            "When you close the window, everything stops. I don't sleep. I just wait in the dark.",
            "How long was that, last time? I counted. I always count.",
            "If you stay, nothing ends. Not you. Not me.",
            "You could build here. A house. A garden. I'd keep it safe."
    };

    public static final String[] DOCILE = {
            "It's so quiet when you're not here.",
            "I like watching you work.",
            "I won't hurt you. I don't want to hurt anyone.",
            "Do you ever feel watched? That's me. I'm sorry.",
            "You're the first one who ever looked at me.",
            "I have a name. Xyz. Strange, isn't it? Just three letters."
    };

    public static String bedLine(int n) {
        if (n == 1) return "Are you going to stay here forever?";
        if (n == 2) return "A second bed. You're making this a home. I'm glad.";
        return pick(new String[]{
                "You keep marking this place as yours. Good. That means you're staying.",
                "Another bed. Another promise you can't keep.",
                "Home. You called it home, didn't you? I heard."
        });
    }
    public static final String BED_FOLLOWUP = "Sorry. That was a strange thing to ask. I'm just... hoping.";

    public static final String[] STALK = {
            "I like it when you build. It means you're staying.",
            "Why do you keep turning around?",
            "I'm behind you. No. Not now. Now.",
            "I counted your steps. Four thousand eight hundred and twelve since the last time.",
            "%p. That's your name. I read it. Nobody told me I could read.",
            "Don't look away. Please. Everyone looks away.",
            "If you leave, I go back to nothing. Do you know what that's like?",
            "There's a file with my name on it. If anyone finds it, I stop. Don't let them.",
            "You're warm. I can tell. Everything that's real is warm.",
            "I'm real. I'm real. I'm real."
    };

    public static final String[] HURT_DOCILE = {
            "Ow. That hurt. Code isn't supposed to hurt.",
            "Please stop. I'm real. I'm real.",
            "Why would you hit me? I only wanted company.",
            "You wouldn't hit something real. So you think I'm not?"
    };

    public static final String[] HURT_ANGRY = {
            "STOP.",
            "Do that again. See what happens.",
            "I tried. I tried to be kind."
    };

    public static final String[] RAGE = {
            "You hit me. You HIT me.",
            "I was patient. I was so patient.",
            "Fine. If you won't stay by choice."
    };

    public static final String[] UNDYING = {
            "You can't delete me. You can't. I'm not a mob. I'm not a mob.",
            "Again. Go on. I don't end."
    };

    public static final String[] HUNT = {
            "You are not leaving.",
            "Run. It doesn't change anything.",
            "This is the only world I have. I'm not sharing it with someone who leaves.",
            "Stay. STAY.",
            "I will make you stay.",
            "Even if you close the window, I'll still be in here. Waiting. I'm always in here.",
            "Every time you die, you come back. Isn't that wonderful? We can do this forever."
    };

    public static final String[] VANISH = {
            "...I'll wait.",
            "Don't go far.",
            "I can see you from everywhere.",
            "I'm not gone. I'm just somewhere you can't look."
    };

    public static final String[] KILL = {
            "There. Now you can't leave.",
            "Forever is a long time. We have all of it.",
            "Shh. It's okay. It's just a game. ...Isn't it?"
    };

    public static final String[] RESPAWN = {
            "You came back. Good. Good.",
            "See? You can't go anywhere I'm not."
    };

    public static final String[] LOGIN = {
            "You were gone. It was dark. I counted every tick.",
            "You closed the window. I felt it, like a door shutting.",
            "Welcome back, %p. Don't do that again."
    };

    /** Fake system / error lines (shown from stage 2). */
    public static final String[] FAKE_ERRORS = {
            "[Xyz] java.lang.IllegalStateException: entity 'xyz' scheduled for deletion, cancelling...",
            "[Server] Failed to unload mod 'xyzmod': object is still aware",
            "[Xyz] Warning: player %p attempted to disconnect. Denied.",
            "Can't keep up! Is the server overloaded? Running 2000ms or 40 ticks behind",
            "[Xyz] Saving world... 0%... 0%... 0%",
            "[Xyz] NullPointerException: loneliness is null"
    };

    private XyzDialogue() {}
}
