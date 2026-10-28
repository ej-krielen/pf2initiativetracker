package com.rekijan.initiativetrackersecondedition.feature.character;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewParent;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.util.NumberInput;
import com.rekijan.initiativetrackersecondedition.core.util.TextBinding;
import com.rekijan.initiativetrackersecondedition.feature.condition.DebuffAdapter;
import com.rekijan.initiativetrackersecondedition.feature.reaction.ReactionAdapter;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Binds a character to the sheet layout (view_character_sheet): the Stats tab with the PC and
 * dying switches, the numbers, the dying track numbers (dying, max dying, wounded, doomed,
 * recovery DC), hero points, the free-text stat lines and notes, and the Conditions tab with the
 * conditions, buffs and persistent damage. Every edit goes straight into the character and is
 * reported to the change callback.
 * <p>
 * Used by the GM's character details and by the player screen. The player sheet also shows name,
 * initiative and hp (the GM edits those on the card) and hides the PC switch, since a player's
 * own character is always a PC. Add opens the condition wizard through the add callback. The
 * player sheet also has the reaction and the custom reactions, which the GM has on the card.
 * <p>
 * On the player's sheet, fields that differ from the GM's version can be marked as not sent yet
 * ({@link #markUnsent}).
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public class CharacterSheet {

    public static final int TAB_STATS = 0;
    public static final int TAB_CONDITIONS = 1;

    /** The edit texts that can be marked as not sent yet. */
    private static final int[] MARKABLE_FIELDS = {
            R.id.sheet_name_editText, R.id.sheet_initiative_editText, R.id.sheet_hp_editText,
            R.id.initiative_bonus_editText, R.id.max_hp_editText, R.id.fast_healing_editText,
            R.id.regeneration_editText, R.id.dying_editText, R.id.max_dying_editText, R.id.wounded_editText,
            R.id.doomed_editText, R.id.recovery_dc_editText, R.id.hero_points_editText, R.id.skills_editText,
            R.id.attack_routine_editText, R.id.ac_editText, R.id.saves_editText, R.id.notes_editText};
    /** The switches that can be marked as not sent yet. */
    private static final int[] MARKABLE_SWITCHES = {R.id.is_dying_switch, R.id.sheet_reaction_available_switch};

    private final View root;
    private final boolean playerMode;
    private final Runnable onChanged;
    private final TabLayout tabs;
    private final View statsPane;
    private final View conditionsPane;
    private final RecyclerView conditionList;
    private final TextView conditionsEmpty;
    private CharacterModel character;
    private DebuffAdapter adapter;
    private final RecyclerView reactionList;
    private boolean conditionsUnsent;
    private Toast maxDyingToast;

    /**
     * @param root           The inflated view_character_sheet.
     * @param playerMode     True for the player's own character, false for the GM's details.
     * @param onChanged      Called after every edit, e.g. to redraw the encounter or save.
     * @param onAddCondition Called on Add, to open the condition wizard for this character.
     */
    public CharacterSheet(View root, boolean playerMode, Runnable onChanged, Runnable onAddCondition) {
        this.root = root;
        this.playerMode = playerMode;
        this.onChanged = onChanged;
        tabs = root.findViewById(R.id.sheet_tabs);
        statsPane = root.findViewById(R.id.sheet_stats_pane);
        conditionsPane = root.findViewById(R.id.sheet_conditions_pane);
        conditionList = root.findViewById(R.id.debuff_recyclerView);
        conditionsEmpty = root.findViewById(R.id.sheet_conditions_empty_textView);
        conditionList.setLayoutManager(new LinearLayoutManager(root.getContext()));
        reactionList = root.findViewById(R.id.sheet_reactions_recyclerView);
        reactionList.setLayoutManager(new LinearLayoutManager(root.getContext()));

        root.findViewById(R.id.is_pc_switch).setVisibility(playerMode ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.sheet_player_rows).setVisibility(playerMode ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.sheet_player_reactions).setVisibility(playerMode ? View.VISIBLE : View.GONE);

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showPane(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Nothing to do
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Nothing to do
            }
        });
        root.findViewById(R.id.clear_all_debuffs_button).setOnClickListener(v -> clearConditions());
        root.findViewById(R.id.add_debuff_button).setOnClickListener(v -> onAddCondition.run());
        root.findViewById(R.id.sheet_add_reaction_button).setOnClickListener(v -> addReaction());
    }

    /** Shows this character on the sheet. */
    public void bind(CharacterModel character) {
        this.character = character;

        bindSwitch(R.id.is_pc_switch, character.isPC(), character::setPC);
        bindSwitch(R.id.is_dying_switch, character.isDying(), character::setDying);

        if (playerMode) {
            bindText(R.id.sheet_name_editText, character.getCharacterName(), character::setCharacterName);
            bindInt(R.id.sheet_initiative_editText, character.getInitiative(),
                    CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE, character::setInitiative);
            bindInt(R.id.sheet_hp_editText, character.getHp(),
                    CharacterModel.MIN_HP, CharacterModel.MAX_HP, character::setHp);
            bindSwitch(R.id.sheet_reaction_available_switch, character.isReactionAvailable(), character::setReactionAvailable);
            reactionList.setAdapter(new ReactionAdapter(character, onChanged));
        }
        bindInt(R.id.initiative_bonus_editText, character.getInitiativeBonus(),
                CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE, character::setInitiativeBonus);
        bindInt(R.id.max_hp_editText, character.getMaxHp(), 0, CharacterModel.MAX_HP, character::setMaxHp);
        bindInt(R.id.fast_healing_editText, character.getFastHealing(),
                0, CharacterModel.MAX_HEALING, character::setFastHealing);
        bindInt(R.id.regeneration_editText, character.getRegeneration(),
                0, CharacterModel.MAX_HEALING, character::setRegeneration);
        bindInt(R.id.dying_editText, character.getDyingValue(),
                0, CharacterModel.MAX_CONDITION_VALUE, character::setDyingValue);
        bindInt(R.id.wounded_editText, character.getWoundedValue(),
                0, CharacterModel.MAX_CONDITION_VALUE, character::setWoundedValue);
        bindInt(R.id.doomed_editText, character.getDoomedValue(),
                0, CharacterModel.MAX_CONDITION_VALUE, character::setDoomedValue);
        bindInt(R.id.recovery_dc_editText, character.getRecoveryDC(),
                0, CharacterModel.MAX_RECOVERY_DC, character::setRecoveryDC);
        bindInt(R.id.hero_points_editText, character.getHeroPoints(),
                0, CharacterModel.MAX_HERO_POINTS, character::setHeroPoints);
        TextBinding.bindText(root.findViewById(R.id.max_dying_editText),
                String.valueOf(character.getMaxDyingValue()), this::onMaxDyingChanged);
        bindText(R.id.skills_editText, character.getSkills(), character::setSkills);
        bindText(R.id.attack_routine_editText, character.getAttackRoutine(), character::setAttackRoutine);
        bindText(R.id.ac_editText, character.getAc(), character::setAc);
        bindText(R.id.saves_editText, character.getSaves(), character::setSaves);
        bindText(R.id.notes_editText, character.getCharacterNotes(), character::setCharacterNotes);

        adapter = new DebuffAdapter(character, this::onConditionsChanged);
        conditionList.setAdapter(adapter);
        updateConditionCount();
    }

    /** @return The open tab, {@link #TAB_STATS} or {@link #TAB_CONDITIONS}, to keep it on rotation. */
    public int getSelectedTab() {
        return tabs.getSelectedTabPosition();
    }

    public void selectTab(int tab) {
        TabLayout.Tab t = tabs.getTabAt(tab);
        if (t != null) t.select();
        showPane(tab);
    }

    private void showPane(int tab) {
        statsPane.setVisibility(tab == TAB_STATS ? View.VISIBLE : View.GONE);
        conditionsPane.setVisibility(tab == TAB_CONDITIONS ? View.VISIBLE : View.GONE);
    }

    /** Player sheet: a new, available custom reaction at the end of the list. */
    private void addReaction() {
        character.getReactionList().add(new ReactionModel());
        RecyclerView.Adapter<?> reactions = reactionList.getAdapter();
        if (reactions != null) reactions.notifyItemInserted(character.getReactionList().size() - 1);
        onChanged.run();
    }

    /**
     * Marks the fields that are not sent to the GM yet: a gold outline and "Not sent yet" on edit
     * texts, gold text on switches and the Reactions header, and a dot on the Conditions tab.
     *
     * @param ids        The ids of the edit texts and switches of the unsent fields.
     * @param reactions  True when the custom reactions are not sent yet.
     * @param conditions True when the conditions are not sent yet.
     */
    public void markUnsent(Set<Integer> ids, boolean reactions, boolean conditions) {
        Context context = root.getContext();
        ColorStateList gold = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.rk_pc_marker));
        ColorStateList normal = ContextCompat.getColorStateList(context, R.color.text_input_stroke);
        for (int id : MARKABLE_FIELDS) {
            TextInputLayout layout = layoutOf(root.findViewById(id));
            if (layout == null) continue;
            boolean unsent = ids.contains(id);
            layout.setHelperText(unsent ? context.getString(R.string.sheet_not_sent) : null);
            if (unsent) layout.setHelperTextColor(gold);
            if (normal != null) layout.setBoxStrokeColorStateList(unsent ? gold : normal);
        }
        for (int id : MARKABLE_SWITCHES) {
            markText(root.findViewById(id), ids.contains(id), gold);
        }
        markText(root.findViewById(R.id.sheet_reactions_header_textView), reactions, gold);
        conditionsUnsent = conditions;
        updateConditionCount();
    }

    /** Gold text while unsent; the color from the layout otherwise, kept in the tag. */
    private static void markText(TextView view, boolean unsent, ColorStateList gold) {
        if (view.getTag(R.id.tag_text_color) == null) {
            view.setTag(R.id.tag_text_color, view.getTextColors());
        }
        view.setTextColor(unsent ? gold : (ColorStateList) view.getTag(R.id.tag_text_color));
    }

    private static TextInputLayout layoutOf(View view) {
        ViewParent parent = view != null ? view.getParent() : null;
        while (parent != null && !(parent instanceof TextInputLayout)) parent = parent.getParent();
        return (TextInputLayout) parent;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void clearConditions() {
        character.getDebuffList().clear();
        adapter.notifyDataSetChanged();
        onConditionsChanged();
    }

    private void onConditionsChanged() {
        updateConditionCount();
        onChanged.run();
    }

    private void updateConditionCount() {
        int count = character.getDebuffList().size();
        TabLayout.Tab tab = tabs.getTabAt(TAB_CONDITIONS);
        if (tab != null) {
            String text = count == 0 ? root.getContext().getString(R.string.sheet_tab_conditions)
                    : root.getContext().getString(R.string.sheet_tab_conditions_count, count);
            tab.setText(conditionsUnsent ? root.getContext().getString(R.string.sheet_tab_unsent, text) : text);
        }
        conditionsEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    /** Max dying above {@link CharacterModel#MAX_DYING_VALUE} is capped, with a short explanation. */
    private void onMaxDyingChanged(String text) {
        int value = NumberInput.parseInt(text, CharacterModel.DEFAULT_MAX_DYING, 1, Integer.MAX_VALUE);
        if (value > CharacterModel.MAX_DYING_VALUE) {
            if (maxDyingToast != null) maxDyingToast.cancel();
            maxDyingToast = Toast.makeText(root.getContext(), root.getContext().getString(
                    R.string.max_dying_value_invalid, CharacterModel.MAX_DYING_VALUE), Toast.LENGTH_SHORT);
            maxDyingToast.show();
        }
        character.setMaxDyingValue(value);
        onChanged.run();
    }

    private void bindSwitch(int id, boolean value, Consumer<Boolean> setter) {
        CompoundButton button = root.findViewById(id);
        button.setOnCheckedChangeListener(null);
        button.setChecked(value);
        button.setOnCheckedChangeListener((b, isChecked) -> {
            setter.accept(isChecked);
            onChanged.run();
        });
    }

    private void bindText(int id, String value, Consumer<String> setter) {
        EditText editText = root.findViewById(id);
        TextBinding.bindText(editText, value, text -> {
            setter.accept(text);
            onChanged.run();
        });
    }

    private void bindInt(int id, int value, int min, int max, IntConsumer setter) {
        TextBinding.bindInt(root.findViewById(id), value, min, max, number -> {
            setter.accept(number);
            onChanged.run();
        });
    }
}
