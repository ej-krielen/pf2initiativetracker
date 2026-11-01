package com.rekijan.initiativetrackersecondedition.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.rekijan.initiativetrackersecondedition.BuildConfig;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.data.EncounterStore;
import com.rekijan.initiativetrackersecondedition.core.util.DialogHost;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterDefaults;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterSheet;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterDetailFragment;
import com.rekijan.initiativetrackersecondedition.feature.condition.ConditionWizardFragment;
import com.rekijan.initiativetrackersecondedition.feature.encounter.CharacterDialogs;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;
import com.rekijan.initiativetrackersecondedition.feature.encounter.EncounterFragment;
import com.rekijan.initiativetrackersecondedition.feature.encounter.TurnDialogs;
import com.rekijan.initiativetrackersecondedition.feature.info.InfoDialogs;
import com.rekijan.initiativetrackersecondedition.feature.order.EditOrderFragment;
import com.rekijan.initiativetrackersecondedition.feature.party.PartyBook;
import com.rekijan.initiativetrackersecondedition.feature.party.PartyDialogs;
import com.rekijan.initiativetrackersecondedition.feature.party.PartySlots;
import com.rekijan.initiativetrackersecondedition.feature.player.PlayerFragment;
import com.rekijan.initiativetrackersecondedition.feature.profile.ProfilesFragment;
import com.rekijan.initiativetrackersecondedition.feature.reaction.ReactionsFragment;
import com.rekijan.initiativetrackersecondedition.feature.spell.SpellDialogs;
import com.rekijan.initiativetrackersecondedition.feature.sync.ApprovalDialogs;
import com.rekijan.initiativetrackersecondedition.feature.sync.SpellRequests;
import com.rekijan.initiativetrackersecondedition.feature.sync.SyncDiff;
import com.rekijan.initiativetrackersecondedition.feature.sync.SyncFragment;
import com.rekijan.initiativetrackersecondedition.feature.sync.SyncManager;
import com.rekijan.initiativetrackersecondedition.monetization.BillingManager;

import nl.rekijan.ads.AdConsentManager;
import nl.rekijan.ads.RekijanBannerAdView;

import java.util.List;

/**
 * The only activity. Opens the profile that was used last: a party's
 * encounter (the GM) or a player character ({@link PlayerFragment}). Back from there, or Switch
 * profile in the drawer, goes to the profile list ({@link ProfilesFragment}); back on the list
 * closes the app.
 * <p>
 * For the GM in portrait, on phones and tablets, the encounter list is swapped for the character
 * details, reactions, condition wizard, Edit order or Table sync. In landscape (layout-land) the
 * details, reactions, condition wizard and Edit order sit next to the list; Next turn shows the
 * details of whoever's turn it is. When a rotation changes the number of panes, the open screen
 * moves to the other pane with its state (see {@link #setUpPanes}), and open dialogs come back
 * through {@link DialogHost}. The toolbar
 * holds PCs to max HP and Sort as icons and the other encounter actions in the overflow; Next turn
 * is in the encounter's turn bar. The navigation drawer ({@link NavDrawer}) holds Switch profile, Add
 * character, Delete characters, Edit order, Table sync and the app items. It also runs the ad
 * consent flow and the ad bar, billing (tips, Remove Ads, party packs), shows the GM's Table sync
 * requests ({@link ApprovalDialogs}) one at a time, and saves the fight when the app goes to the
 * background. Android 12+ shows the system splash from the launch theme.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 17-9-2015
 */
public class MainActivity extends AppCompatActivity implements NavDrawer.Listener, ProfilesFragment.Host {

    /** Menu actions that only make sense while the encounter list is on screen. */
    private static final int[] ENCOUNTER_ACTIONS = {R.id.action_settings_set_pcs_max_hp, R.id.action_settings_sort,
            R.id.action_settings_reset_round, R.id.action_settings_hide_reactions,
            R.id.action_settings_start_turn_info, R.id.action_settings_end_turn_info};
    private static final String LOG_TAG = "MainActivity";
    private static final String STATE_TWO_PANE = "two_pane";
    private static final String STATE_DETAILS_CHOSEN = "details_chosen";
    /** The argument key under which the details, reactions and condition wizard keep their character's position. */
    private static final String ARG_POSITION = "POSITION";
    /** Back stack name of the screens opened over the details in the second pane. */
    private static final String BACK_STACK_SECOND_PANE = "second_pane";

    private CombatTrackerApp app;
    private Encounter encounter;
    private EncounterStore store;
    private BillingManager billingManager;
    private RekijanBannerAdView adBar;
    private AdConsentManager adConsentManager;
    private MaterialToolbar toolbar;
    private AlertDialog requestDialog;
    private DrawerLayout drawerLayout;
    private NavDrawer navDrawer;
    private OnBackPressedCallback profileRootBack;
    /** True in landscape (layout-land): the details sit next to the list. */
    private boolean isTwoPane;
    /**
     * True when the user opened the character in the details pane. Characters the second pane
     * shows by itself (the first one, or whoever's turn it is) don't open in one pane after a rotation.
     */
    private boolean detailsChosen;

    /**
     * Reacts to the consent outcome, at startup and after the user changed their choice in the
     * privacy settings form. Updates the drawer so "Privacy Settings" appears when required.
     */
    private final AdConsentManager.Callback adCallback = new AdConsentManager.Callback() {
        @Override
        public void onReady() {
            if (isFinishing() || isDestroyed()) return;
            if (!store.isAdsRemoved()) { // may have been bought while consent was loading
                adBar.load(BuildConfig.ADMOB_BANNER_ID);
            }
            refreshDrawer();
        }

        @Override
        public void onFailure(@NonNull String reason) {
            if (isFinishing() || isDestroyed()) return;
            Log.d(LOG_TAG, "No ads: " + reason);
            hideAds();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        app = (CombatTrackerApp) getApplication();
        encounter = app.getEncounter();
        store = app.getStore();
        isTwoPane = findViewById(R.id.second_fragment_container) != null;
        if (savedInstanceState != null) detailsChosen = savedInstanceState.getBoolean(STATE_DETAILS_CHOSEN);

        // Back callbacks: the one added last wins, so the drawer (close first) comes after this one
        setUpProfileRootBack();
        setUpDrawer();
        applySystemBarInsets();

        setUpPanes(savedInstanceState);
        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(this::onScreenChanged);
        fm.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                                              @NonNull View v, @Nullable Bundle savedInstanceState) {
                // Root screens are swapped without the back stack, so follow the views instead
                if (f.getId() == R.id.main_fragment_container) onScreenChanged();
            }
        }, false);

        adBar = findViewById(R.id.bottomAdBar);
        if (!store.isAdsRemoved()) {
            adConsentManager = new AdConsentManager(this);
            adConsentManager.requestConsentThenInit(adCallback);
        }

        billingManager = new BillingManager(getApplication());
        billingManager.setRemoveAdsListener(new BillingManager.RemoveAdsListener() {
            @Override
            public void onAdsRemoved() {
                hideAds();
            }

            @Override
            public void onPriceLoaded() {
                refreshDrawer();
                refreshProfiles();
            }
        });
        billingManager.setPartyPackListener((level, fresh) -> {
            if (fresh) {
                Toast.makeText(this, getString(R.string.billing_party_pack_thanks,
                        app.getParties().getLimit()), Toast.LENGTH_LONG).show();
            }
            refreshProfiles();
        });
        billingManager.start();
        onScreenChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DialogHost.attach(this);
        adBar.resume();
        app.getSync().setRequestListener(this::showNextRequest);
        // The lifecycle only counts as resumed once onResume has returned, and showNextRequest checks that
        getWindow().getDecorView().post(this::showNextRequest);
    }

    @Override
    protected void onPause() {
        app.getSync().setRequestListener(null);
        app.saveEncounter();
        adBar.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // A Table sync request stays in the queue, so it is asked again on the next screen
        if (requestDialog != null) {
            requestDialog.setOnDismissListener(null);
            requestDialog.dismiss();
        }
        DialogHost.detach(this);
        billingManager.endConnection();
        adBar.destroyCurrentAd();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_TWO_PANE, isTwoPane);
        outState.putBoolean(STATE_DETAILS_CHOSEN, detailsChosen);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean showingEncounter = mainPane() instanceof EncounterFragment;
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
            closeDetailsInOnePane();
            TurnDialogs.askRoundReset(this, encounter, encounter.sortByInitiative());
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
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    // ---------------------------------------------------------------- Profiles

    @Override
    public void openParty(String id) {
        app.openParty(id);
        showRoot(EncounterFragment.newInstance());
    }

    @Override
    public void openPlayer(String id) {
        app.openPlayer(id);
        showRoot(PlayerFragment.newInstance(id));
    }

    @Override
    public void showPartyLimit() {
        PartyBook parties = app.getParties();
        int level = PartySlots.nextPackLevel(parties.getPackLevel());
        String price = level > 0 ? billingManager.getPartyPackPrice(level) : null;
        PartyDialogs.showLimit(this, parties.getUsed(), parties.getLimit(), price,
                () -> billingManager.purchasePartyPack(this, level));
    }

    @Override
    public void offerMoreParties() {
        PartyBook parties = app.getParties();
        int level = PartySlots.nextPackLevel(parties.getPackLevel());
        String price = level > 0 ? billingManager.getPartyPackPrice(level) : null;
        if (price == null) {
            Toast.makeText(this, R.string.billing_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }
        PartyDialogs.offerPack(this, parties.getUsed(), parties.getLimit(), price,
                () -> billingManager.purchasePartyPack(this, level));
    }

    @Nullable
    @Override
    public String nextPackPrice() {
        int level = PartySlots.nextPackLevel(app.getParties().getPackLevel());
        return level > 0 && billingManager != null ? billingManager.getPartyPackPrice(level) : null;
    }

    /** The player screen saved an edit: the name in the toolbar may have changed. */
    public void onPlayerChanged() {
        updateToolbar();
    }

    // ---------------------------------------------------------------- Drawer

    @Override
    public void onSwitchProfile() {
        if (mainPane() instanceof ProfilesFragment) return;
        showRoot(ProfilesFragment.newInstance());
    }

    @Override
    public void onAddCharacter() {
        closeDetailsInOnePane();
        encounter.add(CharacterDefaults.newCharacter(this));
    }

    @Override
    public void onDeleteCharacters() {
        closeDetailsInOnePane();
        CharacterDialogs.askDeleteCharacters(this, encounter, this::showFirstCharacterInTwoPanes);
    }

    @Override
    public void onEditOrder() {
        if (mainPane() instanceof EditOrderFragment
                || getSupportFragmentManager().findFragmentById(R.id.second_fragment_container) instanceof EditOrderFragment) {
            return;
        }
        closeDetailsInOnePane();
        showEditOrder();
    }

    @Override
    public void onTableSync() {
        if (mainPane() instanceof SyncFragment) return;
        closeDetailsInOnePane();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, SyncFragment.newInstance())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onRemoveAds() {
        billingManager.purchaseRemoveAds(this);
    }

    @Override
    public void onTip() {
        billingManager.openTipDialog(this);
    }

    @Override
    public void onAbout() {
        InfoDialogs.about(this);
    }

    @Override
    public void onPrivacy() {
        InfoDialogs.privacyPolicy(this);
    }

    @Override
    public void onPrivacySettings() {
        if (adConsentManager != null) adConsentManager.showPrivacyOptionsForm(adCallback);
    }

    // ---------------------------------------------------------------- Panes

    /**
     * Shows a character's details, which the user chose: next to the list in landscape, instead of
     * it in portrait. A spell card has no details.
     */
    public void showCharacterDetail(int position) {
        showCharacterDetail(position, CharacterSheet.TAB_STATS);
    }

    /** Shows a character's details opened on a tab, e.g. the conditions from the card's conditions line. */
    public void showCharacterDetail(int position, int tab) {
        if (encounter.isValidPosition(position) && encounter.get(position).isSpellCard()) return;
        detailsChosen = true;
        if (isTwoPane) {
            showInSecondPane(position, tab);
        } else {
            showOnTop(CharacterDetailFragment.newInstance(position, tab));
        }
    }

    public void showReactions(int position) {
        showOnTop(ReactionsFragment.newInstance(position));
    }

    public void showConditionWizard(int position) {
        showOnTop(ConditionWizardFragment.newInstance(position));
    }

    /** The condition wizard for a player character, over the player screen (also in landscape). */
    public void showPlayerConditionWizard(String playerId) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, ConditionWizardFragment.newForPlayer(playerId))
                .addToBackStack(null)
                .commit();
    }

    public void showEditOrder() {
        showOnTop(EditOrderFragment.newInstance());
    }

    /**
     * Opens a screen with a way back: over the details in landscape, over the list in portrait.
     * In landscape it takes the place of a screen opened there before, so back goes to the details.
     */
    private void showOnTop(Fragment fragment) {
        if (isTwoPane) popSecondPaneScreens();
        getSupportFragmentManager().beginTransaction()
                .replace(isTwoPane ? R.id.second_fragment_container : R.id.main_fragment_container, fragment)
                .addToBackStack(isTwoPane ? BACK_STACK_SECOND_PANE : null)
                .commit();
    }

    /** Landscape: the details of a character in the second pane, in place of what was open there. */
    private void showInSecondPane(int position, int tab) {
        popSecondPaneScreens();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.second_fragment_container, CharacterDetailFragment.newInstance(position, tab))
                .commit();
    }

    /** Landscape: closes the reactions, condition wizard or Edit order opened over the details. */
    private void popSecondPaneScreens() {
        FragmentManager fm = getSupportFragmentManager();
        while (fm.getBackStackEntryCount() > 0
                && BACK_STACK_SECOND_PANE.equals(fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName())) {
            fm.popBackStackImmediate();
        }
    }

    /**
     * First start: the profile used last. After a rotation the screens come back by themselves when
     * the number of panes stayed the same. Otherwise the screen that was open on the GM's side
     * (details, reactions, condition wizard or Edit order) moves between the second pane and the
     * main pane, keeping its own state (tab, scroll position, typed text), and Table sync stays open.
     */
    private void setUpPanes(@Nullable Bundle savedInstanceState) {
        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            String playerId = app.getOpenPlayerId();
            fm.beginTransaction()
                    .add(R.id.main_fragment_container, playerId != null
                            ? PlayerFragment.newInstance(playerId) : EncounterFragment.newInstance())
                    .commitNow();
            if (playerId == null) showFirstCharacterInTwoPanes();
            return;
        }
        boolean wasTwoPane = savedInstanceState.getBoolean(STATE_TWO_PANE);
        if (wasTwoPane == isTwoPane) return;

        Fragment main = mainPane();
        Fragment second = fm.findFragmentById(R.id.second_fragment_container);
        if (isTwoPane) {
            // To landscape: a screen over the list moves next to it, with the details under it
            if (!isSecondPaneScreen(main)) {
                // The list, Table sync or a player screen stays where it is
                if (second == null && isGmScreen(main)) showFirstCharacterInTwoPanes();
                return;
            }
            Fragment open = copyOf(main);
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            if (open instanceof CharacterDetailFragment) {
                fm.beginTransaction().replace(R.id.second_fragment_container, open).commitNow();
            } else {
                detailsChosen = false;
                Bundle args = open.getArguments();
                int position = args != null ? args.getInt(ARG_POSITION, 0) : 0;
                if (encounter.isValidPosition(position) && !encounter.get(position).isSpellCard()) {
                    fm.beginTransaction().replace(R.id.second_fragment_container,
                            CharacterDetailFragment.newInstance(position, CharacterSheet.TAB_STATS)).commitNow();
                }
                fm.beginTransaction().replace(R.id.second_fragment_container, open)
                        .addToBackStack(BACK_STACK_SECOND_PANE).commit();
            }
        } else {
            // To portrait: the second pane is gone. What the user opened there goes over the list.
            boolean keep = second != null && (!(second instanceof CharacterDetailFragment) || detailsChosen);
            Fragment open = keep ? copyOf(second) : null;
            Fragment sync = main instanceof SyncFragment ? copyOf(main) : null;
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            FragmentTransaction removeSecond = fm.beginTransaction();
            for (Fragment f : fm.getFragments()) {
                if (f.getId() == R.id.second_fragment_container) removeSecond.remove(f);
            }
            removeSecond.commitNow();
            Fragment top = sync != null ? sync : open;
            if (top != null) {
                fm.beginTransaction().replace(R.id.main_fragment_container, top).addToBackStack(null).commit();
            }
        }
    }

    /** @return A new instance of the screen with the same arguments and its saved state, to show in the other pane. */
    private Fragment copyOf(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment copy = fm.getFragmentFactory().instantiate(getClassLoader(), fragment.getClass().getName());
        copy.setArguments(fragment.getArguments());
        copy.setInitialSavedState(fm.saveFragmentInstanceState(fragment));
        return copy;
    }

    /** @return True for the GM's screens that sit next to the list in landscape. */
    private static boolean isSecondPaneScreen(@Nullable Fragment f) {
        return f instanceof CharacterDetailFragment || f instanceof ReactionsFragment || f instanceof EditOrderFragment
                || (f instanceof ConditionWizardFragment && !((ConditionWizardFragment) f).isForPlayer());
    }

    /**
     * Puts a root screen (encounter, player or profile list) in the main pane: the back stack and
     * the details pane are cleared, and in landscape the encounter gets its first character's details.
     */
    private void showRoot(Fragment root) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        fm.beginTransaction().replace(R.id.main_fragment_container, root).commitNow();
        Fragment detailPane = fm.findFragmentById(R.id.second_fragment_container);
        if (detailPane != null) fm.beginTransaction().remove(detailPane).commitNow();
        if (root instanceof EncounterFragment) showFirstCharacterInTwoPanes();
        onScreenChanged();
    }

    /** In landscape: the details of the first character, or nothing when the party is empty. */
    private void showFirstCharacterInTwoPanes() {
        if (!isTwoPane) return;
        detailsChosen = false;
        if (encounter.size() > 0 && !encounter.get(0).isSpellCard()) {
            showInSecondPane(0, CharacterSheet.TAB_STATS);
        } else {
            popSecondPaneScreens();
            Fragment detailPane = getSupportFragmentManager().findFragmentById(R.id.second_fragment_container);
            if (detailPane != null) getSupportFragmentManager().beginTransaction().remove(detailPane).commit();
        }
    }

    /** Next turn, from the encounter's turn bar. */
    public void nextTurn() {
        Encounter.Turn turn = encounter.nextTurn();
        if (turn == null) return;
        TurnDialogs.onTurnChanged(this, encounter, turn);
        // In landscape the details follow whoever's turn it is
        showFirstCharacterInTwoPanes();
    }

    /** Encounter actions go back to the list first when there is one pane. */
    private void closeDetailsInOnePane() {
        FragmentManager fm = getSupportFragmentManager();
        if (!isTwoPane && fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
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

    @Nullable
    private Fragment mainPane() {
        return getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
    }

    /** @return True for the GM's screens: the encounter and what opens from it. */
    private static boolean isGmScreen(@Nullable Fragment f) {
        return f != null && !(f instanceof PlayerFragment) && !(f instanceof ProfilesFragment)
                && !(f instanceof ConditionWizardFragment && ((ConditionWizardFragment) f).isForPlayer());
    }

    // ---------------------------------------------------------------- Drawer, toolbar, back, ads

    private void setUpDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        navDrawer = new NavDrawer(navView.getHeaderView(0), this, drawerLayout::closeDrawers);

        // Back closes the drawer first
        OnBackPressedCallback closeDrawerOnBack = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                drawerLayout.closeDrawers();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, closeDrawerOnBack);
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerStateChanged(int newState) {
                // Fresh round and name before it slides in, also when swiped open
                if (newState != DrawerLayout.STATE_IDLE && !drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    refreshDrawer();
                }
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                closeDrawerOnBack.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                closeDrawerOnBack.setEnabled(false);
            }
        });
    }

    /** Back on a profile's main screen (encounter or character) goes to the profile list. */
    private void setUpProfileRootBack() {
        profileRootBack = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                showRoot(ProfilesFragment.newInstance());
            }
        };
        getOnBackPressedDispatcher().addCallback(this, profileRootBack);
    }

    /** Fills the drawer for the screen that is open. */
    private void refreshDrawer() {
        if (navDrawer == null) return;
        boolean adsRemoved = store.isAdsRemoved();
        String removeAdsPrice = null;
        if (!adsRemoved) {
            String price = billingManager != null ? billingManager.getRemoveAdsPrice() : null;
            removeAdsPrice = price != null ? price : "";
        }
        // Only when Google's consent SDK says users must be able to change their choice (usually EEA/UK)
        boolean showPrivacySettings = !adsRemoved
                && adConsentManager != null && adConsentManager.isPrivacyOptionsRequired();
        Fragment main = mainPane();
        NavDrawer.Mode mode;
        String line;
        if (main instanceof ProfilesFragment) {
            mode = NavDrawer.Mode.PROFILES;
            line = null;
        } else if (!isGmScreen(main) && app.getOpenPlayerId() != null) {
            mode = NavDrawer.Mode.PLAYER;
            line = getString(R.string.nav_player_line, playerName(app.getOpenPlayerId()));
        } else {
            mode = NavDrawer.Mode.GM;
            line = getString(R.string.nav_active_party, app.getParties().getActive().getName(), encounter.getRound());
        }
        navDrawer.refresh(app, new NavDrawer.State(mode, line, removeAdsPrice, showPrivacySettings));
    }

    private void refreshProfiles() {
        Fragment main = mainPane();
        if (main instanceof ProfilesFragment) ((ProfilesFragment) main).refresh();
    }

    private String playerName(String id) {
        String name = app.getPlayers().getName(id);
        return name.trim().isEmpty() ? getString(R.string.empty_character_name) : name;
    }

    /** No ads (consent failed, or Remove Ads bought): the bar goes away and the drawer updates. */
    private void hideAds() {
        adBar.destroyCurrentAd();
        adBar.setVisibility(View.GONE);
        refreshDrawer();
    }

    /** The subtitle under the wordmark: the open party, the player character, or nothing. */
    private void updateToolbar() {
        Fragment main = mainPane();
        if (main instanceof ProfilesFragment) {
            toolbar.setSubtitle(null);
        } else if (!isGmScreen(main) && app.getOpenPlayerId() != null) {
            toolbar.setSubtitle(getString(R.string.nav_player_line, playerName(app.getOpenPlayerId())));
        } else {
            toolbar.setSubtitle(app.getParties().getActive().getName());
        }
    }

    /**
     * Another screen is open: the toolbar subtitle, the menu or back arrow, which back goes to the
     * profile list, the second pane (only for the GM) and the drawer follow.
     */
    private void onScreenChanged() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment main = mainPane();
        boolean canGoBack = fm.getBackStackEntryCount() > 0;
        updateToolbar();

        if (canGoBack) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            toolbar.setNavigationContentDescription(R.string.nav_back);
            toolbar.setNavigationOnClickListener(v -> fm.popBackStack());
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_menu);
            toolbar.setNavigationContentDescription(R.string.nav_drawer_open);
            toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }
        // The drawer only swipes open on a root screen, so a swipe on the details doesn't open it
        drawerLayout.setDrawerLockMode(canGoBack
                ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED);
        profileRootBack.setEnabled(!canGoBack && !(main instanceof ProfilesFragment));

        if (isTwoPane) {
            int visibility = isGmScreen(main) ? View.VISIBLE : View.GONE;
            findViewById(R.id.second_fragment_container).setVisibility(visibility);
            findViewById(R.id.pane_divider).setVisibility(visibility);
        }
        invalidateOptionsMenu();
        refreshDrawer();
        showNextRequest();
    }

    /**
     * Shows the next Table sync request for the GM, one at a time and only on the GM's screens
     * while this screen is in front. After an answer the next one in the queue follows.
     */
    private void showNextRequest() {
        if (requestDialog != null && requestDialog.isShowing()) return;
        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED) || !isGmScreen(mainPane())) return;
        SyncManager sync = app.getSync();
        SyncManager.Request request = sync.nextRequest();
        if (request == null) return;
        if (request.caster != null && (request.cast != null || request.end != null)) {
            requestDialog = showSpellRequest(sync, request);
            if (requestDialog == null) return;
            requestDialog.setOnDismissListener(d -> {
                requestDialog = null;
                showNextRequest();
            });
            return;
        }
        ApprovalDialogs.Listener answer = new ApprovalDialogs.Listener() {
            @Override
            public void onApprove(List<SyncDiff.Change> accepted) {
                sync.approve(request, accepted);
            }

            @Override
            public void onRefuse() {
                sync.refuse(request);
            }

            @Override
            public void onLink(CharacterModel pc) {
                // The request comes back as changes from that PC once this dialog is gone
                sync.link(request, pc);
            }
        };
        String name = request.sheet.name.isEmpty() ? getString(R.string.empty_character_name) : request.sheet.name;
        requestDialog = request.join
                ? ApprovalDialogs.showJoin(this, request, app.getParties().getActive().getName(), answer)
                : ApprovalDialogs.showChanges(this, name, request.changes, answer);
        requestDialog.setOnDismissListener(d -> {
            requestDialog = null;
            showNextRequest();
        });
    }

    /**
     * A player's spell request: the Cast spell dialog filled in with it, or End spell for one of
     * their spells. The cast or end happens in the dialog; the sync manager only hears the answer.
     *
     * @return The shown dialog, or null when it couldn't be shown.
     */
    @Nullable
    private AlertDialog showSpellRequest(SyncManager sync, SyncManager.Request request) {
        CharacterModel caster = request.caster;
        if (request.cast != null) {
            SpellRequests.Cast cast = request.cast;
            return SpellDialogs.showCastRequest(this, app.getEncounter(),
                    new SpellDialogs.Request(caster, cast.name, cast.rounds, cast.condition, cast.value, cast.targets),
                    done -> sync.answerCast(request, done));
        }
        String name = caster.getCharacterName().isEmpty() ? getString(R.string.empty_character_name)
                : caster.getCharacterName();
        return SpellDialogs.showEndRequest(this, app.getEncounter(), name, request.end.spellId,
                done -> sync.answerEnd(request, done));
    }

    /**
     * Edge-to-edge: pads the screen for the status bar, navigation bar and display cutout, and
     * for the keyboard when it is open. The drawer gets the same insets: its header below the
     * status bar, its bottom above the navigation bar. Done here for both, so it works the same
     * on every Android version.
     */
    private void applySystemBarInsets() {
        View root = findViewById(R.id.root);
        NavigationView navView = findViewById(R.id.nav_view);
        View header = navView.getHeaderView(0);
        int headerTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            root.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            navView.setPadding(bars.left, 0, 0, bars.bottom);
            header.setPadding(header.getPaddingLeft(), headerTop + bars.top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
