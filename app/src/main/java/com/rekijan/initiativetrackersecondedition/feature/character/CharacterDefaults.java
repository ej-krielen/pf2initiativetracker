package com.rekijan.initiativetrackersecondedition.feature.character;

import android.content.Context;

import com.rekijan.initiativetrackersecondedition.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates new characters with the default stat lines (AC 10, saves 10) in the user's language,
 * and the starting party for a fresh install: three PCs and a monster.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public final class CharacterDefaults {

    private static final int DEFAULT_HP = 6;
    private static final int DEFAULT_AC_AND_SAVES = 10;
    private static final int STARTING_PARTY_SIZE = 4;

    private CharacterDefaults() {
    }

    public static CharacterModel newCharacter(Context context) {
        boolean isTablet = context.getResources().getBoolean(R.bool.isTablet);
        CharacterModel c = new CharacterModel();
        c.setAc(String.valueOf(DEFAULT_AC_AND_SAVES));
        c.setSaves(context.getString(isTablet ? R.string.standard_saves : R.string.standard__mobile_saves,
                DEFAULT_AC_AND_SAVES));
        c.setHp(DEFAULT_HP);
        c.setMaxHp(DEFAULT_HP);
        return c;
    }

    /** Three PCs and a monster, the first one is first in round. */
    public static List<CharacterModel> startingParty(Context context) {
        List<CharacterModel> party = new ArrayList<>();
        for (int i = 0; i < STARTING_PARTY_SIZE; i++) {
            CharacterModel c = newCharacter(context);
            c.setPC(i < STARTING_PARTY_SIZE - 1);
            party.add(c);
        }
        party.get(0).setFirstInRound(true);
        return party;
    }
}
