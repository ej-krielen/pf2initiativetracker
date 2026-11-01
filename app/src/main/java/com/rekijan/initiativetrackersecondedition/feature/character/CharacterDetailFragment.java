package com.rekijan.initiativetrackersecondedition.feature.character;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.app.CombatTrackerApp;
import com.rekijan.initiativetrackersecondedition.app.MainActivity;
import com.rekijan.initiativetrackersecondedition.core.util.DialogHost;
import com.rekijan.initiativetrackersecondedition.core.util.Dialogs;
import com.rekijan.initiativetrackersecondedition.core.util.TextBinding;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;
import com.rekijan.initiativetrackersecondedition.feature.spell.SpellDialogs;
import com.rekijan.initiativetrackersecondedition.feature.sync.PlayerSheet;
import com.rekijan.initiativetrackersecondedition.feature.sync.SyncManager;
import com.rekijan.initiativetrackersecondedition.feature.sync.SyncRules;

/**
 * The details of one character in the GM's encounter: the name, then the sheet with the Stats and
 * Conditions tabs ({@link CharacterSheet}): PC or not, dying or not, initiative bonus, max hp, fast
 * healing, regeneration, the dying track (dying, max dying, wounded, doomed, recovery DC), hero
 * points, the free-text stat lines, notes and the conditions (a spell can be ended on all its
 * targets from there). On a phone it replaces the list; on
 * a tablet it is shown next to it. While hosting Table sync, a synced PC shows that under the
 * name, with Unlink: the GM runs it from then on. When the GM approves a change from the player
 * while this screen is open, the sheet shows it right away.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class CharacterDetailFragment extends Fragment implements SyncManager.StateListener {

    private static final String ARG_POSITION = "POSITION";
    private static final String STATE_TAB = "sheet_tab";

    private CharacterSheet sheet;
    private Encounter encounter;
    private int tab = CharacterSheet.TAB_STATS;
    private SyncManager sync;
    private CharacterModel character;
    /** What the sheet shows, to see when an approval changed the character behind it. */
    private PlayerSheet shown;

    public static CharacterDetailFragment newInstance(int position) {
        return newInstance(position, CharacterSheet.TAB_STATS);
    }

    /** @param tab The tab to open on, {@link CharacterSheet#TAB_STATS} or {@link CharacterSheet#TAB_CONDITIONS}. */
    public static CharacterDetailFragment newInstance(int position, int tab) {
        CharacterDetailFragment fragment = new CharacterDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        fragment.tab = tab;
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_character_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CombatTrackerApp app = (CombatTrackerApp) requireActivity().getApplication();
        encounter = app.getEncounter();
        sync = app.getSync();
        int position = requireArguments().getInt(ARG_POSITION);
        if (!encounter.isValidPosition(position)) {
            // The character was removed (e.g. before a rotation); show nothing
            view.setVisibility(View.INVISIBLE);
            return;
        }
        character = encounter.get(position);

        bindName();
        sheet = new CharacterSheet(view.findViewById(R.id.character_sheet), false, () -> {
                    shown = PlayerSheet.of(character);
                    encounter.notifyChanged();
                },
                () -> ((MainActivity) requireActivity()).showConditionWizard(encounter.indexOf(character)));
        sheet.setOnEndSpell(spell -> SpellDialogs.askEnd(requireContext(), encounter, spell.getSpellId(),
                spell.getName(), false, sheet::refreshConditions));
        sheet.setGmEncounter(encounter);
        sheet.bind(character);
        shown = PlayerSheet.of(character);
        if (savedInstanceState != null) tab = savedInstanceState.getInt(STATE_TAB, tab);
        sheet.selectTab(tab);

        String shownName = character.getCharacterName().trim().isEmpty()
                ? getString(R.string.empty_character_name) : character.getCharacterName();
        view.findViewById(R.id.character_unlink_button).setOnClickListener(v -> DialogHost.show(c -> Dialogs.builder(c)
                .setTitle(c.getString(R.string.sync_unlink_title, shownName))
                .setMessage(c.getString(R.string.sync_unlink_message, shownName))
                .setPositiveButton(R.string.sync_unlink, (d, w) -> sync.unlink(character))
                .setNegativeButton(R.string.dialog_cancel, null)
                .create()));
        renderSync();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (sync != null) sync.addStateListener(this);
        renderSync();
    }

    @Override
    public void onStop() {
        if (sync != null) sync.removeStateListener(this);
        super.onStop();
    }

    @Override
    public void onSyncStateChanged() {
        // An approved change from the player: show it
        if (sheet != null && character != null && !PlayerSheet.of(character).toMap().equals(shown.toMap())) {
            sheet.bind(character);
            bindName();
            shown = PlayerSheet.of(character);
        }
        renderSync();
    }

    /** The name field: an edit goes straight into the character and redraws its card. */
    private void bindName() {
        View view = getView();
        if (view == null) return;
        EditText nameEditText = view.findViewById(R.id.character_detail_name_editText);
        TextBinding.bindText(nameEditText, character.getCharacterName(), name -> {
            character.setCharacterName(name);
            shown = PlayerSheet.of(character);
            encounter.notifyChanged();
        });
    }

    /** The sync row: shown while the character is synced, or linked to a player who left. */
    private void renderSync() {
        View view = getView();
        if (view == null || character == null) return;
        SyncRules.Status status = sync.getStatus(character);
        view.findViewById(R.id.character_sync_row).setVisibility(
                status == SyncRules.Status.NONE ? View.GONE : View.VISIBLE);
        ((TextView) view.findViewById(R.id.character_sync_status_textView)).setText(
                status == SyncRules.Status.SYNCED ? R.string.sync_linked_synced : R.string.sync_linked_player_left);
    }

    /** Back from the condition wizard, the same tab is open again. */
    @Override
    public void onDestroyView() {
        if (sheet != null) tab = sheet.getSelectedTab();
        sheet = null;
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, sheet != null ? sheet.getSelectedTab() : tab);
    }
}
