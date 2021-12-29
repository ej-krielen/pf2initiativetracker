package com.rekijan.initiativetrackersecondedition.character.adapter;

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

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.AppExtension;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.character.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.character.model.DebuffModel;
import com.rekijan.initiativetrackersecondedition.ui.fragments.DebuffWizardFragment;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom RecyclerView.Adapter for the DebuffModel class
 *
 * @author Erik-Jan Krielen ej.krielen@gmail.com
 * @since 1-6-2019
 */
public class DebuffWizardAdapter extends RecyclerView.Adapter<DebuffWizardAdapter.DebuffViewHolder> implements Filterable {

    private final AppExtension app;
    private final DebuffWizardFragment callingFragment;
    // Field for the list of DebuffModels
    private ArrayList<DebuffModel> debuffs = new ArrayList<>();
    //Field of the original list
    private final ArrayList<DebuffModel> originalDebuffs = new ArrayList<>();
    // Reference to character to which the debuffs belong
    private final CharacterModel character;

    // C'tor
    public DebuffWizardAdapter(CharacterModel character, AppExtension app, DebuffWizardFragment callingFragment) {
        this.character = character;
        this.app = app;
        this.callingFragment = callingFragment;
    }

    public void add(DebuffModel debuffs) {
        this.debuffs.add(debuffs);
    }

    public void addAll(List<DebuffModel> debuffs) {
        this.debuffs.addAll(debuffs);
        this.originalDebuffs.addAll(debuffs);
    }

    /**
     * Remove a debuff based on position
     * @param position
     */
    public void remove(int position) {
        if (debuffs.size() > position) {
            debuffs.remove(position);
        }
        notifyDataSetChanged();
    }

    /**
     * Remove based on the debuff given
     * @param debuff
     */
    public void remove(DebuffModel debuff) {
        for (int i = 0; i < debuffs.size(); i++) {
            if (debuffs.get(i) == debuff) {
                remove(i);
            }
        }
    }

    public void removeAll() {
        debuffs.clear();
    }

    public ArrayList<DebuffModel> getList() {
        return debuffs;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                debuffs = (ArrayList<DebuffModel>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                ArrayList<DebuffModel> filteredResults = null;
                if (constraint.length() == 0) {
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

    protected ArrayList<DebuffModel> getFilteredResults(String constraint) {
        ArrayList<DebuffModel> results = new ArrayList<>();

        for (DebuffModel debuff : originalDebuffs) {
            if (debuff.getName().toLowerCase().contains(constraint)) {
                results.add(debuff);
            }
        }
        return results;
    }

    /* ViewHolder region */
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
    /* End of Viewholder region */

    @Override
    public DebuffViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_condition, parent, false);
        return new DebuffViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final DebuffViewHolder holder, int position) {

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

        holder.addConditionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Remove from adapter and CharacterModel

                int value = getValue(holder.conditionValueEditText.getText().toString());
                String name = value > 0 ? debuff.getName() + " " + value : debuff.getName();
                character.getDebuffList().add(new DebuffModel(name, debuff.getDescription(), getValue(holder.conditionDurationEditText.getText().toString())));
                Toast.makeText(app, String.format(app.getResources().getString(R.string.debuff_added_existing), name), Toast.LENGTH_LONG).show();
                callingFragment.getParentFragmentManager().popBackStack();
            }
        });
    }

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