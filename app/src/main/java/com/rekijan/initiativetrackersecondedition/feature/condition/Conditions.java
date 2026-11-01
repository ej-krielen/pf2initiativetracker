package com.rekijan.initiativetrackersecondedition.feature.condition;

import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Knows the Pathfinder 2e (Player Core) conditions by name: which ones have a value, and the
 * pre-remaster names a save can contain (Flat Footed is Off-Guard). Names are compared without
 * case and extra spaces, because saved conditions can be renamed by the user. Searching finds a
 * condition by its pre-remaster name too ({@link #searchLabel}). Plain Java, so it is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public final class Conditions {

    public static final String FRIGHTENED = "frightened";
    public static final String OFF_GUARD = "off-guard";

    /** The conditions that always (or usually) come with a value, Player Core pg. 442-447. */
    private static final Set<String> VALUED = new HashSet<>(Arrays.asList("clumsy", "doomed", "drained",
            "dying", "enfeebled", FRIGHTENED, "sickened", "slowed", "stunned", "stupefied", "wounded"));

    /** Pre-remaster names and the Player Core condition they became. */
    private static final Map<String, String> LEGACY_NAMES = new HashMap<>();

    static {
        LEGACY_NAMES.put("flat footed", OFF_GUARD);
        LEGACY_NAMES.put("flat-footed", OFF_GUARD);
        LEGACY_NAMES.put("flatfooted", OFF_GUARD);
    }

    /** The pre-remaster name a search shows next to the Player Core condition it became. */
    private static final Map<String, String> LEGACY_LABELS = new HashMap<>();

    static {
        LEGACY_LABELS.put(OFF_GUARD, "flat-footed");
    }

    /** A name ending in a number, a valued condition saved with its value: "Frightened 2". */
    private static final Pattern NAME_WITH_VALUE = Pattern.compile("^(.*\\S)\\s+(\\d{1,2})$");

    private Conditions() {
    }

    /**
     * @return The lower case Player Core name of a condition, also for a pre-remaster name, e.g.
     * "Flat Footed" gives "off-guard".
     */
    public static String key(String name) {
        if (name == null) return "";
        String key = name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        String renamed = LEGACY_NAMES.get(key);
        return renamed != null ? renamed : key;
    }

    public static boolean isValued(String name) {
        return VALUED.contains(key(name));
    }

    /**
     * Matches a condition against search text, ignoring case, spaces and hyphens ("flat-f",
     * "flat f" and "flatf" are the same).
     *
     * @return The name when it matches (or the search is empty), the name with its pre-remaster
     * name when only that one matches ("Off-Guard (flat-footed)"), or null when neither matches.
     */
    public static String searchLabel(String name, String search) {
        String query = lettersOf(search);
        if (query.isEmpty() || lettersOf(name).contains(query)) return name;
        String legacy = LEGACY_LABELS.get(key(name));
        if (legacy != null && lettersOf(legacy).contains(query)) return name + " (" + legacy + ")";
        return null;
    }

    private static String lettersOf(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    /** @return True when the (de)buff is the given condition, see {@link #key(String)}. */
    public static boolean is(DebuffModel debuff, String conditionKey) {
        return !debuff.isPersistentDamage() && key(debuff.getName()).equals(conditionKey);
    }

    /**
     * Splits a value saved in the name of a condition ("Frightened 2") into the name and the
     * value, so the value can count down. Only known valued conditions are changed, so a
     * custom (de)buff like "Bless 1" keeps its name.
     */
    public static void upgradeLegacyValue(DebuffModel debuff) {
        if (debuff.isPersistentDamage() || debuff.getValue() > 0) return;
        Matcher m = NAME_WITH_VALUE.matcher(debuff.getName().trim());
        if (!m.matches() || !isValued(m.group(1))) return;
        debuff.setName(m.group(1));
        debuff.setValue(Integer.parseInt(m.group(2)));
    }
}
