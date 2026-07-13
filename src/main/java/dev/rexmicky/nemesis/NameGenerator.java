package dev.rexmicky.nemesis;

import java.util.concurrent.ThreadLocalRandom;

/** Generates a rival's name once, and a title that escalates as it levels up. */
public final class NameGenerator {

    private static final String[] NAMES = {
            "Grishnak", "Morggath", "Ur'Zog", "Vurl", "Skarn", "Thok", "Rhagnor", "Gorehowl",
            "Nokk", "Zul'Mar", "Drayven", "Bonecrack", "Vex'ra", "Hakkar", "Grull", "Xarot",
            "Malketh", "Ogrim", "Serpix", "Karnage", "Dregmaw", "Vashek"
    };

    private static final String[][] TITLES = {
            { "the Cruel", "the Vicious", "the Grim", "the Wretched", "the Savage" },      // Lv.1
            { "the Relentless", "the Bloody", "the Merciless", "the Feared" },             // Lv.2
            { "the Undying", "the Dreadful", "the Terror", "the Ruthless" },               // Lv.3
            { "the Nightmare", "the Butcher", "the Devourer", "the Unbroken" },            // Lv.4
            { "the Unkillable", "Death Incarnate", "the World-Ender", "the Doom" }         // Lv.5+
    };

    private NameGenerator() {}

    public static String randomName() {
        return NAMES[ThreadLocalRandom.current().nextInt(NAMES.length)];
    }

    /** Deterministic for a given (name, level) so it doesn't flicker on re-apply. */
    public static String title(String baseName, int level, String owner) {
        int tier = Math.min(level, TITLES.length) - 1;
        if (tier < 0) tier = 0;
        // At high level, some rivals become personally named after their victim.
        if (level >= 4 && Math.floorMod(baseName.hashCode(), 3) == 0) {
            return "Bane of " + owner;
        }
        String[] pool = TITLES[tier];
        return pool[Math.floorMod(baseName.hashCode() + level * 7, pool.length)];
    }
}
