package com.rekijan.initiativetrackersecondedition.features.debuffs;

import android.content.Context;
import android.content.res.Resources;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.data.model.DebuffModel;

import java.util.ArrayList;

/**
 * Provides the master list of standard Pathfinder 2e conditions.
 * Loads condition data from XML resources and caches it for performance.
 */
public class ConditionProvider {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Members
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static ConditionProvider sInstance = null;
    private ArrayList<DebuffModel> conditions;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Initialization
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private ConditionProvider() {}

    /**
     * @return The singleton instance of the provider.
     */
    public static synchronized ConditionProvider getInstance() {
        if (sInstance == null) {
            sInstance = new ConditionProvider();
        }
        return sInstance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Loads and returns the list of standard conditions defined in the app resources.
     * Uses a lazy-loading cache to avoid re-parsing XML on every request.
     *
     * @param context Android context for resource access.
     * @return A list of initialized DebuffModel objects representing standard conditions.
     */
    public ArrayList<DebuffModel> getStandardConditions(Context context) {
        if (conditions == null || conditions.isEmpty()) {
            conditions = loadFromResources(context);
        }
        return conditions;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Private helpers
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private ArrayList<DebuffModel> loadFromResources(Context context) {
        ArrayList<DebuffModel> list = new ArrayList<>();
        Resources res = context.getResources();

        String[] names = res.getStringArray(R.array.condition_names);
        String[] descriptions = res.getStringArray(R.array.condition_descriptions);

        // Ensure arrays are synchronized
        final int length = Math.min(names.length, descriptions.length);

        for (int i = 0; i < length; i++) {
            list.add(new DebuffModel(names[i], descriptions[i]));
        }

        return list;
    }
}