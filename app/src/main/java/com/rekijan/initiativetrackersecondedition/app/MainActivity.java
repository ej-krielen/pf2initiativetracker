package com.rekijan.initiativetrackersecondedition.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.data.EncounterStore;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterDefaults;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterDetailFragment;
import com.rekijan.initiativetrackersecondedition.feature.condition.ConditionWizardFragment;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;
import com.rekijan.initiativetrackersecondedition.feature.encounter.EncounterFragment;
import com.rekijan.initiativetrackersecondedition.feature.encounter.TurnDialogs;
import com.rekijan.initiativetrackersecondedition.feature.info.InfoDialogs;
import com.rekijan.initiativetrackersecondedition.feature.order.EditOrderFragment;
import com.rekijan.initiativetrackersecondedition.feature.reaction.ReactionsFragment;
import com.rekijan.initiativetrackersecondedition.monetization.BillingManager;

/**
 * The only activity. Hosts the encounter list and, on a phone, swaps it for the character
 * details, reactions, condition wizard or the Edit Order screen; on a tablet those sit next to
 * the list. It also handles the toolbar menu (PCs to max HP and Sort as icons, the rest in the
 * overflow; Next turn is in the encounter's turn bar) and saves the fight when the app goes to
 * the background. Android 12+ shows the system splash from the launch theme.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 17-9-2015
 */
public class MainActivity extends AppCompatActivity {

    /** Menu actions that only make sense while the encounter list is on screen. */
    private static final int[] ENCOUNTER_ACTIONS = {R.id.action_settings_set_pcs_max_hp, R.id.action_settings_sort,
            R.id.action_settings_add_character, R.id.action_settings_delete_character,
            R.id.action_settings_reset_round, R.id.action_settings_hide_reactions,
            R.id.action_settings_start_turn_info, R.id.action_settings_end_turn_info};

    private CombatTrackerApp app;
    private Encounter encounter;
    private EncounterStore store;
    private BillingManager billingManager;
    private boolean isTablet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        applySystemBarInsets();

        app = (CombatTrackerApp) getApplication();
        encounter = app.getEncounter();
        store = app.getStore();
        isTablet = getResources().getBoolean(R.bool.isTablet);

        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            fm.beginTransaction()
                    .add(R.id.main_fragment_container, EncounterFragment.newInstance())
                    .commit();
            if (isTablet && encounter.size() > 0) {
                fm.beginTransaction()
                        .add(R.id.second_fragment_container, CharacterDetailFragment.newInstance(0))
                        .commit();
            }
        }
        fm.addOnBackStackChangedListener(this::onBackStackChanged);
        onBackStackChanged();

        billingManager = new BillingManager(getApplication());
        billingManager.start();
    }

    @Override
    protected void onPause() {
        app.saveEncounter();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        billingManager.endConnection();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Right after start the encounter list is not attached yet, but it is what will show
        Fragment main = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
        boolean showingEncounter = main == null || main instanceof EncounterFragment;
        for (int id : ENCOUNTER_ACTIONS) {
            menu.findItem(id).setVisible(showingEncounter);
        }
        menu.findItem(R.id.action_settings_hide_reactions).setChecked(store.isHideReactions());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings_sort) {
            closeDetailsOnPhone();
            TurnDialogs.askRoundReset(this, encounter, encounter.sortByInitiative());
        } else if (id == R.id.action_settings_add_character) {
            closeDetailsOnPhone();
            encounter.add(CharacterDefaults.newCharacter(this));
        } else if (id == R.id.action_settings_delete_character) {
            InfoDialogs.howToDeleteCharacter(this);
        } else if (id == R.id.action_settings_set_pcs_max_hp) {
            showMaxHpResult(encounter.setPcsToMaxHp());
        } else if (id == R.id.action_settings_reset_round) {
            encounter.resetRound();
        } else if (id == R.id.action_settings_hide_reactions) {
            store.setHideReactions(!store.isHideReactions());
            invalidateOptionsMenu();
            encounter.notifyChanged();
        } else if (id == R.id.action_settings_start_turn_info) {
            InfoDialogs.startOfTurn(this);
        } else if (id == R.id.action_settings_end_turn_info) {
            InfoDialogs.endOfTurn(this);
        } else if (id == R.id.action_settings_about) {
            InfoDialogs.about(this);
        } else if (id == R.id.action_settings_privacy) {
            InfoDialogs.privacyPolicy(this);
        } else if (id == R.id.action_settings_tip) {
            billingManager.openTipDialog(this);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        getSupportFragmentManager().popBackStack();
        return true;
    }

    /** Shows a character's details: next to the list on a tablet, instead of it on a phone. */
    public void showCharacterDetail(int position) {
        if (isTablet) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.second_fragment_container, CharacterDetailFragment.newInstance(position))
                    .commit();
        } else {
            showOnTop(CharacterDetailFragment.newInstance(position));
        }
    }

    public void showReactions(int position) {
        showOnTop(ReactionsFragment.newInstance(position));
    }

    public void showConditionWizard(int position) {
        showOnTop(ConditionWizardFragment.newInstance(position));
    }

    public void showEditOrder() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, EditOrderFragment.newInstance())
                .addToBackStack(null)
                .commit();
    }

    /** Opens a screen with a way back: in the side panel on a tablet, over the list on a phone. */
    private void showOnTop(Fragment fragment) {
        int container = isTablet ? R.id.second_fragment_container : R.id.main_fragment_container;
        getSupportFragmentManager().beginTransaction()
                .replace(container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /** Next turn, from the encounter's turn bar. */
    public void nextTurn() {
        Encounter.Turn turn = encounter.nextTurn();
        if (turn == null) return;
        TurnDialogs.onTurnChanged(this, encounter, turn);
        // On a tablet the details follow whoever's turn it is
        if (isTablet) showCharacterDetail(0);
    }

    /** Encounter actions from the menu go back to the list first on a phone. */
    private void closeDetailsOnPhone() {
        FragmentManager fm = getSupportFragmentManager();
        if (!isTablet && fm.getBackStackEntryCount() > 0) {
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    private void showMaxHpResult(Encounter.MaxHpResult result) {
        if (!result.skipped.isEmpty()) {
            String message = getString(R.string.toast_max_hp, TextUtils.join("\n", result.skipped));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
        if (result.woundedReset) {
            Toast.makeText(this, R.string.toast_wounded_removed, Toast.LENGTH_SHORT).show();
        }
    }

    private void onBackStackChanged() {
        boolean canGoBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(canGoBack);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        }
        invalidateOptionsMenu();
    }

    /**
     * Edge-to-edge: pads the screen for the status bar, navigation bar and display cutout, and
     * for the keyboard when it is open. Done here, so it works the same on every Android version.
     */
    private void applySystemBarInsets() {
        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
