package com.rekijan.initiativetrackersecondedition.features.initiative;

import android.app.Activity;
import android.text.TextUtils;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.common.helper.DialogHelper;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;

/**
 * Handles user actions related to the initiative list.
 * Coordinates between the UI requests and the data/adapter updates.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 21-6-2026
 */
public class EncounterActionHandler {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private final Activity activity;    private final InitiativeManager manager;
    private final InitiativeAdapter adapter;

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Constructor    ////////////////////////////////////////////////////////////////////////////////////////////////    public EncounterActionHandler(Activity activity, InitiativeManager manager, InitiativeAdapter adapter) {        this.activity = activity;
        this.manager = manager;
        this.adapter = adapter;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods    ////////////////////////////////////////////////////////////////////////////////////////////////    public void addCharacter() {        manager.addCharacter(new CharacterModel(activity));
        adapter.notifyItemInserted(manager.getCharacters().size() - 1);
    }

    public void confirmDeleteCharacter(int position) {
        final CharacterModel character = manager.getCharacters().get(position);
        final String name = TextUtils.isEmpty(character.getCharacterName()) ? 
                activity.getString(R.string.empty_character_name) : 
                character.getCharacterName();

        String message = activity.getString(R.string.dialog_delete) + " " + name + "?";
        DialogHelper.getInstance().confirmationDialog(activity,
                activity.getString(R.string.dialog_delete_title), message, () -> {
                    manager.removeCharacterAt(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(0, manager.getCharacters().size());
                });
    }

    public void askSortRoundResetConfirmation(Runnable onReset) {
        boolean warnForDoubleInitiative = manager.sortInitiative();
        adapter.notifyItemRangeChanged(0, manager.getCharacters().size());

        String message = warnForDoubleInitiative ? 
                activity.getString(R.string.dialog_reset_round_counter) + activity.getString(R.string.dialog_reset_round_double_init) : 
                activity.getString(R.string.dialog_reset_round_counter);
        
        DialogHelper.getInstance().confirmationDialog(activity,
                activity.getString(R.string.dialog_reset_round_counter_title), message, onReset);
    }
}