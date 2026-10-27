package com.rekijan.initiativetrackersecondedition.feature.encounter;

import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;

/**
 * Something the user has to resolve when the turn moves on, such as persistent damage or a
 * recovery check. {@link Encounter.Turn#steps} lists them in the order they are asked.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-10-2026
 */
public final class TurnStep {

    public enum Kind {
        /** End of turn: take the persistent damage, then the flat check. */
        PERSISTENT_DAMAGE,
        /** End of turn: a (de)buff ran out, remove it? */
        EXPIRED_DEBUFF,
        /** Start of turn: add fast healing? */
        FAST_HEALING,
        /** Start of turn: add regeneration? */
        REGENERATION,
        /** Start of turn: a dying character attempts a recovery check. */
        RECOVERY_CHECK
    }

    public final Kind kind;
    public final CharacterModel character;
    /** The (de)buff for persistent damage and expired (de)buffs, else null. */
    public final DebuffModel debuff;

    TurnStep(Kind kind, CharacterModel character, DebuffModel debuff) {
        this.kind = kind;
        this.character = character;
        this.debuff = debuff;
    }
}
