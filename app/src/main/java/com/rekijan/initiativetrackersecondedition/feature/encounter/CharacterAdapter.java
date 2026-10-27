package com.rekijan.initiativetrackersecondedition.feature.encounter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.util.TextBinding;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.health.HealthDialogs;
import com.rekijan.initiativetrackersecondedition.feature.health.HpDialog;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Shows the initiative order: one card per character with initiative and name (editable in
 * place), hp (tap to change), a summary of their (de)buffs, the reaction block (can be folded
 * away, or hidden for everyone from the menu) and, while dying, the dying track.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 17-9-2015
 */
public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder> {

    /** What the buttons on a card open, by position in the order. */
    public interface Navigation {
        void showDetails(int position);

        void showReactions(int position);
    }

    private final Encounter encounter;
    private final Navigation navigation;
    private final BooleanSupplier hideReactions;

    /**
     * @param hideReactions Tells whether the user hid the reactions on all cards.
     */
    public CharacterAdapter(Encounter encounter, Navigation navigation, BooleanSupplier hideReactions) {
        this.encounter = encounter;
        this.navigation = navigation;
        this.hideReactions = hideReactions;
    }

    static class CharacterViewHolder extends RecyclerView.ViewHolder {
        final CardView cardView;
        final EditText initiativeEditText;
        final EditText nameEditText;
        final TextView hpTextView;
        final TextView debuffsTextView;
        final Button showDetailButton;
        final CompoundButton reactionSwitch;
        final TextView reactionLabel;
        final TextView reactionsAvailableTextView;
        final Button reactionsButton;
        final Button collapseReactionButton;
        final Button openReactionButton;
        final TextView dyingLabel;
        final ImageView dyingRulesImageView;
        final ViewGroup dyingLayout;

        CharacterViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.initiative_character_cardView);
            initiativeEditText = itemView.findViewById(R.id.initiative_character_initiative_editText);
            nameEditText = itemView.findViewById(R.id.initiative_character_name_editText);
            hpTextView = itemView.findViewById(R.id.initiative_character_hp_textView);
            debuffsTextView = itemView.findViewById(R.id.initiative_character_debuff_overview_textView);
            showDetailButton = itemView.findViewById(R.id.initiative_character_detail_button);
            reactionSwitch = itemView.findViewById(R.id.initiative_character_reaction_available_switch);
            reactionLabel = itemView.findViewById(R.id.initiative_character_reaction_label);
            reactionsAvailableTextView = itemView.findViewById(R.id.initiative_character_reactions_available_textView);
            reactionsButton = itemView.findViewById(R.id.initiative_character_reactions_button);
            collapseReactionButton = itemView.findViewById(R.id.initiative_character_collapse_reaction_button);
            openReactionButton = itemView.findViewById(R.id.initiative_character_open_reaction_button);
            dyingLabel = itemView.findViewById(R.id.initiative_character_dying_label);
            dyingRulesImageView = itemView.findViewById(R.id.initiative_character_dying_rules_imageView);
            dyingLayout = itemView.findViewById(R.id.initiative_character_dying_layout);
        }
    }

    @NonNull
    @Override
    public CharacterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_character, parent, false);
        return new CharacterViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CharacterViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        CharacterModel character = encounter.get(position);

        int color = character.isDying() ? R.color.characterDying : character.isPC() ? R.color.pcColor : R.color.npcColor;
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, color));

        TextBinding.bindInt(holder.initiativeEditText, character.getInitiative(),
                CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE, character::setInitiative);
        TextBinding.bindText(holder.nameEditText, character.getCharacterName(), character::setCharacterName);

        holder.hpTextView.setText(String.valueOf(character.getHp()));
        holder.hpTextView.setOnClickListener(v -> HpDialog.show(context, encounter, character));

        List<DebuffModel> debuffs = character.getDebuffList();
        if (debuffs.isEmpty()) {
            holder.debuffsTextView.setVisibility(View.GONE);
        } else {
            holder.debuffsTextView.setVisibility(View.VISIBLE);
            holder.debuffsTextView.setText(debuffSummary(context, debuffs));
        }

        holder.showDetailButton.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) navigation.showDetails(pos);
        });
        bindReactions(context, holder, character);
        bindDying(context, holder, character);
    }

    @Override
    public int getItemCount() {
        return encounter.size();
    }

    private void bindReactions(Context context, CharacterViewHolder holder, CharacterModel character) {
        boolean hidden = hideReactions.getAsBoolean() || character.isDying();
        boolean collapsed = character.isReactionCollapsed();
        int contentVisibility = hidden || collapsed ? View.GONE : View.VISIBLE;
        holder.reactionLabel.setVisibility(contentVisibility);
        holder.reactionSwitch.setVisibility(contentVisibility);
        holder.reactionsButton.setVisibility(contentVisibility);
        holder.reactionsAvailableTextView.setVisibility(contentVisibility);
        holder.collapseReactionButton.setVisibility(hidden ? View.GONE : collapsed ? View.INVISIBLE : View.VISIBLE);
        holder.openReactionButton.setVisibility(hidden ? View.GONE : collapsed ? View.VISIBLE : View.INVISIBLE);

        holder.reactionSwitch.setOnCheckedChangeListener(null);
        holder.reactionSwitch.setChecked(character.isReactionAvailable());
        holder.reactionSwitch.setOnCheckedChangeListener((button, isChecked) -> character.setReactionAvailable(isChecked));

        int available = character.getAvailableReactionCount();
        holder.reactionsAvailableTextView.setText(available == 1
                ? context.getString(R.string.char_card_reaction_available_label)
                : context.getString(R.string.char_card_reactions_available_label, available));

        holder.reactionsButton.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) navigation.showReactions(pos);
        });
        holder.collapseReactionButton.setOnClickListener(v -> {
            character.setReactionCollapsed(true);
            encounter.notifyChanged();
        });
        holder.openReactionButton.setOnClickListener(v -> {
            character.setReactionCollapsed(false);
            encounter.notifyChanged();
        });
    }

    private static void bindDying(Context context, CharacterViewHolder holder, CharacterModel character) {
        int visibility = character.isDying() ? View.VISIBLE : View.GONE;
        holder.dyingLabel.setVisibility(visibility);
        holder.dyingRulesImageView.setVisibility(visibility);
        holder.dyingLayout.setVisibility(visibility);
        if (!character.isDying()) return;

        holder.dyingLabel.setText(context.getString(R.string.char_card_dying_label, character.getDyingValue(),
                character.getDeathThreshold(), character.getWoundedValue(), character.getDoomedValue(),
                character.getRecoveryCheckDc()));
        holder.dyingRulesImageView.setOnClickListener(v -> HealthDialogs.showDyingRules(context));

        // The track has a number and an icon per dying step, up to the max dying value
        for (int i = 0; i + 1 < holder.dyingLayout.getChildCount(); i += 2) {
            TextView step = (TextView) holder.dyingLayout.getChildAt(i);
            ImageView icon = (ImageView) holder.dyingLayout.getChildAt(i + 1);
            int index = i / 2;
            int stepVisibility = index < character.getMaxDyingValue() ? View.VISIBLE : View.GONE;
            step.setVisibility(stepVisibility);
            icon.setVisibility(stepVisibility);

            boolean doomed = character.getMaxDyingValue() - index <= character.getDoomedValue();
            boolean dying = index < character.getDyingValue();
            step.setText(doomed ? "X" : String.valueOf(index + 1));
            setStep(context, step, icon,
                    doomed ? R.drawable.dying_doomed_rounded_corners
                            : dying ? R.drawable.dying_dying_rounded_corners : R.drawable.dying_healthy_rounded_corners,
                    doomed ? R.drawable.ic_dying_doomed : dying ? R.drawable.ic_dying_dying : R.drawable.ic_dying_healthy);
        }
    }

    private static void setStep(Context context, TextView step, ImageView icon,
                                @DrawableRes int background, @DrawableRes int image) {
        step.setBackground(ContextCompat.getDrawable(context, background));
        icon.setImageResource(image);
    }

    /** E.g. "(De)buffs: Frightened 2, Off-Guard (1)"; a duration of 0 is left out. */
    private static String debuffSummary(Context context, List<DebuffModel> debuffs) {
        StringBuilder sb = new StringBuilder(context.getString(R.string.char_card_debuffs));
        for (int i = 0; i < debuffs.size(); i++) {
            DebuffModel d = debuffs.get(i);
            sb.append(i == 0 ? " " : ", ").append(d.getDisplayName());
            if (d.getDuration() > 0) sb.append(" (").append(d.getDuration()).append(")");
        }
        return sb.toString();
    }
}
