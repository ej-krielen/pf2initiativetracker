package com.rekijan.initiativetrackersecondedition.feature.condition;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.util.NumberInput;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The list of standard conditions in the condition wizard, searchable by name. Each row has a
 * duration and, for valued conditions, a value; Add puts a copy on the character.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class ConditionWizardAdapter extends RecyclerView.Adapter<ConditionWizardAdapter.ConditionViewHolder> {

    private final List<ConditionProvider.Condition> allConditions;
    private final List<ConditionProvider.Condition> shown = new ArrayList<>();
    private final Consumer<DebuffModel> onAdd;
    private boolean hideDescriptions;

    /**
     * @param onAdd Gets the condition the user added, with the duration and value filled in.
     */
    public ConditionWizardAdapter(List<ConditionProvider.Condition> conditions, boolean hideDescriptions,
                                  Consumer<DebuffModel> onAdd) {
        this.allConditions = conditions;
        this.hideDescriptions = hideDescriptions;
        this.onAdd = onAdd;
        shown.addAll(conditions);
    }

    static class ConditionViewHolder extends RecyclerView.ViewHolder {
        final TextView nameTextView;
        final TextView descriptionTextView;
        final EditText durationEditText;
        final EditText valueEditText;
        final TextView valueLabel;
        final Button addButton;

        ConditionViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.condition_name_textView);
            descriptionTextView = itemView.findViewById(R.id.condition_description_textView);
            durationEditText = itemView.findViewById(R.id.condition_duration_editText);
            valueEditText = itemView.findViewById(R.id.condition_value_editText);
            valueLabel = itemView.findViewById(R.id.condition_value_textView);
            addButton = itemView.findViewById(R.id.add_condition_button);
        }
    }

    /** Shows only the conditions whose name contains the search text. */
    @SuppressLint("NotifyDataSetChanged") // The filtered list can change completely
    public void filter(String search) {
        String query = search == null ? "" : search.trim().toLowerCase(Locale.getDefault());
        shown.clear();
        for (ConditionProvider.Condition c : allConditions) {
            if (c.name.toLowerCase(Locale.getDefault()).contains(query)) shown.add(c);
        }
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged") // Every row changes
    public void setHideDescriptions(boolean hide) {
        hideDescriptions = hide;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConditionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_condition, parent, false);
        return new ConditionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ConditionViewHolder holder, int position) {
        ConditionProvider.Condition condition = shown.get(position);
        holder.nameTextView.setText(condition.name);
        holder.descriptionTextView.setText(condition.description);
        holder.descriptionTextView.setVisibility(hideDescriptions ? View.GONE : View.VISIBLE);
        holder.durationEditText.setText("0");
        holder.valueEditText.setText("0");
        int valueVisibility = Conditions.isValued(condition.name) ? View.VISIBLE : View.GONE;
        holder.valueEditText.setVisibility(valueVisibility);
        holder.valueLabel.setVisibility(valueVisibility);

        holder.addButton.setOnClickListener(v -> {
            int duration = NumberInput.parseInt(holder.durationEditText.getText().toString(), 0, 0,
                    DebuffModel.MAX_DURATION);
            int value = holder.valueEditText.getVisibility() == View.VISIBLE
                    ? NumberInput.parseInt(holder.valueEditText.getText().toString(), 0, 0, DebuffModel.MAX_VALUE)
                    : 0;
            onAdd.accept(new DebuffModel(condition.name, condition.description, duration, value));
        });
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }
}
