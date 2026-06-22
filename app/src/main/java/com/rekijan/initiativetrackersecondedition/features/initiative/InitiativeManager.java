package com.rekijan.initiativetrackersecondedition.features.initiative;

import android.content.Context;

import com.rekijan.initiativetrackersecondedition.common.helper.CharacterEditor;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.data.model.DebuffModel;
import com.rekijan.initiativetrackersecondedition.features.health.HitPointDialogHelper;
import com.rekijan.initiativetrackersecondedition.features.debuffs.DebuffDialogHelper;
import com.rekijan.initiativetrackersecondedition.features.health.DyingDialogHelper;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Owns the list of characters and the game rules for turns and sorting.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 21-6-2026
 */
public class InitiativeManager {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private final ArrayList<CharacterModel> characters = new ArrayList<>();
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods    ////////////////////////////////////////////////////////////////////////////////////////////////    /**     * Removes a character from the initiative list at a specific position.
     * Handles handing over the "First in Round" marker if the removed character was the first.
     *
     * @param position The 0-based index of the character to remove.
     */
    public void removeCharacterAt(int position) {
        if (position >= 0 && position < characters.size()) {
            boolean wasFirstRound = characters.get(position).isFirstRound();
            characters.remove(position);

            if (wasFirstRound && !characters.isEmpty()) {
                int newFirstIndex = (position == 0) ? characters.size() - 1 : position - 1;
                characters.get(newFirstIndex).setIsFirstRound(true);
            }
        }
    }

    /**
     * Sorts the {@link #characters} list by {@link CharacterModel#getInitiative()}. High to low, monsters going before PC if tied. <br>
     * And make the first character the first in the round.
     *
     * @return true if double initiatives (ties between same types) were found to alert the user.
     */
    public boolean sortInitiative() {
        Collections.sort(characters, (o1, o2) -> {
            int firstCompare = o2.getInitiative() - o1.getInitiative();
            if (firstCompare != 0) {
                return firstCompare;
            } else {
                return Boolean.compare(o1.isPC(), o2.isPC());
            }
        });

        for (CharacterModel c : characters) {
            c.setIsFirstRound(false);
        }
        if (!characters.isEmpty()) {
            characters.get(0).setIsFirstRound(true);
        }

        return checkForDoubleInitiative();
    }

    /**
     * Sets the CharacterModel at the specified position to be the first in the round (used to track when a new round begins).
     *
     * @param pos The 0-based index to set as first in round.
     */
    public void setFirstInRound(int pos) {
        for (CharacterModel c : characters) {
            if (c.isFirstRound()) {
                c.setIsFirstRound(false);
            }
        }
        if (pos >= 0 && pos < characters.size()) {
            characters.get(pos).setIsFirstRound(true);
        }
    }

    /**
     * Advances the encounter to the next turn.
     * Each {@link CharacterModel} is pushed down the list by one, bottom one becomes the top.<br>
     * Also handles end-of-turn actions for the outgoing character and start-of-turn actions for the incoming character.
     *
     * @param context Android context for showing dialogs.
     * @return true if the new active character is the first in the round, indicating a new round begins.
     */
    public boolean nextTurn(Context context) {
        if (characters.size() < 2) return false;

        processEndOfTurn(characters.get(0), context);
        rotateTurnOrder();

        CharacterModel activeCharacter = characters.get(0);
        processStartOfTurn(activeCharacter, context);

        return activeCharacter.isFirstRound();
    }

    /**
     * Moves the character to the bottom of the list and sets its dying value.
     * Also handles 'First in Round' logic handover if necessary.
     *
     * @param character          The character to move.
     * @param dyingValueIncrease The amount to increase the dying value by.
     */
    public void moveCharacterToDying(CharacterModel character, int dyingValueIncrease) {
        int posToRemove = characters.indexOf(character);
        if (posToRemove != -1) {
            // If it was the first in round, hand it over to the next character in the original list
            if (character.isFirstRound() && posToRemove != characters.size() - 1) {
                character.setIsFirstRound(false);
                characters.get(posToRemove + 1).setIsFirstRound(true);
            }

            character.setIsDying(true);
            character.setDyingValue(dyingValueIncrease + character.getWoundedValue());

            // Move to end
            characters.remove(posToRemove);
            characters.add(character);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Public helpers    ////////////////////////////////////////////////////////////////////////////////////////////////    public ArrayList<CharacterModel> getCharacters() {        return characters;
    }

    public void addCharacter(CharacterModel character) {
        characters.add(character);
    }

    public void removeCharacter(CharacterModel character) {
        characters.remove(character);
    }

    public void clearAllCharacters() {
        characters.clear();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Private helpers    ////////////////////////////////////////////////////////////////////////////////////////////////    private void processEndOfTurn(CharacterModel character, Context context) {        if (!character.isAlreadyDead()) {
            if (character.doesCharacterHavePersistentDamage()) {
                for (DebuffModel debuff : character.getPersistentDamageModels()) {
                    DebuffDialogHelper.getInstance().promptPersistentDamageDialog(character, debuff, context);
                }
            }
        }
        CharacterEditor.getInstance().updateDebuffs(context, character);
    }

    private void rotateTurnOrder() {
        CharacterModel character = characters.remove(0);
        characters.add(character);
    }

    private void processStartOfTurn(CharacterModel character, Context context) {
        CharacterEditor.getInstance().updateReactions(character);
        HitPointDialogHelper.getInstance().automaticHealingCheck(character, context);

        if (character.isDying() && !character.isAlreadyDead()) {
            DyingDialogHelper.getInstance().promptRecoveryCheckDialog(character, context);
        }
    }

    /**
     * Checks if there are CharacterModels with the same initiative. Monsters go before PCs, so if one is tied with the other it does not count.
     *
     * @return a boolean if double initiatives were found to alert to user of this fact.
     */
    private boolean checkForDoubleInitiative() {
        boolean doubleInitiativeDetected = false;
        for (CharacterModel m : characters) {
            if (doubleInitiativeDetected) break;
            for (CharacterModel m2 : characters) {
                if (m != m2 && m2.getInitiative() == m.getInitiative() && m.isPC() == m2.isPC()) {
                    doubleInitiativeDetected = true;
                    break;
                }
            }
        }
        return doubleInitiativeDetected;
    }
}