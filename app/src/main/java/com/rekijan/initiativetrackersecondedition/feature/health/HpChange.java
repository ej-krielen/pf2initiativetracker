package com.rekijan.initiativetrackersecondedition.feature.health;

import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;

/**
 * The rules for a new hp value: what it means for a character that is (or is not) dying. Plain
 * Java so the outcomes are unit tested; {@link HealthDialogs} asks the follow-up questions.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public final class HpChange {

    public enum Outcome {
        /** Nothing more to ask. */
        NONE,
        /** An NPC dropped to 0 hp or below: probably dead, offer to remove it. */
        OFFER_REMOVAL,
        /** A PC dropped to 0 hp: hp is set to 0, ask whether they are knocked out. */
        ASK_KNOCK_OUT,
        /** A dying character got 1 hp or more: no longer dying, wounded went up. */
        RECOVERED_BY_HEALING,
        /** A dying character took damage: hp stays 0, ask how much dying goes up. */
        DAMAGED_WHILE_DYING
    }

    private HpChange() {
    }

    /** Sets the new hp and applies the dying rules that follow from it. */
    public static Outcome apply(CharacterModel character, int newHp) {
        character.setHp(newHp);
        if (!character.isDying()) {
            if (newHp > 0) return Outcome.NONE;
            if (!character.isPC()) return Outcome.OFFER_REMOVAL;
            character.setHp(0);
            return Outcome.ASK_KNOCK_OUT;
        }
        if (newHp > 0) {
            character.loseDying();
            return Outcome.RECOVERED_BY_HEALING;
        }
        if (newHp < 0) {
            character.setHp(0);
            return Outcome.DAMAGED_WHILE_DYING;
        }
        return Outcome.NONE;
    }
}
