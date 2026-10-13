package com.xyzmod.client;

import com.xyzmod.OverlayPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/** Client-side on-screen text: Xyz's typed-out voice (bottom) and glitching fake errors (top-left). */
public final class XyzOverlay {
    private static String speech = "";
    private static int speechAge, speechTotal;
    private static String glitch = "";
    private static int glitchAge, glitchTotal;

    public static void show(String text, int type, int ticks) {
        if (type == OverlayPacket.SPEECH) { speech = text; speechAge = 0; speechTotal = ticks; }
        else { glitch = text; glitchAge = 0; glitchTotal = ticks; }
    }

    public static void tick() {
        if (!speech.isEmpty() && ++speechAge > speechTotal) speech = "";
        if (!glitch.isEmpty() && ++glitchAge > glitchTotal) glitch = "";
    }

    private static int fade(int age, int total) {
        int left = total - age;
        return left >= 20 ? 255 : Math.max(0, left * 255 / 20);
    }

    public static final IGuiOverlay OVERLAY = (ForgeGui gui, GuiGraphics g, float partial, int w, int h) -> {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        if (!speech.isEmpty()) {
            int alpha = fade(speechAge, speechTotal);
            if (alpha > 8) {
                int visible = Math.min(speech.length(), (int) ((speechAge + partial) / 1.5F));
                List<FormattedCharSequence> lines = font.split(FormattedText.of(speech.substring(0, visible)), w * 2 / 3);
                int color = (alpha << 24) | 0xB00000;
                int y = h - 80 - lines.size() * 10;
                for (FormattedCharSequence line : lines) {
                    g.drawString(font, line, (w - font.width(line)) / 2, y, color, true);
                    y += 10;
                }
            }
        }

        if (!glitch.isEmpty() && mc.level != null) {
            int alpha = fade(glitchAge, glitchTotal);
            if (alpha > 8) {
                RandomSource rnd = RandomSource.create(mc.level.getGameTime() / 2);
                StringBuilder sb = new StringBuilder();
                for (char c : glitch.toCharArray()) {
                    sb.append(c != ' ' && rnd.nextInt(25) == 0 ? "#%@&$01".charAt(rnd.nextInt(7)) : c);
                }
                int dx = rnd.nextInt(5) - 2, dy = rnd.nextInt(5) - 2;
                g.pose().pushPose();
                g.pose().scale(1.3F, 1.3F, 1.0F);
                List<FormattedCharSequence> lines = font.split(FormattedText.of(sb.toString()), (int) (w / 1.3F) - 24);
                int y = 8 + dy;
                for (FormattedCharSequence line : lines) {
                    g.drawString(font, line, 8 + dx, y, (alpha << 24) | 0xFF2020, false);
                    y += 10;
                }
                g.pose().popPose();
            }
        }
    };

    private XyzOverlay() {}
}
