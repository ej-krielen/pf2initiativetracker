package com.rekijan.initiativetrackersecondedition.feature.sync;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Runs Table sync for the whole app, so it keeps working whichever screen is open. The GM is
 * the final arbiter of every player character.
 * <p>
 * GM side: starts a session tied to the active party and listens to the players' sends. A send
 * becomes a request ({@link #nextRequest}): a new player asks to join, a known one asks for the
 * changes in {@link SyncDiff}. The GM approves (some of) them or refuses; a send that changes
 * nothing is answered silently. Requests only show while the hosted party is the active one. A
 * few seconds after a synced character changes in that party, and right after every answer, the
 * GM's copy ({@link GmCopy}) is uploaded. Firestore drops a listener after an error; it is
 * attached again after a delay that doubles up to five minutes, and a session that turns out to
 * be gone is ended.
 * <p>
 * The GM may link a joining player to a PC they ran so far ({@link #link}), and remove or unlink
 * a synced PC ({@link #removeCharacters}, {@link #unlink}); the player then gets a notice. The
 * rules are in {@link SyncRules}.
 * <p>
 * Player side, per player character: joins a session by code and sends the whole sheet. While
 * its screen is open it follows the GM's copy live and merges it in ({@link SheetMerge}), so the
 * GM's answer and later changes arrive by themselves and unsent edits are kept.
 * <p>
 * Spells ({@link SpellRequests}): a player asks the GM to cast a spell or to end one of theirs
 * ({@link #requestCast}, {@link #requestEnd}); the request is added to their doc without a new
 * send. The GM gets it in the same queue, after the sheet requests, and answers it
 * ({@link #answerCast}, {@link #answerEnd}); the GM's copy carries the answer and the PC's spells.
 * <p>
 * Signing in (anonymously) only happens when the user starts or joins a session, so users who
 * never use Table sync never get a Firebase account. Firebase itself (App Check, Firestore) is
 * only set up then too, or at startup when a session is still running, so a GM who plays
 * standalone never talks to Firebase at all.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-10-2026
 */
public class SyncManager implements Encounter.Listener {

    private static final String TAG = "SyncManager";
    private static final long RETRY_MIN_MS = TimeUnit.SECONDS.toMillis(5);
    private static final long RETRY_MAX_MS = TimeUnit.MINUTES.toMillis(5);
    private static final long SIGN_IN_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(15);
    /** The GM's copies go up this long after the last change, so typing doesn't write every key. */
    private static final long UPLOAD_DELAY_MS = TimeUnit.SECONDS.toMillis(3);
    /** How often, and how far apart, a player checks whether a session ended after its GM's copy went. */
    private static final int SESSION_END_CHECKS = 3;
    private static final long SESSION_END_CHECK_DELAY_MS = TimeUnit.SECONDS.toMillis(4);

    /** The app side Table sync needs: the active party, saving it, and the player characters. */
    public interface Host {
        String getActivePartyId();

        Encounter getEncounter();

        void saveEncounter();

        CharacterModel loadPlayer(String id);

        void savePlayer(String id, CharacterModel character);
    }

    /** Told when anything Table sync shows has changed: state, players, a merge. */
    public interface StateListener {
        void onSyncStateChanged();
    }

    /** Told when there may be a request for the GM to answer. */
    public interface RequestListener {
        void onRequestsChanged();
    }

    /** Told when the GM's version was merged into a player character. */
    public interface PlayerUpdateListener {
        /**
         * @param characterId The player character that changed (saved already).
         * @param answer      What the GM decided about the last send, or {@link Answer#NONE}.
         * @param kept        With {@link Answer#KEPT}: how many sent values the GM kept as they were.
         */
        void onPlayerUpdated(String characterId, Answer answer, int kept);

        /**
         * The GM answered a spell request of this player character.
         *
         * @param cast True for a cast request, false for an end request.
         * @param done True when the GM cast (or ended) it, false when they rejected it (or kept it going).
         */
        void onSpellAnswered(String characterId, String spellName, boolean cast, boolean done);
    }

    /** The GM's answer to a player's send, as the player sees it. */
    public enum Answer {
        /** No answer in this update, only a change by the GM. */
        NONE,
        /** Everything sent was accepted. */
        ACCEPTED,
        /** Some sent values are the GM's again. */
        KEPT,
        /** The GM didn't let the character join; it left the session. */
        REJECTED,
        /** The GM removed the character from the party; it left the session. */
        REMOVED,
        /** The GM runs the character from now on; it left the session. */
        UNLINKED,
        /** The GM ended the session (or it expired); the character left it. */
        SESSION_ENDED
    }

    /**
     * A player's send waiting for the GM: a join, sheet changes, or a spell request
     * ({@link #cast} or {@link #end} set, with the PC in {@link #caster}).
     */
    public static final class Request {
        public final String docId;
        public final PlayerSheet sheet;
        /** True when the character isn't in the party: the player asks to join. */
        public final boolean join;
        /** For a known character: the changes from the GM's version to the send. */
        public final List<SyncDiff.Change> changes;
        /** For a join: the PCs the player can be linked to ({@link SyncRules#linkCandidates}). */
        public final List<CharacterModel> linkCandidates;
        /** For a join: the GM removed this player's character from this party before. */
        public final boolean removedBefore;
        /** A spell the player asks the GM to cast, or null. */
        @Nullable
        public final SpellRequests.Cast cast;
        /** A spell the player asks the GM to end, or null. */
        @Nullable
        public final SpellRequests.End end;
        /** For a spell request: the player's PC in the party. */
        @Nullable
        public final CharacterModel caster;

        Request(String docId, PlayerSheet sheet, boolean join, List<SyncDiff.Change> changes,
                List<CharacterModel> linkCandidates, boolean removedBefore) {
            this(docId, sheet, join, changes, linkCandidates, removedBefore, null, null, null);
        }

        private Request(String docId, PlayerSheet sheet, boolean join, List<SyncDiff.Change> changes,
                        List<CharacterModel> linkCandidates, boolean removedBefore, SpellRequests.Cast cast,
                        SpellRequests.End end, CharacterModel caster) {
            this.docId = docId;
            this.sheet = sheet;
            this.join = join;
            this.changes = changes;
            this.linkCandidates = linkCandidates;
            this.removedBefore = removedBefore;
            this.cast = cast;
            this.end = end;
            this.caster = caster;
        }

        static Request spell(String docId, PlayerSheet sheet, CharacterModel caster, SpellRequests.Cast cast,
                             SpellRequests.End end) {
            return new Request(docId, sheet, false, Collections.emptyList(), Collections.emptyList(), false,
                    cast, end, caster);
        }
    }

    /** Result of an action the user waits for. */
    public interface Callback {
        void onSuccess();

        /** The session code doesn't exist (any more). */
        void onNotFound();

        void onFailure(Exception e);
    }

    private final Host host;
    private final SyncRepository repository;
    private final SyncStore store;
    private final Runnable firebaseSetup;
    private boolean firebaseReady;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // Separate from mainHandler, whose retries are cancelled all at once
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Handler uploadHandler = new Handler(Looper.getMainLooper());
    private final Runnable upload = this::uploadGmCopies;
    private final List<StateListener> stateListeners = new ArrayList<>();
    private RequestListener requestListener;
    private PlayerUpdateListener playerUpdateListener;
    private ListenerRegistration playersRegistration;
    private Map<String, PlayerSheet> lastSheets = new LinkedHashMap<>();
    /** True once the players' listener answered in this run, so {@link #lastSheets} says who is there. */
    private boolean playersLoaded;
    /** Per document id, the latest send the GM hasn't answered yet. */
    private final Map<String, PlayerSheet> requests = new LinkedHashMap<>();
    /** Per document id, the cast request the GM hasn't answered yet. */
    private final Map<String, SpellRequests.Cast> castRequests = new LinkedHashMap<>();
    /** Per document id, the end spell request the GM hasn't answered yet. */
    private final Map<String, SpellRequests.End> endRequests = new LinkedHashMap<>();
    /** Per document id, the GM's copy last uploaded in this run, to skip unchanged ones. */
    private final Map<String, Map<String, Object>> uploaded = new HashMap<>();
    /** Per player character id, the live listener on its GM's copy. */
    private final Map<String, ListenerRegistration> playerWatches = new HashMap<>();
    private long retryDelayMs = RETRY_MIN_MS;
    /**
     * The hosted session's expiry, which every write to it must carry; read from the server only
     * when not known yet, or after a write failed. Null when not known.
     */
    private Timestamp hostExpiry;
    /** Per player character, the expiry of the session it joined, used the same way. */
    private final Map<String, Timestamp> playerExpiries = new HashMap<>();

    /**
     * @param firebaseSetup Sets up Firebase (App Check); run once, right before the first Firebase call.
     */
    public SyncManager(Host host, SyncRepository repository, SyncStore store, Runnable firebaseSetup) {
        this.host = host;
        this.repository = repository;
        this.store = store;
        this.firebaseSetup = firebaseSetup;
    }

    public void addStateListener(StateListener listener) {
        if (!stateListeners.contains(listener)) stateListeners.add(listener);
    }

    public void removeStateListener(StateListener listener) {
        stateListeners.remove(listener);
    }

    public void setRequestListener(@Nullable RequestListener listener) {
        requestListener = listener;
    }

    public void setPlayerUpdateListener(@Nullable PlayerUpdateListener listener) {
        playerUpdateListener = listener;
    }

    // ---------------------------------------------------------------- GM

    @Nullable
    public String getHostCode() {
        return store.getHostCode();
    }

    @Nullable
    public String getHostPartyId() {
        return store.getHostPartyId();
    }

    /** @return The latest sheets per document id, in the order Firestore gave them. Read-only. */
    public Map<String, PlayerSheet> getPlayers() {
        return Collections.unmodifiableMap(lastSheets);
    }

    /** Picks up a hosted session after an app start; ends it when the server says it is gone. */
    public void resumeOnStartup() {
        String code = store.getHostCode();
        if (code == null) return;
        ensureFirebase();
        startWatchingParty();
        attachListener(code);
        verify(code, false);
    }

    /** Starts hosting a new session for the active party. */
    public void startHosting(@NonNull Callback callback) {
        signIn(new SignInCallback() {
            @Override
            public void onSignedIn(String uid) {
                repository.createSession(uid, new SyncRepository.CreateCallback() {
                    @Override
                    public void onCreated(String code, Timestamp expiresAt) {
                        store.startHosting(code, host.getActivePartyId());
                        hostExpiry = expiresAt;
                        lastSheets = new LinkedHashMap<>();
                        playersLoaded = false;
                        requests.clear();
                        castRequests.clear();
                        endRequests.clear();
                        uploaded.clear();
                        startWatchingParty();
                        attachListener(code);
                        notifyStateChanged();
                        callback.onSuccess();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Stops hosting on this device right away, then deletes the session online so the code stops
     * working and the players' sheets are gone. Characters already in the party stay.
     *
     * @param callback Told whether the online delete worked; it expires anyway when it didn't.
     */
    public void stopHosting(@NonNull SyncRepository.CompletionCallback callback) {
        String code = store.getHostCode();
        endHostingLocally();
        if (code == null) {
            callback.onSuccess();
            return;
        }
        repository.deleteSession(code, callback);
    }

    /** The active party changed: requests and uploads pick up again when it is the hosted party. */
    public void onActivePartyChanged() {
        scheduleUpload();
        notifyRequests();
    }

    /** A party was deleted: when it was the hosted one, the session ends. */
    public void onPartyDeleted(String partyId) {
        if (partyId.equals(store.getHostPartyId())) {
            stopHosting(new SyncRepository.CompletionCallback() {
                @Override
                public void onSuccess() {
                    // Nothing to show: the user just deleted the party
                }

                @Override
                public void onFailure(Exception e) {
                    // Logged by the repository; the session expires on its own
                }
            });
        }
    }

    /** Something in the encounter changed: upload the GM's copies a little later. */
    @Override
    public void onEncounterChanged() {
        scheduleUpload();
    }

    /**
     * @return The next send for the GM to answer, or null when there is none or the hosted party
     * isn't the active one. Sends that change nothing are answered on the way.
     */
    @Nullable
    public Request nextRequest() {
        if (!hostedPartyIsActive()) return null;
        Request next = null;
        boolean answeredAny = false;
        for (Map.Entry<String, PlayerSheet> e : new ArrayList<>(requests.entrySet())) {
            String docId = e.getKey();
            PlayerSheet sheet = e.getValue();
            CharacterModel character = find(docId);
            if (character == null) {
                List<CharacterModel> candidates = SyncRules.linkCandidates(host.getEncounter().getCharacters(),
                        lastSheets.keySet(), docId, sheet.name);
                next = new Request(docId, sheet, true, Collections.emptyList(), candidates,
                        store.wasRemoved(docId, store.getHostPartyId()));
                break;
            }
            List<SyncDiff.Change> changes = SyncDiff.changes(PlayerSheet.of(character), sheet);
            if (!changes.isEmpty()) {
                next = new Request(docId, sheet, false, changes, Collections.emptyList(), false);
                break;
            }
            markAnswered(docId, sheet.sentAt);
            answeredAny = true;
        }
        // Spell requests come after the sheets, and only from PCs in the party
        if (next == null) {
            for (Map.Entry<String, SpellRequests.Cast> e : castRequests.entrySet()) {
                CharacterModel caster = find(e.getKey());
                PlayerSheet sheet = lastSheets.get(e.getKey());
                if (caster == null || sheet == null) continue;
                next = Request.spell(e.getKey(), sheet, caster, e.getValue(), null);
                break;
            }
        }
        if (next == null) {
            for (Map.Entry<String, SpellRequests.End> e : new ArrayList<>(endRequests.entrySet())) {
                CharacterModel caster = find(e.getKey());
                PlayerSheet sheet = lastSheets.get(e.getKey());
                if (caster == null || sheet == null) continue;
                if (!SpellRequests.contains(SpellRequests.spellsOf(host.getEncounter(), caster), e.getValue().spellId)) {
                    // Ended already (ran out, or the GM ended it): the player only needs to hear that
                    answerEnd(e.getKey(), e.getValue().at, true);
                    answeredAny = true;
                    continue;
                }
                next = Request.spell(e.getKey(), sheet, caster, null, e.getValue());
                break;
            }
        }
        // The player waits for an answer, also when nothing changed
        if (answeredAny) uploadNow();
        return next;
    }

    /**
     * The GM accepts a request: a new player is added as a PC, or the accepted changes are made.
     * Changes not in {@code accepted} keep the GM's value; the player gets that back.
     */
    public void approve(@NonNull Request request, @NonNull List<SyncDiff.Change> accepted) {
        if (!hostedPartyIsActive()) return;
        Encounter encounter = host.getEncounter();
        CharacterModel character = find(request.docId);
        if (request.join || character == null) {
            CharacterModel added = new CharacterModel();
            added.setSyncId(request.docId);
            request.sheet.applyTo(added);
            added.setPC(true);
            encounter.add(added);
            store.clearRemoved(request.docId);
        } else {
            SyncDiff.apply(PlayerSheet.of(character), request.sheet, accepted).applyTo(character);
            encounter.notifyChanged();
        }
        host.saveEncounter();
        markAnswered(request.docId, request.sheet.sentAt);
        uploadNow();
        notifyStateChanged();
        notifyRequests();
    }

    /**
     * The GM refuses a request: a new player isn't added (they are told), a known one gets the
     * GM's version back.
     */
    public void refuse(@NonNull Request request) {
        String code = store.getHostCode();
        if (code == null) return;
        markAnswered(request.docId, request.sheet.sentAt);
        if (request.join && find(request.docId) == null) {
            GmCopy rejection = GmCopy.rejection(request.sheet);
            // When this fails the player keeps waiting until they leave
            withHostExpiry(code, expiresAt -> repository.saveGmCopy(code, request.docId, rejection, expiresAt,
                    this::forgetHostExpiry));
        } else {
            uploadNow();
        }
        notifyRequests();
    }

    /**
     * The GM answered a cast request: cast (the spell is already on its targets) or rejected.
     * The player gets the answer with the GM's copy, and their spell list with it.
     */
    public void answerCast(@NonNull Request request, boolean cast) {
        if (request.cast == null) return;
        Map<String, SpellRequests.Answer> answers = store.loadCastAnswers();
        answers.put(request.docId, new SpellRequests.Answer(request.cast.at, cast));
        store.saveCastAnswers(answers);
        SpellRequests.Cast waiting = castRequests.get(request.docId);
        if (waiting != null && waiting.at <= request.cast.at) castRequests.remove(request.docId);
        if (cast) host.saveEncounter();
        uploadNow();
        notifyRequests();
    }

    /** The GM answered an end spell request: ended (the spell is already off its targets) or kept. */
    public void answerEnd(@NonNull Request request, boolean ended) {
        if (request.end == null) return;
        answerEnd(request.docId, request.end.at, ended);
        if (ended) host.saveEncounter();
        uploadNow();
        notifyRequests();
    }

    private void answerEnd(String docId, long at, boolean ended) {
        Map<String, SpellRequests.Answer> answers = store.loadEndAnswers();
        answers.put(docId, new SpellRequests.Answer(at, ended));
        store.saveEndAnswers(answers);
        SpellRequests.End waiting = endRequests.get(docId);
        if (waiting != null && waiting.at <= at) endRequests.remove(docId);
    }

    /**
     * The GM links a player asking to join to a PC they ran so far, instead of adding a new one.
     * The request stays open and comes back as changes from that PC to the send, so the GM picks
     * which values to keep.
     */
    public void link(@NonNull Request request, @NonNull CharacterModel character) {
        if (!hostedPartyIsActive() || !host.getEncounter().getCharacters().contains(character)) return;
        character.setSyncId(request.docId);
        character.setPC(true);
        store.clearRemoved(request.docId);
        host.getEncounter().notifyChanged();
        host.saveEncounter();
        notifyStateChanged();
        notifyRequests();
    }

    /** @return What this character's card shows about Table sync. */
    public SyncRules.Status getStatus(CharacterModel character) {
        return SyncRules.status(character.getSyncId(), hostedPartyIsActive(), playersLoaded, lastSheets.keySet());
    }

    /**
     * Removes characters from the active party. A synced player among them gets a notice and
     * leaves the session; a new request from them says the GM removed them before.
     */
    public void removeCharacters(@NonNull Collection<CharacterModel> characters) {
        if (hostedPartyIsActive()) {
            for (CharacterModel c : characters) {
                if (!SyncRules.isLinked(c.getSyncId())) continue;
                sendNotice(c, GmCopy.Ending.REMOVED);
                store.markRemoved(c.getSyncId(), store.getHostPartyId());
            }
        }
        host.getEncounter().removeAll(characters);
        notifyStateChanged();
    }

    /** The GM keeps this PC and runs it from now on: the player gets a notice and leaves the session. */
    public void unlink(@NonNull CharacterModel character) {
        if (!SyncRules.isLinked(character.getSyncId())) return;
        if (hostedPartyIsActive()) sendNotice(character, GmCopy.Ending.UNLINKED);
        character.setSyncId(null);
        host.getEncounter().notifyChanged();
        host.saveEncounter();
        notifyStateChanged();
    }

    // ---------------------------------------------------------------- Player

    /** @return The code of the session this player character joined, or null. */
    @Nullable
    public String getPlayerCode(String characterId) {
        return store.getPlayerCode(characterId);
    }

    /** @return When this character was last sent, in ms, or 0 when never. */
    public long getPlayerSentAt(String characterId) {
        return store.getPlayerSentAt(characterId);
    }

    /** @return When the GM's version last came in, in ms, or 0 when never (in this session). */
    public long getPlayerSyncedAt(String characterId) {
        return store.getPlayerSyncedAt(characterId);
    }

    /** @return True when the character sent something the GM hasn't answered yet. */
    public boolean isWaitingForGm(String characterId) {
        return store.getPlayerCode(characterId) != null && store.loadPlayerSent(characterId) != null;
    }

    /**
     * @return What the player changed since the GM's last version, or null when they never got
     * one (not added yet), in which case the whole sheet is still to be sent.
     */
    @Nullable
    public List<SyncDiff.Change> getUnsentChanges(String characterId, CharacterModel character) {
        PlayerSheet base = store.loadPlayerBase(characterId);
        return base == null ? null : SyncDiff.changes(base, PlayerSheet.of(character));
    }

    /**
     * Joins a session with this player character, after checking on the server that it exists.
     *
     * @param code A normalized, valid code (see {@link SessionCodes}).
     */
    public void join(@NonNull String characterId, @NonNull String code, @NonNull Callback callback) {
        signIn(new SignInCallback() {
            @Override
            public void onSignedIn(String uid) {
                repository.fetchExpiry(code, new SyncRepository.ExpiryCallback() {
                    @Override
                    public void onResult(@Nullable Timestamp expiresAt) {
                        if (expiresAt == null) {
                            callback.onNotFound();
                            return;
                        }
                        store.setPlayerCode(characterId, code);
                        playerExpiries.put(characterId, expiresAt);
                        notifyStateChanged();
                        callback.onSuccess();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Sends the whole sheet to the joined session, as a request for the GM. When the session is
     * gone, the character leaves it and the caller is told.
     */
    public void send(@NonNull String characterId, @NonNull CharacterModel character, @NonNull Callback callback) {
        String code = store.getPlayerCode(characterId);
        if (code == null) {
            callback.onNotFound();
            return;
        }
        PlayerSheet sheet = PlayerSheet.of(character);
        // Open spell requests stay on the player's doc, which a send writes as a whole
        sheet.cast = store.loadPlayerCast(characterId);
        sheet.end = store.loadPlayerEnd(characterId);
        signIn(new SignInCallback() {
            @Override
            public void onSignedIn(String uid) {
                Timestamp known = playerExpiries.get(characterId);
                if (known != null) {
                    sendSheet(code, uid, characterId, sheet, known, true, callback);
                } else {
                    fetchAndSend(code, uid, characterId, sheet, callback);
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /** Reads the session's expiry from the server, then sends; a session that is gone is left. */
    private void fetchAndSend(String code, String uid, String characterId, PlayerSheet sheet, Callback callback) {
        repository.fetchExpiry(code, new SyncRepository.ExpiryCallback() {
            @Override
            public void onResult(@Nullable Timestamp expiresAt) {
                if (expiresAt == null) {
                    leaveGoneSession(characterId);
                    callback.onNotFound();
                    return;
                }
                playerExpiries.put(characterId, expiresAt);
                sendSheet(code, uid, characterId, sheet, expiresAt, false, callback);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Writes the sheet with this expiry. The rules refuse it when the expiry is no longer the
     * session's (the GM renewed it, or the session is gone); with a remembered expiry it is then
     * read again and sent once more.
     *
     * @param remembered True when the expiry was remembered, not just read from the server.
     */
    private void sendSheet(String code, String uid, String characterId, PlayerSheet sheet, Timestamp expiresAt,
                           boolean remembered, Callback callback) {
        sheet.sentAt = store.nextSentAt(characterId, System.currentTimeMillis());
        String docId = SyncIds.playerDocId(uid, characterId);
        repository.savePlayer(code, docId, sheet, expiresAt, new SyncRepository.CompletionCallback() {
            @Override
            public void onSuccess() {
                store.setPlayerSentAt(characterId, sheet.sentAt);
                store.savePlayerSent(characterId, sheet);
                notifyStateChanged();
                callback.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                if (remembered && isDenied(e)) {
                    playerExpiries.remove(characterId);
                    fetchAndSend(code, uid, characterId, sheet, callback);
                } else {
                    callback.onFailure(e);
                }
            }
        });
    }

    /** @return The cast request waiting for the GM, or null. */
    @Nullable
    public SpellRequests.Cast getPendingCast(String characterId) {
        return store.getPlayerCode(characterId) != null ? store.loadPlayerCast(characterId) : null;
    }

    /** @return The end spell request waiting for the GM, or null. */
    @Nullable
    public SpellRequests.End getPendingEnd(String characterId) {
        return store.getPlayerCode(characterId) != null ? store.loadPlayerEnd(characterId) : null;
    }

    /** @return The spells this character has going, as the GM last said; empty outside a session. */
    public List<SpellRequests.Spell> getPlayerSpells(String characterId) {
        return store.getPlayerCode(characterId) != null ? store.loadPlayerSpells(characterId) : new ArrayList<>();
    }

    /** Asks the GM to cast a spell; the GM picks the targets. One request waits at a time. */
    public void requestCast(@NonNull String characterId, @NonNull SpellRequests.Cast cast, @NonNull Callback callback) {
        cast.at = store.nextRequestAt(characterId, System.currentTimeMillis());
        SpellRequests.Cast clean = cast.cleaned();
        sendRequest(characterId, SpellRequests.KEY_CAST, clean.toMap(),
                () -> store.savePlayerCast(characterId, clean), () -> store.savePlayerCast(characterId, null), callback);
    }

    /** Asks the GM to end one of this character's spells. One request waits at a time. */
    public void requestEnd(@NonNull String characterId, @NonNull SpellRequests.Spell spell, @NonNull Callback callback) {
        SpellRequests.End end = new SpellRequests.End();
        end.at = store.nextRequestAt(characterId, System.currentTimeMillis());
        end.spellId = spell.id;
        end.name = spell.name;
        SpellRequests.End clean = end.cleaned();
        sendRequest(characterId, SpellRequests.KEY_END, clean.toMap(),
                () -> store.savePlayerEnd(characterId, clean), () -> store.savePlayerEnd(characterId, null), callback);
    }

    /**
     * Puts a spell request on the player's doc in the session. It is saved first, so a whole send
     * carries it too; when only adding it fails (the doc isn't there, or the session's expiry
     * changed), the whole sheet is sent with it. Forgotten again when that fails as well.
     */
    private void sendRequest(String characterId, String key, Map<String, Object> value, Runnable save,
                             Runnable forget, Callback callback) {
        String code = store.getPlayerCode(characterId);
        if (code == null) {
            callback.onNotFound();
            return;
        }
        save.run();
        notifyStateChanged();
        Callback result = new Callback() {
            @Override
            public void onSuccess() {
                notifyStateChanged();
                callback.onSuccess();
            }

            @Override
            public void onNotFound() {
                forget.run();
                notifyStateChanged();
                callback.onNotFound();
            }

            @Override
            public void onFailure(Exception e) {
                forget.run();
                notifyStateChanged();
                callback.onFailure(e);
            }
        };
        signIn(new SignInCallback() {
            @Override
            public void onSignedIn(String uid) {
                repository.savePlayerRequest(code, SyncIds.playerDocId(uid, characterId), key, value,
                        new SyncRepository.CompletionCallback() {
                            @Override
                            public void onSuccess() {
                                result.onSuccess();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                send(characterId, host.loadPlayer(characterId), result);
                            }
                        });
            }

            @Override
            public void onFailure(Exception e) {
                result.onFailure(e);
            }
        });
    }

    /**
     * Follows the GM's copy of this character live, while its screen is open. Does nothing when
     * it isn't in a session or is followed already.
     */
    public void watchPlayer(String characterId) {
        String code = store.getPlayerCode(characterId);
        if (code == null || playerWatches.containsKey(characterId)) return;
        ensureFirebase();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String docId = SyncIds.playerDocId(user.getUid(), characterId);
        playerWatches.put(characterId, repository.listenToGmCopy(code, docId, new SyncRepository.GmCopyListener() {
            @Override
            public void onGmCopy(@Nullable GmCopy copy) {
                if (!code.equals(store.getPlayerCode(characterId))) return;
                if (copy != null) {
                    onGmCopyReceived(characterId, copy);
                } else {
                    // Not added yet, or the GM ended the session (which removes the copies too)
                    checkSessionEnded(characterId, code);
                }
            }

            @Override
            public void onError(FirebaseFirestoreException error) {
                Log.w(TAG, "GM copy listener error: " + error.getCode(), error);
                // Firestore has removed the listener; the next time the screen opens it is attached again
                playerWatches.remove(characterId);
            }
        }));
    }

    public void unwatchPlayer(String characterId) {
        ListenerRegistration registration = playerWatches.remove(characterId);
        if (registration != null) registration.remove();
    }

    /** Leaves the joined session; the sheet sent to it is removed (best effort). */
    public void leave(String characterId) {
        unwatchPlayer(characterId);
        String code = store.getPlayerCode(characterId);
        if (code != null) {
            ensureFirebase();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) repository.deletePlayer(code, SyncIds.playerDocId(user.getUid(), characterId));
        }
        playerExpiries.remove(characterId);
        store.setPlayerCode(characterId, null);
        notifyStateChanged();
    }

    /** A player character is being deleted: leave its session and forget its sync state. */
    public void forgetPlayer(String characterId) {
        leave(characterId);
        store.forgetPlayer(characterId);
    }

    // ---------------------------------------------------------------- Private: player

    /**
     * Merges the GM's version into the player character. When it answers the last send, the
     * merge base is what was sent, so refused values go back to the GM's; otherwise it is the GM's
     * previous version, so only what the GM changed comes in.
     */
    private void onGmCopyReceived(String characterId, GmCopy copy) {
        Log.d(TAG, "GM copy received, answered " + copy.answeredSentAt);
        if (copy.ended != GmCopy.Ending.NONE) {
            // A removal or unlink from before the last join is old: ignore it, also its sheet
            if (!SyncRules.endsPlayer(copy, store.getPlayerJoinFloor(characterId))) return;
            store.clearPlayerSent(characterId);
            leave(characterId);
            Answer ended = copy.ended == GmCopy.Ending.REMOVED ? Answer.REMOVED : Answer.UNLINKED;
            if (playerUpdateListener != null) playerUpdateListener.onPlayerUpdated(characterId, ended, 0);
            return;
        }
        PlayerSheet sent = store.loadPlayerSent(characterId);
        long lastSentAt = store.getPlayerSentAt(characterId);
        boolean answer = sent != null && copy.answeredSentAt >= lastSentAt;
        if (answer && copy.rejected) {
            store.clearPlayerSent(characterId);
            leave(characterId);
            if (playerUpdateListener != null) playerUpdateListener.onPlayerUpdated(characterId, Answer.REJECTED, 0);
            return;
        }
        if (copy.rejected) return;

        store.savePlayerSpells(characterId, copy.spells);
        SpellRequests.Cast cast = store.loadPlayerCast(characterId);
        boolean castAnswered = cast != null && copy.castAnswer.answers(cast.at);
        if (castAnswered) store.savePlayerCast(characterId, null);
        SpellRequests.End end = store.loadPlayerEnd(characterId);
        boolean endAnswered = end != null && copy.endAnswer.answers(end.at);
        if (endAnswered) store.savePlayerEnd(characterId, null);

        CharacterModel character = host.loadPlayer(characterId);
        PlayerSheet local = PlayerSheet.of(character);
        PlayerSheet base = store.loadPlayerBase(characterId);
        PlayerSheet mergeBase = answer ? sent : base != null ? base : copy.sheet;
        PlayerSheet merged = SheetMerge.merge(mergeBase, local, copy.sheet);

        Answer result = Answer.NONE;
        int kept = 0;
        if (answer) {
            kept = SyncDiff.changes(copy.sheet, sent).size();
            result = kept == 0 ? Answer.ACCEPTED : Answer.KEPT;
            store.setPlayerAnswered(characterId, lastSentAt);
            store.clearPlayerSent(characterId);
        }
        store.savePlayerBase(characterId, copy.sheet);
        store.setPlayerSyncedAt(characterId, System.currentTimeMillis());
        if (!SyncDiff.changes(local, merged).isEmpty()) {
            merged.applyTo(character);
            host.savePlayer(characterId, character);
        }
        notifyStateChanged();
        if (playerUpdateListener != null) {
            playerUpdateListener.onPlayerUpdated(characterId, result, kept);
            if (castAnswered) playerUpdateListener.onSpellAnswered(characterId, cast.name, true, copy.castAnswer.ok);
            if (endAnswered) playerUpdateListener.onSpellAnswered(characterId, end.name, false, copy.endAnswer.ok);
        }
    }

    /**
     * The GM's copy is gone: when the session is gone too, the character leaves it and the player
     * is told. Ending a session deletes the copies before the session itself, so a session that is
     * still there is checked again a little later.
     */
    private void checkSessionEnded(String characterId, String code) {
        checkSessionEnded(characterId, code, SESSION_END_CHECKS);
    }

    private void checkSessionEnded(String characterId, String code, int checksLeft) {
        repository.fetchExpiry(code, new SyncRepository.ExpiryCallback() {
            @Override
            public void onResult(@Nullable Timestamp expiresAt) {
                if (!code.equals(store.getPlayerCode(characterId))) return;
                if (expiresAt != null) {
                    // Only a player who had the GM's copy can have lost it; a new one just waits
                    if (checksLeft > 1 && store.loadPlayerBase(characterId) != null) {
                        timeoutHandler.postDelayed(() -> checkSessionEnded(characterId, code, checksLeft - 1),
                                SESSION_END_CHECK_DELAY_MS);
                    }
                    return;
                }
                leaveGoneSession(characterId);
                if (playerUpdateListener != null) {
                    playerUpdateListener.onPlayerUpdated(characterId, Answer.SESSION_ENDED, 0);
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Offline: the next send finds out
            }
        });
    }

    /** @return True when Firestore refused a write (the rules said no). */
    private static boolean isDenied(Exception e) {
        return e instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED;
    }

    private void leaveGoneSession(String characterId) {
        unwatchPlayer(characterId);
        playerExpiries.remove(characterId);
        store.setPlayerCode(characterId, null);
        notifyStateChanged();
    }

    // ---------------------------------------------------------------- Private: sign-in

    private interface SignInCallback {
        void onSignedIn(String uid);

        void onFailure(Exception e);
    }

    private void signIn(SignInCallback callback) {
        ensureFirebase();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            callback.onSignedIn(user.getUid());
            return;
        }
        // Without a connection sign-in waits forever, so give up after a while; only one answer counts
        boolean[] answered = {false};
        Runnable timeout = () -> {
            if (answered[0]) return;
            answered[0] = true;
            Log.w(TAG, "Anonymous sign-in timed out");
            callback.onFailure(new TimeoutException("Sign-in timed out"));
        };
        timeoutHandler.postDelayed(timeout, SIGN_IN_TIMEOUT_MS);
        auth.signInAnonymously().addOnCompleteListener(task -> {
            timeoutHandler.removeCallbacks(timeout);
            if (answered[0]) return;
            answered[0] = true;
            FirebaseUser signedIn = task.isSuccessful() ? task.getResult().getUser() : null;
            if (signedIn != null) {
                callback.onSignedIn(signedIn.getUid());
            } else {
                Exception e = task.getException() != null ? task.getException()
                        : new IllegalStateException("No user after sign-in");
                Log.w(TAG, "Anonymous sign-in failed", e);
                callback.onFailure(e);
            }
        });
    }

    private void ensureFirebase() {
        if (firebaseReady) return;
        firebaseSetup.run();
        firebaseReady = true;
    }

    // ---------------------------------------------------------------- Private: GM

    /**
     * Gives the hosted session's expiry: the known one, or else the one read from the server.
     * Nothing happens when the session is gone or the read fails; the next change tries again.
     */
    private void withHostExpiry(String code, Consumer<Timestamp> use) {
        if (hostExpiry != null) {
            use.accept(hostExpiry);
            return;
        }
        repository.fetchExpiry(code, new SyncRepository.ExpiryCallback() {
            @Override
            public void onResult(@Nullable Timestamp expiresAt) {
                if (expiresAt == null || !code.equals(store.getHostCode())) return;
                hostExpiry = expiresAt;
                use.accept(expiresAt);
            }

            @Override
            public void onFailure(Exception e) {
                // Logged by the repository; the next change tries again
            }
        });
    }

    /** A write with the known expiry failed: read it from the server next time. */
    private void forgetHostExpiry() {
        hostExpiry = null;
    }

    private void attachListener(String code) {
        cancelRetry();
        detachListener();
        playersRegistration = repository.listenToPlayers(code, new SyncRepository.PlayersListener() {
            @Override
            public void onPlayers(Map<String, PlayerSheet> sheets) {
                if (!code.equals(store.getHostCode())) return;
                retryDelayMs = RETRY_MIN_MS;
                boolean firstLoad = !playersLoaded;
                lastSheets = new LinkedHashMap<>(sheets);
                playersLoaded = true;
                Map<String, Long> answered = store.loadApplied();
                requests.keySet().retainAll(sheets.keySet());
                for (Map.Entry<String, PlayerSheet> e : sheets.entrySet()) {
                    Long last = answered.get(e.getKey());
                    if (last == null || e.getValue().sentAt > last) {
                        requests.put(e.getKey(), e.getValue());
                    } else {
                        requests.remove(e.getKey());
                    }
                }
                Map<String, SpellRequests.Answer> castAnswers = store.loadCastAnswers();
                Map<String, SpellRequests.Answer> endAnswers = store.loadEndAnswers();
                castRequests.clear();
                endRequests.clear();
                for (Map.Entry<String, PlayerSheet> e : sheets.entrySet()) {
                    SpellRequests.Cast cast = e.getValue().cast;
                    SpellRequests.Answer castAnswer = castAnswers.get(e.getKey());
                    if (cast != null && (castAnswer == null || !castAnswer.answers(cast.at))) {
                        castRequests.put(e.getKey(), cast);
                    }
                    SpellRequests.End end = e.getValue().end;
                    SpellRequests.Answer endAnswer = endAnswers.get(e.getKey());
                    if (end != null && (endAnswer == null || !endAnswer.answers(end.at))) {
                        endRequests.put(e.getKey(), end);
                    }
                }
                notifyStateChanged();
                notifyRequests();
                // Once per run, all the GM's copies go up: an answer lost on its way (app closed, connection
                // gone) reaches the player this way, so they don't keep waiting
                if (firstLoad) uploadNow();
            }

            @Override
            public void onError(FirebaseFirestoreException error) {
                Log.w(TAG, "Players listener error: " + error.getCode(), error);
                // Firestore has removed the listener already
                playersRegistration = null;
                scheduleRetry(code);
            }
        });
    }

    /**
     * Tells a synced player their link ended: a GM copy with the notice, answering everything
     * they sent, and their sheet in the session is removed so it can't come back as a request.
     */
    private void sendNotice(CharacterModel character, GmCopy.Ending ending) {
        String code = store.getHostCode();
        String docId = character.getSyncId();
        PlayerSheet last = lastSheets.get(docId);
        long answeredSentAt = SyncRules.noticeSentAt(last != null ? last.sentAt : null, store.loadApplied().get(docId));
        markAnswered(docId, answeredSentAt);
        uploaded.remove(docId);
        GmCopy notice = GmCopy.notice(character, answeredSentAt, ending);
        repository.deletePlayer(code, docId);
        // When this fails the player finds out when they send again
        withHostExpiry(code, expiresAt -> repository.saveGmCopy(code, docId, notice, expiresAt, this::forgetHostExpiry));
    }

    /** Remembers that the GM answered this send; an older answer never overrides a newer one. */
    private void markAnswered(String docId, long sentAt) {
        Map<String, Long> answered = store.loadApplied();
        Long last = answered.get(docId);
        if (last == null || sentAt > last) {
            answered.put(docId, sentAt);
            store.saveApplied(answered);
        }
        PlayerSheet waiting = requests.get(docId);
        if (waiting != null && waiting.sentAt <= sentAt) requests.remove(docId);
    }

    /** @return The first character in the active party sent by this player character, or null. */
    @Nullable
    private CharacterModel find(String docId) {
        for (CharacterModel c : host.getEncounter().getCharacters()) {
            if (docId.equals(c.getSyncId())) return c;
        }
        return null;
    }

    /** @return True when hosting and the hosted party is the one in the encounter. */
    private boolean hostedPartyIsActive() {
        String partyId = store.getHostPartyId();
        return store.getHostCode() != null && partyId != null && partyId.equals(host.getActivePartyId());
    }

    private void startWatchingParty() {
        host.getEncounter().addListener(this);
    }

    private void scheduleUpload() {
        if (!hostedPartyIsActive()) return;
        uploadHandler.removeCallbacks(upload);
        uploadHandler.postDelayed(upload, UPLOAD_DELAY_MS);
    }

    private void uploadNow() {
        uploadHandler.removeCallbacks(upload);
        uploadGmCopies();
    }

    /** Uploads the GM's copy of every synced character in the hosted party that changed. */
    private void uploadGmCopies() {
        if (!hostedPartyIsActive()) return;
        String code = store.getHostCode();
        Map<String, Long> answered = store.loadApplied();
        Map<String, SpellRequests.Answer> castAnswers = store.loadCastAnswers();
        Map<String, SpellRequests.Answer> endAnswers = store.loadEndAnswers();
        Map<String, GmCopy> changed = new HashMap<>();
        for (CharacterModel c : host.getEncounter().getCharacters()) {
            String docId = c.getSyncId();
            if (docId == null || SyncIds.ownerUid(docId).isEmpty() || changed.containsKey(docId)) continue;
            Long last = answered.get(docId);
            GmCopy copy = GmCopy.of(c, last != null ? last : 0);
            copy.spells = SpellRequests.spellsOf(host.getEncounter(), c);
            if (castAnswers.containsKey(docId)) copy.castAnswer = castAnswers.get(docId);
            if (endAnswers.containsKey(docId)) copy.endAnswer = endAnswers.get(docId);
            if (!copy.toMap().equals(uploaded.get(docId))) changed.put(docId, copy);
        }
        if (changed.isEmpty()) return;
        withHostExpiry(code, expiresAt -> {
            for (Map.Entry<String, GmCopy> e : changed.entrySet()) {
                String docId = e.getKey();
                Log.d(TAG, "Uploading GM copy for " + docId);
                // A failed copy goes up again with the next change, with the expiry read again
                repository.saveGmCopy(code, docId, e.getValue(), expiresAt, () -> {
                    uploaded.remove(docId);
                    forgetHostExpiry();
                });
                uploaded.put(docId, e.getValue().toMap());
            }
        });
    }

    /**
     * Checks that the hosted session still exists: gone ends it, there renews its expiry (and
     * attaches the listener again when asked). A failed check keeps the session.
     */
    private void verify(String code, boolean reattach) {
        repository.fetchExpiry(code, new SyncRepository.ExpiryCallback() {
            @Override
            public void onResult(@Nullable Timestamp expiresAt) {
                if (!code.equals(store.getHostCode())) return;
                if (expiresAt == null) {
                    Log.i(TAG, "Hosted session is gone: " + code);
                    endHostingLocally();
                    return;
                }
                hostExpiry = expiresAt;
                if (reattach) attachListener(code);
                repository.extendSessionIfNeeded(code, renewed -> {
                    if (code.equals(store.getHostCode())) hostExpiry = renewed;
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (reattach) scheduleRetry(code);
            }
        });
    }

    private void endHostingLocally() {
        cancelRetry();
        detachListener();
        uploadHandler.removeCallbacks(upload);
        host.getEncounter().removeListener(this);
        store.stopHosting();
        hostExpiry = null;
        lastSheets = new LinkedHashMap<>();
        playersLoaded = false;
        requests.clear();
        castRequests.clear();
        endRequests.clear();
        uploaded.clear();
        retryDelayMs = RETRY_MIN_MS;
        notifyStateChanged();
        notifyRequests();
    }

    private void scheduleRetry(String code) {
        long delay = retryDelayMs;
        retryDelayMs = Math.min(retryDelayMs * 2, RETRY_MAX_MS);
        cancelRetry();
        mainHandler.postDelayed(() -> {
            if (code.equals(store.getHostCode())) verify(code, true);
        }, delay);
    }

    private void cancelRetry() {
        mainHandler.removeCallbacksAndMessages(null);
    }

    private void detachListener() {
        if (playersRegistration != null) {
            playersRegistration.remove();
            playersRegistration = null;
        }
    }

    private void notifyStateChanged() {
        for (StateListener l : new ArrayList<>(stateListeners)) {
            l.onSyncStateChanged();
        }
    }

    private void notifyRequests() {
        if (requestListener != null) requestListener.onRequestsChanged();
    }
}
