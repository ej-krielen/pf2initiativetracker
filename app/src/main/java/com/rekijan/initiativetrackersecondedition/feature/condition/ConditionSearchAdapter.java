package com.rekijan.initiativetrackersecondedition.feature.condition;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The suggestions of a typed condition field: the standard conditions that match what is typed,
 * also by pre-remaster name ("flat-f" suggests "Off-Guard (flat-footed)", see
 * {@link Conditions#searchLabel}). Picking one puts its name in the field.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-10-2026
 */
public class ConditionSearchAdapter extends ArrayAdapter<String> {

    private final List<ConditionProvider.Condition> all;
    private final List<ConditionProvider.Condition> shown = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();

    public ConditionSearchAdapter(Context context, List<ConditionProvider.Condition> conditions) {
        super(context, android.R.layout.simple_list_item_1);
        this.all = conditions;
        show("");
    }

    /** @return The condition of a suggestion, by its position in the list. */
    public ConditionProvider.Condition conditionAt(int position) {
        return shown.get(position);
    }

    /** @return The standard condition with exactly this name (any case), or null for a typed name of your own. */
    @Nullable
    public ConditionProvider.Condition find(String name) {
        String key = Conditions.key(name);
        for (ConditionProvider.Condition c : all) {
            if (Conditions.key(c.name).equals(key)) return c;
        }
        return null;
    }

    @Override
    public int getCount() {
        return labels.size();
    }

    @Override
    public String getItem(int position) {
        return labels.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                // Worked out on the UI thread in publishResults; the list is short
                FilterResults results = new FilterResults();
                results.values = constraint == null ? "" : constraint.toString();
                results.count = 1;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                show((String) results.values);
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                int i = labels.indexOf(String.valueOf(resultValue));
                return i >= 0 ? shown.get(i).name : String.valueOf(resultValue);
            }
        };
    }

    private void show(String search) {
        shown.clear();
        labels.clear();
        for (ConditionProvider.Condition c : all) {
            String label = Conditions.searchLabel(c.name, search);
            if (label == null) continue;
            shown.add(c);
            labels.add(label);
        }
        notifyDataSetChanged();
    }
}
