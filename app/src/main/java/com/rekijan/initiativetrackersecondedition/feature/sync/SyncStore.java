package com.rekijan.initiativetrackersecondedition.feature.sync;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.rekijan.initiativetrackersecondedition.core.data.KeyValueStore;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Saves the Table sync state on the device, in the app's SharedPreferences:
 * <ul>
 *     <li>GM side: the code of the hosted session, the party it sends to, and per player the
 *     sentAt of the send the GM last approved or refused; and which players the GM removed from
 *     which party, so a new request from them says so; and per player the GM's answer to their
 *     last cast and end spell requests.</li>
 *     <li>Player side, per player character: the code of the joined session; when it was last
 *     sent and last updated from the GM; the GM's version it last got (the merge base); what it
 *     last sent, until answered; which answer it has seen; and its sentAt when it joined, so a
 *     removal notice from before then is old. Also its cast and end spell requests until
 *     answered, the time of the last one, and the spells it has going as the GM last said. The
 *     character itself is kept by PlayerBook.</li>
 * </ul>
 * Plain Java on top of {@link KeyValueStore}, so it is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-10-2026
 */
public class SyncStore {

    static final String KEY_HOST_CODE = "sync_host_code";
    static final String KEY_HOST_PARTY = "sync_host_party_id";
    static final String KEY_HOST_APPLIED = "sync_host_applied";
    static final String KEY_HOST_REMOVED = "sync_host_removed";
    static final String KEY_PLAYER_CODE_PREFIX = "sync_player_code_";
    static final String KEY_PLAYER_SENT_AT_PREFIX = "sync_player_sent_at_";
    static final String KEY_PLAYER_SYNCED_AT_PREFIX = "sync_player_synced_at_";
    static final String KEY_PLAYER_BASE_PREFIX = "sync_player_base_";
    static final String KEY_PLAYER_SENT_PREFIX = "sync_player_sent_";
    static final String KEY_PLAYER_ANSWERED_PREFIX = "sync_player_answered_";
    static final String KEY_PLAYER_JOIN_FLOOR_PREFIX = "sync_player_join_floor_";
    static final String KEY_HOST_CAST_ANSWERS = "sync_host_cast_answers";
    static final String KEY_HOST_END_ANSWERS = "sync_host_end_answers";
    static final String KEY_PLAYER_CAST_PREFIX = "sync_player_cast_";
    static final String KEY_PLAYER_END_SPELL_PREFIX = "sync_player_end_spell_";
    static final String KEY_PLAYER_SPELLS_PREFIX = "sync_player_spells_";
    static final String KEY_PLAYER_REQUEST_AT_PREFIX = "sync_player_request_at_";

    private static final Gson GSON = new Gson();
    private static final Type ANSWERS_TYPE = new TypeToken<HashMap<String, SpellRequests.Answer>>() {
    }.getType();
    private static final Type SPELLS_TYPE = new TypeToken<ArrayList<SpellRequests.Spell>>() {
    }.getType();
    private static final Type APPLIED_TYPE = new TypeToken<HashMap<String, Long>>() {
    }.getType();
    private static final Type REMOVED_TYPE = new TypeToken<HashMap<String, String>>() {
    }.getType();

    private final KeyValueStore values;

    public SyncStore(KeyValueStore values) {
        this.values = values;
    }

    // ---------------------------------------------------------------- GM

    /** @return The hosted session's code, or null when not hosting. */
    public String getHostCode() {
        return values.getString(KEY_HOST_CODE, null);
    }

    /** @return The id of the party the hosted session sends to, or null when not hosting. */
    public String getHostPartyId() {
        return values.getString(KEY_HOST_PARTY, null);
    }

    /** Starts hosting: a fresh session has no applied sends yet. */
    public void startHosting(String code, String partyId) {
        values.edit()
                .putString(KEY_HOST_CODE, code)
                .putString(KEY_HOST_PARTY, partyId)
                .remove(KEY_HOST_APPLIED)
                .remove(KEY_HOST_CAST_ANSWERS)
                .remove(KEY_HOST_END_ANSWERS)
                .apply();
    }

    public void stopHosting() {
        values.edit()
                .remove(KEY_HOST_CODE)
                .remove(KEY_HOST_PARTY)
                .remove(KEY_HOST_APPLIED)
                .remove(KEY_HOST_CAST_ANSWERS)
                .remove(KEY_HOST_END_ANSWERS)
                .apply();
    }

    /** @return A modifiable copy of the GM's answers to cast requests, per player; empty when none. */
    public Map<String, SpellRequests.Answer> loadCastAnswers() {
        return readAnswers(KEY_HOST_CAST_ANSWERS);
    }

    public void saveCastAnswers(Map<String, SpellRequests.Answer> answers) {
        values.edit().putString(KEY_HOST_CAST_ANSWERS, GSON.toJson(answers, ANSWERS_TYPE)).apply();
    }

    /** @return A modifiable copy of the GM's answers to end spell requests, per player; empty when none. */
    public Map<String, SpellRequests.Answer> loadEndAnswers() {
        return readAnswers(KEY_HOST_END_ANSWERS);
    }

    public void saveEndAnswers(Map<String, SpellRequests.Answer> answers) {
        values.edit().putString(KEY_HOST_END_ANSWERS, GSON.toJson(answers, ANSWERS_TYPE)).apply();
    }

    private Map<String, SpellRequests.Answer> readAnswers(String key) {
        String json = values.getString(key, null);
        Map<String, SpellRequests.Answer> answers = new HashMap<>();
        if (json == null) return answers;
        try {
            Map<String, SpellRequests.Answer> map = GSON.fromJson(json, ANSWERS_TYPE);
            if (map == null) return answers;
            for (Map.Entry<String, SpellRequests.Answer> e : map.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) answers.put(e.getKey(), e.getValue());
            }
        } catch (JsonParseException e) {
            // Unreadable: no answers
        }
        return answers;
    }

    /** @return A modifiable copy of the applied sends; empty when none or unreadable. */
    public Map<String, Long> loadApplied() {
        String json = values.getString(KEY_HOST_APPLIED, null);
        if (json == null) return new HashMap<>();
        try {
            Map<String, Long> map = GSON.fromJson(json, APPLIED_TYPE);
            return map != null ? new HashMap<>(map) : new HashMap<>();
        } catch (JsonParseException e) {
            return new HashMap<>();
        }
    }

    public void saveApplied(Map<String, Long> applied) {
        values.edit().putString(KEY_HOST_APPLIED, GSON.toJson(applied, APPLIED_TYPE)).apply();
    }

    /** Remembers that the GM removed this player's character from this party. Kept across sessions. */
    public void markRemoved(String docId, String partyId) {
        Map<String, String> removed = loadRemoved();
        removed.put(docId, partyId);
        values.edit().putString(KEY_HOST_REMOVED, GSON.toJson(removed, REMOVED_TYPE)).apply();
    }

    /** @return True when the GM removed this player's character from this party before. */
    public boolean wasRemoved(String docId, String partyId) {
        return partyId != null && partyId.equals(loadRemoved().get(docId));
    }

    /** The player is in a party again (added or linked): forget the removal. */
    public void clearRemoved(String docId) {
        Map<String, String> removed = loadRemoved();
        if (removed.remove(docId) != null) {
            values.edit().putString(KEY_HOST_REMOVED, GSON.toJson(removed, REMOVED_TYPE)).apply();
        }
    }

    private Map<String, String> loadRemoved() {
        String json = values.getString(KEY_HOST_REMOVED, null);
        if (json == null) return new HashMap<>();
        try {
            Map<String, String> map = GSON.fromJson(json, REMOVED_TYPE);
            return map != null ? new HashMap<>(map) : new HashMap<>();
        } catch (JsonParseException e) {
            return new HashMap<>();
        }
    }

    // ---------------------------------------------------------------- Player (per character)

    /** @return The code of the session this player character joined, or null when not joined. */
    public String getPlayerCode(String characterId) {
        return values.getString(KEY_PLAYER_CODE_PREFIX + characterId, null);
    }

    /**
     * Joins (code) or leaves (null); leaving also forgets when it was last synced. Joining
     * remembers the last sentAt as the join floor ({@link #getPlayerJoinFloor}).
     */
    public void setPlayerCode(String characterId, String code) {
        if (code == null) {
            values.edit()
                    .remove(KEY_PLAYER_CODE_PREFIX + characterId)
                    .remove(KEY_PLAYER_SYNCED_AT_PREFIX + characterId)
                    .remove(KEY_PLAYER_BASE_PREFIX + characterId)
                    .remove(KEY_PLAYER_SENT_PREFIX + characterId)
                    .remove(KEY_PLAYER_ANSWERED_PREFIX + characterId)
                    .remove(KEY_PLAYER_CAST_PREFIX + characterId)
                    .remove(KEY_PLAYER_END_SPELL_PREFIX + characterId)
                    .remove(KEY_PLAYER_SPELLS_PREFIX + characterId)
                    .apply();
        } else {
            values.edit()
                    .putString(KEY_PLAYER_CODE_PREFIX + characterId, code)
                    .putString(KEY_PLAYER_JOIN_FLOOR_PREFIX + characterId, Long.toString(getPlayerSentAt(characterId)))
                    .apply();
        }
    }

    /** @return The character's sentAt when it last joined; GM notices answering up to this are old. */
    public long getPlayerJoinFloor(String characterId) {
        return readLong(KEY_PLAYER_JOIN_FLOOR_PREFIX + characterId);
    }

    /** @return When this character was last sent, in ms, or 0 when never. */
    public long getPlayerSentAt(String characterId) {
        return readLong(KEY_PLAYER_SENT_AT_PREFIX + characterId);
    }

    /**
     * Picks the sentAt for a new send: the clock, but always later than the previous send, so the
     * GM's app sees it as new even when the phone's clock was set back.
     *
     * @param now The current time in ms.
     * @return The sentAt to use; not saved until {@link #setPlayerSentAt}.
     */
    public long nextSentAt(String characterId, long now) {
        return Math.max(now, getPlayerSentAt(characterId) + 1);
    }

    public void setPlayerSentAt(String characterId, long sentAt) {
        values.edit().putString(KEY_PLAYER_SENT_AT_PREFIX + characterId, Long.toString(sentAt)).apply();
    }

    /** @return When the GM's version last came in, in ms, or 0 when never. */
    public long getPlayerSyncedAt(String characterId) {
        return readLong(KEY_PLAYER_SYNCED_AT_PREFIX + characterId);
    }

    public void setPlayerSyncedAt(String characterId, long syncedAt) {
        values.edit().putString(KEY_PLAYER_SYNCED_AT_PREFIX + characterId, Long.toString(syncedAt)).apply();
    }

    /** Forgets everything about a deleted player character. */
    public void forgetPlayer(String characterId) {
        values.edit()
                .remove(KEY_PLAYER_CODE_PREFIX + characterId)
                .remove(KEY_PLAYER_SENT_AT_PREFIX + characterId)
                .remove(KEY_PLAYER_SYNCED_AT_PREFIX + characterId)
                .remove(KEY_PLAYER_BASE_PREFIX + characterId)
                .remove(KEY_PLAYER_SENT_PREFIX + characterId)
                .remove(KEY_PLAYER_ANSWERED_PREFIX + characterId)
                .remove(KEY_PLAYER_JOIN_FLOOR_PREFIX + characterId)
                .remove(KEY_PLAYER_CAST_PREFIX + characterId)
                .remove(KEY_PLAYER_END_SPELL_PREFIX + characterId)
                .remove(KEY_PLAYER_SPELLS_PREFIX + characterId)
                .remove(KEY_PLAYER_REQUEST_AT_PREFIX + characterId)
                .apply();
    }

    /** @return The cast request waiting for the GM, or null. */
    public SpellRequests.Cast loadPlayerCast(String characterId) {
        SpellRequests.Cast cast = read(KEY_PLAYER_CAST_PREFIX + characterId, SpellRequests.Cast.class);
        return cast != null ? cast.cleaned() : null;
    }

    /** Saves the request waiting for the GM, or forgets it (null). */
    public void savePlayerCast(String characterId, SpellRequests.Cast cast) {
        write(KEY_PLAYER_CAST_PREFIX + characterId, cast != null ? GSON.toJson(cast.cleaned()) : null);
    }

    /** @return The end spell request waiting for the GM, or null. */
    public SpellRequests.End loadPlayerEnd(String characterId) {
        SpellRequests.End end = read(KEY_PLAYER_END_SPELL_PREFIX + characterId, SpellRequests.End.class);
        return end != null ? end.cleaned() : null;
    }

    public void savePlayerEnd(String characterId, SpellRequests.End end) {
        write(KEY_PLAYER_END_SPELL_PREFIX + characterId, end != null ? GSON.toJson(end.cleaned()) : null);
    }

    /** @return The spells the character has going, as the GM last said; empty when none. */
    public List<SpellRequests.Spell> loadPlayerSpells(String characterId) {
        List<SpellRequests.Spell> spells = read(KEY_PLAYER_SPELLS_PREFIX + characterId, SPELLS_TYPE);
        List<SpellRequests.Spell> list = new ArrayList<>();
        if (spells == null) return list;
        for (SpellRequests.Spell s : spells) {
            if (s != null) list.add(s.cleaned());
        }
        return list;
    }

    public void savePlayerSpells(String characterId, List<SpellRequests.Spell> spells) {
        write(KEY_PLAYER_SPELLS_PREFIX + characterId, spells.isEmpty() ? null : GSON.toJson(spells, SPELLS_TYPE));
    }

    /**
     * Picks and saves the time of a new spell request: the clock, but always later than the
     * previous request, so the GM's app sees it as new even when the phone's clock was set back.
     */
    public long nextRequestAt(String characterId, long now) {
        long at = Math.max(now, readLong(KEY_PLAYER_REQUEST_AT_PREFIX + characterId) + 1);
        values.edit().putString(KEY_PLAYER_REQUEST_AT_PREFIX + characterId, Long.toString(at)).apply();
        return at;
    }

    private <T> T read(String key, Type type) {
        String json = values.getString(key, null);
        if (json == null) return null;
        try {
            return GSON.fromJson(json, type);
        } catch (JsonParseException e) {
            return null;
        }
    }

    private void write(String key, String json) {
        if (json == null) {
            values.edit().remove(key).apply();
        } else {
            values.edit().putString(key, json).apply();
        }
    }

    /** @return The GM's version this character last got (the merge base), or null when none yet. */
    public PlayerSheet loadPlayerBase(String characterId) {
        return readSheet(KEY_PLAYER_BASE_PREFIX + characterId);
    }

    public void savePlayerBase(String characterId, PlayerSheet sheet) {
        values.edit().putString(KEY_PLAYER_BASE_PREFIX + characterId, GSON.toJson(sheet.cleaned())).apply();
    }

    /** @return What this character last sent, until the GM answered it; null otherwise. */
    public PlayerSheet loadPlayerSent(String characterId) {
        return readSheet(KEY_PLAYER_SENT_PREFIX + characterId);
    }

    public void savePlayerSent(String characterId, PlayerSheet sheet) {
        values.edit().putString(KEY_PLAYER_SENT_PREFIX + characterId, GSON.toJson(sheet.cleaned())).apply();
    }

    public void clearPlayerSent(String characterId) {
        values.edit().remove(KEY_PLAYER_SENT_PREFIX + characterId).apply();
    }

    /** @return The sentAt of the last send whose answer from the GM this character has seen; 0 when none. */
    public long getPlayerAnswered(String characterId) {
        return readLong(KEY_PLAYER_ANSWERED_PREFIX + characterId);
    }

    public void setPlayerAnswered(String characterId, long sentAt) {
        values.edit().putString(KEY_PLAYER_ANSWERED_PREFIX + characterId, Long.toString(sentAt)).apply();
    }

    private PlayerSheet readSheet(String key) {
        String json = values.getString(key, null);
        if (json == null) return null;
        try {
            PlayerSheet sheet = GSON.fromJson(json, PlayerSheet.class);
            return sheet != null ? sheet.cleaned() : null;
        } catch (JsonParseException e) {
            return null;
        }
    }

    private long readLong(String key) {
        String text = values.getString(key, null);
        try {
            return text != null ? Long.parseLong(text) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
