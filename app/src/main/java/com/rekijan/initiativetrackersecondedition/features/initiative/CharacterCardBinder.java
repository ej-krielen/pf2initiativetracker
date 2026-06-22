package com.rekijan.initiativetrackersecondedition.features.initiative;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.AppExtension;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.data.model.DebuffModel;
import com.rekijan.initiativetrackersecondedition.data.model.ReactionModel;
import com.rekijan.initiativetrackersecondedition.features.health.DyingDialogHelper;

import java.util.ArrayList;

/**
 * Encapsulates the complex visual logic for the character cards in the initiative list.
 * Decides colors, visibility of sections, and icon assignments based on character state.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 21-6-2026
 */
public class CharacterCardBinder {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private static CharacterCardBinder sInstance = null;
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Initialization    ////////////////////////////////////////////////////////////////////////////////////////////////    public static synchronized CharacterCardBinder getInstance() {        if (sInstance == null) {
            sInstance = new CharacterCardBinder();
        }
        return sInstance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods    ////////////////////////////////////////////////////////////////////////////////////////////////    public void bindCard(Context context, InitiativeAdapter.CharacterViewHolder holder, CharacterModel character) {        setCardBackground(context, holder, character);
        bindDebuffOverview(context, holder, character);
        bindReactionSection(context, holder, character);
        bindDyingSection(context, holder, character);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Background Color Logic    ////////////////////////////////////////////////////////////////////////////////////////////////    private void setCardBackground(Context context, InitiativeAdapter.CharacterViewHolder holder, CharacterModel character) {        final int color;
        if (character.isDying()) {
            color = ContextCompat.getColor(context, R.color.characterDying);
        } else if (character.isPC()) {
            color = ContextCompat.getColor(context, R.color.pcColor);
        } else {
            color = ContextCompat.getColor(context, R.color.npcColor);
        }
        holder.characterCardView.setCardBackgroundColor(color);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Debuff Overview Logic    ////////////////////////////////////////////////////////////////////////////////////////////////    private void bindDebuffOverview(Context context, InitiativeAdapter.CharacterViewHolder holder, CharacterModel character) {        ArrayList<DebuffModel> debuffs = character.getDebuffList();
        if (!debuffs.isEmpty()) {
            holder.debuffOverviewTextView.setVisibility(View.VISIBLE);
            StringBuilder debuffString = new StringBuilder();
            debuffString.append(context.getResources().getString(R.string.char_card_debuffs));
            for (DebuffModel d: debuffs) {
                debuffString.append(" ");
                debuffString.append(d.getName());
                if (d.getDuration() > 0) {
                    debuffString.append(" (");
                    debuffString.append(d.getDuration());
                    debuffString.append(")");
                }
                debuffString.append(", ");
            }
            debuffString.delete(debuffString.length()-2, debuffString.length()); //Remove last comma and space
            holder.debuffOverviewTextView.setText(debuffString.toString());
        } else {
            holder.debuffOverviewTextView.setVisibility(View.GONE); //Don't show TextView if list is empty
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Reaction Section Logic    ////////////////////////////////////////////////////////////////////////////////////////////////    private void bindReactionSection(Context context, InitiativeAdapter.CharacterViewHolder holder, CharacterModel character) {        AppExtension app = (AppExtension) context.getApplicationContext();
        boolean hideReactionPref = app.getHideReactions();

        holder.reactionSwitch.setChecked(character.isReactionAvailable());

        updateReactionVisibility(holder, character, hideReactionPref);
        updateReactionAvailableText(context, holder, character);
    }

    private void updateReactionVisibility(InitiativeAdapter.CharacterViewHolder holder, CharacterModel character, boolean hideReactionPref) {
        if (hideReactionPref || character.isDying()) {
            setReactionContentVisibility(holder, View.GONE);
            holder.collapseReactionButton.setVisibility(View.GONE);
            holder.openReactionButton.setVisibility(View.GONE);
        } else if (character.isReactionCollapsed()) {
            setReactionContentVisibility(holder, View.GONE);
            holder.collapseReactionButton.setVisibility(View.INVISIBLE);
            holder.openReactionButton.setVisibility(View.VISIBLE);
        } else {
            setReactionContentVisibility(holder, View.VISIBLE);
            holder.collapseReactionButton.setVisibility(View.VISIBLE);
            holder.openReactionButton.setVisibility(View.INVISIBLE);
        }
    }

    private void setReactionContentVisibility(InitiativeAdapter.CharacterViewHolder holder, int visibility) {
        holder.reactionLabel.setVisibility(visibility);
        holder.reactionSwitch.setVisibility(visibility);
        holder.reactionsButton.setVisibility(visibility);
        holder.reactionsAvailableTextView.setVisibility(visibility);
    }

    private void updateReactionAvailableText(Context context, InitiativeAdapter.CharacterViewHolder holder, CharacterModel character) {
        int nofAvailableReactions = 0;
        if (character.getReactionList() != null) {
            for (ReactionModel r : character.getReactionList()) {
                if (r.isAvailable()) nofAvailableReactions++;
            }
        }
        if (nofAvailableReactions == 1) {
            holder.reactionsAvailableTextView.setText(context.getResources().getString(R.string.char_card_reaction_available_label));
        } else {
            holder.reactionsAvailableTextView.setText(String.format(context.getResources().getString(R.string.char_card_reactions_available_label), nofAvailableReactions));
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Dying Section Logic    ////////////////////////////////////////////////////////////////////////////////////////////////    private void bindDyingSection(Context context, InitiativeAdapter.CharacterViewHolder holder, CharacterModel character) {        if (character.isDying()) {
            updateDyingLabelAndRules(context, holder, character);
            updateDyingValueIcons(context, holder, character);
        } else {
            holder.dyingLabel.setVisibility(View.GONE);
            holder.dyingRulesImageView.setVisibility(View.GONE);
            holder.dyingLayout.setVisibility(View.GONE);
        }
    }

    private void updateDyingLabelAndRules(Context context, InitiativeAdapter.CharacterViewHolder holder, CharacterModel character) {
        holder.dyingLabel.setVisibility(View.VISIBLE);
        holder.dyingRulesImageView.setVisibility(View.VISIBLE);
        holder.dyingLayout.setVisibility(View.VISIBLE);

        int currentDyingValue = character.getDyingValue();
        int maxDyingValue = character.getMaxDyingValue() - character.getDoomedValue();
        String labelString = context.getString(R.string.char_card_dying_label, currentDyingValue, maxDyingValue,
                character.getWoundedValue(), character.getDoomedValue(), (character.getRecoveryDC() + currentDyingValue));
        holder.dyingLabel.setText(labelString);

        holder.dyingRulesImageView.setOnClickListener(v -> DyingDialogHelper.getInstance().deathDyingRulesDialog(context));
    }

    private void updateDyingValueIcons(Context context, InitiativeAdapter.CharacterViewHolder holder, CharacterModel character) {
        for (int i = 0; i < holder.dyingLayout.getChildCount(); i++) {
            View child = holder.dyingLayout.getChildAt(i);
            if (!(child instanceof TextView textView)) continue;

            int index = i / 2; // Each dying value has a TextView and an ImageView
            View imageView = holder.dyingLayout.getChildAt(i + 1);

            // Only show up to the max dying value
            if (index >= character.getMaxDyingValue()) {
                textView.setVisibility(View.GONE);
                imageView.setVisibility(View.GONE);
            } else {
                textView.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.VISIBLE);
            }

            // Mark dying value
            if (index < character.getDyingValue()) {
                textView.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.dying_dying_rounded_corners, context.getTheme()));
                ((ImageView) imageView).setImageResource(R.drawable.ic_dying_dying);
            } else {
                textView.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.dying_healthy_rounded_corners, context.getTheme()));
                ((ImageView) imageView).setImageResource(R.drawable.ic_dying_healthy);
            }

            // Mark doomed value
            if (character.getMaxDyingValue() - index <= character.getDoomedValue()) {
                textView.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.dying_doomed_rounded_corners, context.getTheme()));
                ((ImageView) imageView).setImageResource(R.drawable.ic_dying_doomed);
                textView.setText("X");
            }
        }
    }
}