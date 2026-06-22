package com.rekijan.initiativetrackersecondedition.features.debuffs;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.core.AppExtension;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.data.model.DebuffModel;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom RecyclerView.Adapter for selecting existing conditions to add to a character.
 * Implements Filterable to allow searching through the standard condition list.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class DebuffWizardAdapter extends RecyclerView.Adapter<DebuffWizardAdapter.DebuffViewHolder> implements Filterable {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private final AppExtension app;    private final DebuffWizardFragment callingFragment;
    private ArrayList<DebuffModel> debuffs = new ArrayList<>();
    private final ArrayList<DebuffModel> originalDebuffs = new ArrayList<>();
    private final CharacterModel character;

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Constructor    ////////////////////////////////////////////////////////////////////////////////////////////////    public DebuffWizardAdapter(CharacterModel character, AppExtension app, DebuffWizardFragment callingFragment) {        this.character = character;
        this.app = app;
        this.callingFragment = callingFragment;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Public helpers    ////////////////////////////////////////////////////////////////////////////////////////////////    public void add (DebuffModel debuff) {        this.debuffs.add(debuff);
        notifyItemInserted(debuffs.size() - 1);
    }

    public void addAll (List<DebuffModel> debuffs) {
        int startPos = this.debuffs.size();
        this.debuffs.addAll(debuffs);
        this.originalDebuffs.addAll(debuffs);
        notifyItemRangeInserted(startPos, debuffs.size());
    }

    public void remove (int position) {
        if (debuffs.size() > position) {
            debuffs.remove(position);
            notifyItemRemoved(position);
        }
    }

    public ArrayList<DebuffModel> getList () {
        return debuffs;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Implementation methods    ////////////////////////////////////////////////////////////////////////////////////////////////    @Override    public Filter getFilter() {
        return new Filter() {
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                debuffs = (ArrayList<DebuffModel>) results.values;
                notifyItemRangeChanged(0, debuffs.size());
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                ArrayList<DebuffModel> filteredResults;
                if (TextUtils.isEmpty(constraint)) {
                    filteredResults = originalDebuffs;
                } else {
                    filteredResults = getFilteredResults(constraint.toString().toLowerCase());
                }

                FilterResults results = new FilterResults();
                results.values = filteredResults;

                return results;
            }
        };
    }

    /**
     * Technical filter implementation for the search view.
     */
    protected ArrayList<DebuffModel> getFilteredResults(String constraint) {
        ArrayList<DebuffModel> results = new ArrayList<>();

        for (DebuffModel debuff : originalDebuffs) {
            if (debuff.getName().toLowerCase().contains(constraint)) {
                results.add(debuff);
            }
        }
        return results;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// ViewHolder region    ////////////////////////////////////////////////////////////////////////////////////////////////    /**     * ViewHolder for conditions in the wizard selection list.
     */
    public static class DebuffViewHolder extends RecyclerView.ViewHolder {
        CardView conditionCardView;
        TextView conditionNameTextView;
        TextView conditionDescriptionTextView;
        EditText conditionDurationEditText;
        EditText conditionValueEditText;
        Button addConditionButton;

        DebuffViewHolder(View itemView) {
            super(itemView);
            conditionCardView = itemView.findViewById(R.id.condition_cardView);
            conditionNameTextView = itemView.findViewById(R.id.condition_name_textView);
            conditionDescriptionTextView = itemView.findViewById(R.id.condition_description_textView);
            conditionDurationEditText = itemView.findViewById(R.id.condition_duration_editText);
            conditionValueEditText = itemView.findViewById(R.id.condition_value_editText);
            addConditionButton = itemView.findViewById(R.id.add_condition_button);
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// End of ViewHolder region    ////////////////////////////////////////////////////////////////////////////////////////////////    @NonNull    @Override
    public DebuffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_condition, parent, false);
        return new DebuffViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final DebuffViewHolder holder, int position) {

        //Get corresponding debuff
        final DebuffModel debuff = debuffs.get(position);

        //Set TextView and EditText values
        holder.conditionNameTextView.setText(debuff.getName());
        holder.conditionDescriptionTextView.setText(debuff.getDescription());
        holder.conditionDurationEditText.setText("0");
        holder.conditionValueEditText.setText("0");

        if (app.getHideDescription())
        {
            holder.conditionDescriptionTextView.setVisibility(View.GONE);
        } else {
            holder.conditionDescriptionTextView.setVisibility(View.VISIBLE);
        }

        holder.addConditionButton.setOnClickListener(view -> {
            //Remove from adapter and CharacterModel

            int value = getValue(holder.conditionValueEditText.getText().toString());
            String name = value > 0 ? debuff.getName() + " " + value : debuff.getName();
            character.getDebuffList().add(new DebuffModel(name, debuff.getDescription(), getValue(holder.conditionDurationEditText.getText().toString())));
            Toast.makeText(app, String.format(app.getResources().getString(R.string.debuff_added_existing), name), Toast.LENGTH_LONG).show();
            callingFragment.getParentFragmentManager().popBackStack();
        });
    }

    /**
     * Safely parses text into an integer with overflow protection.
     */
    private int getValue(String text)
    {
        BigInteger maxInt = BigInteger.valueOf(Integer.MAX_VALUE);
        BigInteger value = new BigInteger(TextUtils.isEmpty(text) ? "0" : text);

        if (value.compareTo(maxInt) > 0)
        {
            value = maxInt;
        }
        return value.intValue();
    }

    @Override
    public int getItemCount() {
        return debuffs.size();
    }
}