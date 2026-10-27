package com.rekijan.initiativetrackersecondedition.feature.condition;

import android.content.Context;
import android.content.res.Resources;

import com.rekijan.initiativetrackersecondedition.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The standard Pathfinder 2e (Player Core) conditions offered by the condition wizard, read from
 * condition_strings.xml so they follow the user's language.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 21-6-2026
 */
public final class ConditionProvider {

    /** A condition in the wizard: a template, a copy goes on the character. */
    public static final class Condition {
        public final String name;
        public final String description;

        Condition(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }

    private ConditionProvider() {
    }

    public static List<Condition> standardConditions(Context context) {
        Resources res = context.getResources();
        String[] names = res.getStringArray(R.array.condition_names);
        String[] descriptions = res.getStringArray(R.array.condition_descriptions);
        List<Condition> conditions = new ArrayList<>();
        for (int i = 0; i < Math.min(names.length, descriptions.length); i++) {
            conditions.add(new Condition(names[i], descriptions[i]));
        }
        return Collections.unmodifiableList(conditions);
    }
}
