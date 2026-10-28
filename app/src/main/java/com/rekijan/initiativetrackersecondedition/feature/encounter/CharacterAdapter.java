package com.rekijan.initiativetrackersecondedition.feature.encounter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.util.TextBinding;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.health.HealthDialogs;
import com.rekijan.initiativetrackersecondedition.feature.health.HpDialog;
import com.rekijan.initiativetrackersecondedition.feature.sync.SyncRules;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Shows the initiative order: one card per character with initiative and name (editable in
 * place), hp (tap to change), a summary of their (de)buffs and persistent damage, the reaction
 * row (can be folded away, or hidden for everyone from the menu) and, while dying, the dying
 * track. PCs get gold cards, NPCs bordeaux and dying characters red; the character whose turn it
 * is (the top card) gets a gold outline. While hosting Table sync, a synced PC is tagged Synced,
 * or Player left when its player isn't in the session.
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
    private final Function<CharacterModel, SyncRules.Status> syncStatus;

    /**
     * @param hideReactions Tells whether the user hid the reactions on all cards.
     * @param syncStatus    What each card shows about Table sync.
     */
    public CharacterAdapter(Encounter encounter, Navigation navigation, BooleanSupplier hideReactions,
                            Function<CharacterModel, SyncRules.Status> syncStatus) {
        this.encounter = encounter;
        this.navigation = navigation;
        this.hideReactions = hideReactions;
        this.syncStatus = syncStatus;
    }

    static class CharacterViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final View markerView;
        final EditText initiativeEditText;
        final EditText nameEditText;
        final TextView typeTextView;
        final TextView syncTextView;
        final View hpBox;
        final TextView hpTextView;
        final TextView debuffsTextView;
        final TextView persistentTextView;
        final View showDetailButton;
        final View reactionRow;
        final View reactionContent;
        final CompoundButton reactionSwitch;
        final Button reactionsButton;
        final View collapseReactionButton;
        final View openReactionButton;
        final View dyingBlock;
        final TextView dyingLabel;
        final View dyingRulesButton;
        final ViewGroup dyingTrack;

        CharacterViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.initiative_character_cardView);
            markerView = itemView.findViewById(R.id.initiative_character_marker_view);
            initiativeEditText = itemView.findViewById(R.id.initiative_character_initiative_editText);
            nameEditText = itemView.findViewById(R.id.initiative_character_name_editText);
            typeTextView = itemView.findViewById(R.id.initiative_character_type_textView);
            syncTextView = itemView.findViewById(R.id.character_sync_textView);
            hpBox = itemView.findViewById(R.id.initiative_character_hp_box);
            hpTextView = itemView.findViewById(R.id.initiative_character_hp_textView);
            debuffsTextView = itemView.findViewById(R.id.initiative_character_debuff_overview_textView);
            persistentTextView = itemView.findViewById(R.id.initiative_character_persistent_textView);
            showDetailButton = itemView.findViewById(R.id.initiative_character_detail_button);
            reactionRow = itemView.findViewById(R.id.initiative_character_reaction_row);
            reactionContent = itemView.findViewById(R.id.initiative_character_reaction_content);
            reactionSwitch = itemView.findViewById(R.id.initiative_character_reaction_available_switch);
            reactionsButton = itemView.findViewById(R.id.initiative_character_reactions_button);
            collapseReactionButton = itemView.findViewById(R.id.initiative_character_collapse_reaction_button);
            openReactionButton = itemView.findViewById(R.id.initiative_character_open_reaction_button);
            dyingBlock = itemView.findViewById(R.id.initiative_character_dying_block);
            dyingLabel = itemView.findViewById(R.id.initiative_character_dying_label);
            dyingRulesButton = itemView.findViewById(R.id.initiative_character_dying_rules_imageView);
            dyingTrack = itemView.findViewById(R.id.initiative_character_dying_layout);
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

        bindColors(holder, context, character, position == 0);
        bindSyncTag(holder.syncTextView, context, syncStatus.apply(character));

        TextBinding.bindInt(holder.initiativeEditText, character.getInitiative(),
                CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE, character::setInitiative);
        TextBinding.bindText(holder.nameEditText, character.getCharacterName(), character::setCharacterName);

        holder.hpTextView.setText(hpText(holder.hpTextView, character));
        holder.hpBox.setOnClickListener(v -> HpDialog.show(context, encounter, character));

        bindDebuffs(context, holder, character.getDebuffList());

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

    /**
     * Gold for a PC, bordeaux for an NPC, red while dying: the card tint, marker stripe and type
     * label ("PC · 2 hero points", "NPC", "PC · Dying"). A gold outline on the active character.
     */
    /** Synced in green, Player left in amber, or nothing; the outline takes the same color. */
    private static void bindSyncTag(TextView tag, Context context, SyncRules.Status status) {
        if (status == SyncRules.Status.NONE) {
            tag.setVisibility(View.GONE);
            return;
        }
        boolean synced = status == SyncRules.Status.SYNCED;
        int color = ContextCompat.getColor(context, synced ? R.color.rk_status_on : R.color.rk_status_wait);
        tag.setText(synced ? R.string.char_card_synced : R.string.char_card_player_left);
        tag.setTextColor(color);
        tag.setBackgroundTintList(ColorStateList.valueOf(color));
        tag.setVisibility(View.VISIBLE);
    }

    private static void bindColors(CharacterViewHolder holder, Context context, CharacterModel character,
                                   boolean isActive) {
        boolean dying = character.isDying();
        boolean pc = character.isPC();
        @ColorRes int markerRes = dying ? R.color.rk_dying : pc ? R.color.rk_pc_marker : R.color.rk_npc_marker;
        @ColorRes int containerRes = dying ? R.color.rk_dying_container
                : pc ? R.color.rk_pc_container : R.color.rk_npc_container;
        int marker = ContextCompat.getColor(context, markerRes);
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, containerRes));
        holder.markerView.setBackgroundColor(marker);

        StringBuilder type = new StringBuilder(context.getString(pc ? R.string.char_card_pc : R.string.char_card_npc));
        if (dying) {
            type.append(" · ").append(context.getString(R.string.char_card_dying));
        } else if (pc && character.getHeroPoints() > 0) {
            type.append(" · ").append(context.getResources().getQuantityString(R.plurals.char_card_hero_points,
                    character.getHeroPoints(), character.getHeroPoints()));
        }
        holder.typeTextView.setText(type);
        holder.typeTextView.setTextColor(marker);

        holder.cardView.setStrokeColor(
                MaterialColors.getColor(holder.cardView, com.google.android.material.R.attr.colorSecondary));
        holder.cardView.setStrokeWidth(isActive
                ? context.getResources().getDimensionPixelSize(R.dimen.active_card_stroke) : 0);
    }

    /** E.g. "24 / 32", with the max smaller and muted; just the hp when there is no max. */
    private static CharSequence hpText(TextView view, CharacterModel character) {
        SpannableStringBuilder sb = new SpannableStringBuilder(String.valueOf(character.getHp()));
        if (character.getMaxHp() <= 0) return sb;
        int start = sb.length();
        sb.append(" / ").append(String.valueOf(character.getMaxHp()));
        int muted = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurfaceVariant);
        sb.setSpan(new RelativeSizeSpan(0.65f), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(muted), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    /** The (de)buffs on one line, persistent damage on its own line below. */
    private static void bindDebuffs(Context context, CharacterViewHolder holder, List<DebuffModel> debuffs) {
        SpannableStringBuilder conditions = new SpannableStringBuilder();
        SpannableStringBuilder persistent = new SpannableStringBuilder();
        for (DebuffModel d : debuffs) {
            if (d.isPersistentDamage()) {
                appendPersistent(context, persistent, d);
            } else {
                appendCondition(conditions, d);
            }
        }
        holder.debuffsTextView.setVisibility(conditions.length() == 0 ? View.GONE : View.VISIBLE);
        holder.debuffsTextView.setText(conditions);
        holder.persistentTextView.setVisibility(persistent.length() == 0 ? View.GONE : View.VISIBLE);
        holder.persistentTextView.setText(persistent);
    }

    /** E.g. "Frightened 2 · Off-Guard (1)": a value in bold, a duration of 0 left out. */
    private static void appendCondition(SpannableStringBuilder sb, DebuffModel d) {
        if (sb.length() > 0) sb.append(" · ");
        int start = sb.length();
        sb.append(d.getDisplayName());
        if (d.getValue() > 0) {
            sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (d.getDuration() > 0) sb.append(" (").append(String.valueOf(d.getDuration())).append(")");
    }

    /** E.g. "1d6 fire · DC 15", the damage in bold; the name when no damage was entered. */
    private static void appendPersistent(Context context, SpannableStringBuilder sb, DebuffModel d) {
        if (sb.length() > 0) sb.append("\n");
        int start = sb.length();
        String damage = (d.getDamageValue() + " " + d.getDamageType()).trim();
        sb.append(damage.isEmpty() ? d.getName() : damage);
        sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (d.getDifficultyClass() > 0) {
            sb.append(" · ").append(context.getString(R.string.char_card_persistent_dc, d.getDifficultyClass()));
        }
        if (d.getDuration() > 0) sb.append(" (").append(String.valueOf(d.getDuration())).append(")");
    }

    private void bindReactions(Context context, CharacterViewHolder holder, CharacterModel character) {
        boolean hidden = hideReactions.getAsBoolean() || character.isDying();
        boolean collapsed = character.isReactionCollapsed();
        holder.reactionRow.setVisibility(hidden ? View.GONE : View.VISIBLE);
        holder.reactionContent.setVisibility(collapsed ? View.GONE : View.VISIBLE);
        holder.openReactionButton.setVisibility(collapsed ? View.VISIBLE : View.GONE);

        holder.reactionSwitch.setOnCheckedChangeListener(null);
        holder.reactionSwitch.setChecked(character.isReactionAvailable());
        holder.reactionSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            character.setReactionAvailable(isChecked);
            // Tells Table sync too, so a synced player sees the reaction used
            encounter.notifyChanged();
        });

        int total = character.getReactionList().size();
        int available = character.getAvailableReactionCount();
        holder.reactionsButton.setText(total == 0
                ? context.getString(R.string.char_card_other_reactions)
                : context.getResources().getQuantityString(R.plurals.char_card_reactions_available, available, available));

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
        holder.dyingBlock.setVisibility(character.isDying() ? View.VISIBLE : View.GONE);
        if (!character.isDying()) return;

        holder.dyingLabel.setText(context.getString(R.string.char_card_dying_label, character.getDyingValue(),
                character.getDeathThreshold(), character.getWoundedValue(), character.getDoomedValue(),
                character.getRecoveryCheckDc()));
        holder.dyingRulesButton.setOnClickListener(v -> HealthDialogs.showDyingRules(context));

        // One step per point up to the max dying value: red while dying, plum with a skull when
        // doomed takes it (counted from the end), empty otherwise
        for (int i = 0; i < holder.dyingTrack.getChildCount(); i++) {
            TextView step = (TextView) holder.dyingTrack.getChildAt(i);
            step.setVisibility(i < character.getMaxDyingValue() ? View.VISIBLE : View.GONE);

            boolean doomed = character.getMaxDyingValue() - i <= character.getDoomedValue();
            boolean dying = !doomed && i < character.getDyingValue();
            int background = doomed ? R.color.rk_doomed : dying ? R.color.rk_dying : R.color.rk_track_empty;
            int foreground = doomed ? ContextCompat.getColor(context, R.color.rk_on_doomed)
                    : dying ? ContextCompat.getColor(context, R.color.rk_on_dying)
                    : MaterialColors.getColor(step, com.google.android.material.R.attr.colorOnSurfaceVariant);
            step.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, background)));
            step.setTextColor(foreground);
            step.setText(stepText(context, step, doomed ? R.drawable.ic_doomed : dying ? R.drawable.ic_dying : 0,
                    doomed ? "" : String.valueOf(i + 1), foreground));
            step.setContentDescription(doomed
                    ? context.getString(R.string.char_card_dying_step_doomed, i + 1)
                    : context.getString(dying ? R.string.char_card_dying_step_on : R.string.char_card_dying_step_off, i + 1));
        }
    }

    /**
     * The icon (a broken heart or a skull) inline in front of the number, so both stay centered
     * in the step.
     */
    private static CharSequence stepText(Context context, TextView step, @DrawableRes int icon, String number,
                                         int color) {
        if (icon == 0) return number;
        Drawable drawable = AppCompatResources.getDrawable(context, icon);
        if (drawable == null) return number;
        drawable = drawable.mutate();
        drawable.setTint(color);
        int size = Math.round(step.getTextSize());
        drawable.setBounds(0, 0, size, size);
        SpannableStringBuilder sb = new SpannableStringBuilder(" ");
        sb.setSpan(new ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BOTTOM), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (!number.isEmpty()) sb.append(" ").append(number);
        return sb;
    }
}
