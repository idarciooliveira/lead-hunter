package me.iofdev.leadhunter.cli;

import java.awt.Color;
import java.io.PrintWriter;
import java.util.Map;

/** A hand-placed pixel art fox rendered into the interactive menu. See ADR 0025. */
final class LeadFox {

    /** Left half of the face, one character per pixel; the right half is its mirror image. */
    private static final String[] HALF = {
        ".....KK..............",
        "....KHLK.............",
        "...KHLOOK............",
        "...KHLEOOK...........",
        "..KHLEEEOOK..........",
        "..KHLEEEPOOK.........",
        "..KLLEEEPPOOK........",
        ".KHLLEEEPPOOOK.......",
        ".KHLEEEEPPPOOOK......",
        ".KHLEEEEPPPOOOOOK....",
        ".KLLEEEEPPPOOOOOOKKKK",
        "KKLOOEEEPPPOOOOOOOOOO",
        "KLHLOOOOOOOOOOOOOOOOO",
        "KLHLLOOOOOOOOOOOOOOOW",
        "KLLLLOOHHOOOOOOOOOOWW",
        "KLLLLLOOHHHOOOOOOOWWW",
        ".KLLLLLLDDDDDDOOOOWWW",
        ".KLLLLLDKKKKKKKOOOWWW",
        ".KLLLLKDYTBYYKKDOWWWW",
        ".KLLLLDKDYBYYKDDOWWWW",
        ".KLLLLLODKKKKDOOOWWWW",
        "KLLLOOOOOOOOOOOOWWWWW",
        "KHLLOOOOOOOOOOOWWWWWW",
        "KLLOOOOOOOOOOWWWWWWWW",
        ".KLOOOOOOOOWSWWWWWWWW",
        ".KLLOOOOOWSSWWWWWWWWW",
        "KLKLOOWSWWWWWWWWWBGBB",
        "KWSKLOWSWWWWWWWWWBBBB",
        ".KWWKLWWSWWWWWWWWWBBB",
        "..KSWKWWWWWWWWWWWWWWK",
        "...KSKWWWWWWWWWWWWWWK",
        "....KSKWWWWWWWWKKKKKK",
        ".....KSKWWWWKKKWWWWWW",
        "......KSSKWWWWWSSWWWW",
        ".......KSSKWWWWSSSSSS",
        "........KKSSSSSSSSSSS",
        "..........KKKKKKKKKKK",
        ".....................",
    };

    private static final Map<Character, Color> PALETTE = Map.ofEntries(
            Map.entry('K', new Color(52, 28, 30)),
            Map.entry('D', new Color(158, 62, 32)),
            Map.entry('O', new Color(226, 108, 38)),
            Map.entry('L', new Color(243, 141, 52)),
            Map.entry('H', new Color(255, 184, 96)),
            Map.entry('W', new Color(255, 240, 214)),
            Map.entry('S', new Color(216, 188, 160)),
            Map.entry('E', new Color(118, 42, 40)),
            Map.entry('P', new Color(196, 82, 48)),
            Map.entry('B', new Color(30, 24, 28)),
            Map.entry('Y', new Color(238, 170, 54)),
            Map.entry('G', new Color(96, 84, 88)),
            Map.entry('T', new Color(255, 250, 236)));

    private static final int HALF_WIDTH = HALF[0].length();
    private static final int WIDTH = HALF_WIDTH * 2;
    private static final int HEIGHT = HALF.length;
    private static final String RESET = "\u001B[0m";
    private static final String ASCII_RAMP = "  .:-=+*#%@";

    private LeadFox() {
    }

    /** Prints the illustration and wordmark once, in color or monochrome. */
    static void print(PrintWriter out, boolean color) {
        Color[][] pixels = artwork();
        for (int y = 0; y < HEIGHT; y += 2) {
            if (color) {
                printColorRow(out, pixels, y);
            } else {
                printPlainRow(out, pixels, y);
            }
        }
        printWordmark(out, color);
        out.flush();
    }

    /** The sprite as rows of pixels; {@code null} is transparent, so the terminal shows through. */
    static Color[][] artwork() {
        Color[][] pixels = new Color[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < HALF_WIDTH; x++) {
                Color color = PALETTE.get(HALF[y].charAt(x));
                pixels[y][x] = color;
                pixels[y][WIDTH - 1 - x] = color;
            }
        }
        return pixels;
    }

    private static void printColorRow(PrintWriter out, Color[][] pixels, int y) {
        for (int x = 0; x < WIDTH; x++) {
            Color top = pixels[y][x];
            Color bottom = pixels[y + 1][x];
            if (top == null && bottom == null) {
                out.append(RESET).append(' ');
            } else if (bottom == null) {
                out.append(RESET).append(foreground(top)).append('▀');
            } else if (top == null) {
                out.append(RESET).append(foreground(bottom)).append('▄');
            } else {
                out.append(foreground(top)).append(background(bottom)).append('▀');
            }
        }
        out.append(RESET).append('\n');
    }

    private static String foreground(Color c) {
        return "\u001B[38;2;" + c.getRed() + ';' + c.getGreen() + ';' + c.getBlue() + 'm';
    }

    private static String background(Color c) {
        return "\u001B[48;2;" + c.getRed() + ';' + c.getGreen() + ';' + c.getBlue() + 'm';
    }

    private static void printPlainRow(PrintWriter out, Color[][] pixels, int y) {
        for (int x = 0; x < WIDTH; x++) {
            Color top = pixels[y][x];
            Color bottom = pixels[y + 1][x];
            if (top == null && bottom == null) {
                out.append(' ');
                continue;
            }
            int luminance = ((top == null ? luminance(bottom) : luminance(top))
                    + (bottom == null ? luminance(top) : luminance(bottom))) / 2;
            out.append(ASCII_RAMP.charAt(
                    Math.min(ASCII_RAMP.length() - 1, luminance * (ASCII_RAMP.length() - 1) / 255)));
        }
        out.append('\n');
    }

    private static int luminance(Color c) {
        return (c.getRed() * 212 + c.getGreen() * 715 + c.getBlue() * 72) / 999;
    }

    private static void printWordmark(PrintWriter out, boolean color) {
        printCentered(out, "LEAD HUNTER", color ? "\u001B[38;2;255;185;104m" : "");
        printCentered(out, "LOCAL LEADS. SMARTER OUTREACH.",
                color ? "\u001B[38;2;164;171;187m" : "");
    }

    private static void printCentered(PrintWriter out, String text, String color) {
        out.append(" ".repeat(Math.max(0, (WIDTH - text.length()) / 2)));
        if (!color.isEmpty()) {
            out.append(color).append(text).append(RESET);
        } else {
            out.append(text);
        }
        out.append('\n');
    }
}
