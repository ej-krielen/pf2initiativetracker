package com.rekijan.initiativetrackersecondedition.character.model;

import static com.rekijan.initiativetrackersecondedition.AppConstants.AC_AND_SAVES;
import static com.rekijan.initiativetrackersecondedition.AppConstants.HP;
import static com.rekijan.initiativetrackersecondedition.AppConstants.INITIATIVE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Parcel;
import android.os.Parcelable;

import com.rekijan.initiativetrackersecondedition.R;

import java.util.ArrayList;

/**
 * Model class for the Character
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 11-10-2015
 */
public class CharacterModel implements Parcelable {

    private transient Context context; //transient to skip gson from trying to include it
    private long id;
    private long party_id;
    private int initiative;
    private int initiativeBonus;
    private String skills;
    private String attackRoutine;
    private String ac;
    private String saves;
    private int hp;
    private int maxHp;
    private boolean isFirstInRound;
    private boolean isPC;
    private int fastHealing;
    private int regeneration;

    private boolean isDying;
    private int dyingValue;
    private int maxDyingValue;
    private int woundedValue;
    private int doomedValue;
    private int recoveryDC;
    private int heroPoints;
    private boolean reactionAvailable;

    private boolean reactionCollapsed;

    private String characterName;
    private String characterNotes;

    private ArrayList<DebuffModel> debuffList = new ArrayList<>();
    private ArrayList<ReactionModel> reactionsList = new ArrayList<>();

    public CharacterModel(Context context) {
        initiative = INITIATIVE;
        initiativeBonus = INITIATIVE;
        skills = "";
        attackRoutine = "";
        boolean isTablet = context.getResources().getBoolean(R.bool.isTablet);
        ac = "" + AC_AND_SAVES;
        saves = isTablet ? String.format(context.getString(R.string.standard_saves), AC_AND_SAVES) : String.format(context.getString(R.string.standard__mobile_saves), AC_AND_SAVES);
        hp = HP;
        maxHp = HP;
        characterName = "";
        characterNotes = "";
        isFirstInRound = false;
        isPC = false;
        fastHealing = 0;
        regeneration = 0;
        isDying = false;
        dyingValue = 0;
        maxDyingValue = 4;
        woundedValue = 0;
        doomedValue = 0;
        recoveryDC = 10;
        heroPoints = 1;
        reactionAvailable = true;
        reactionCollapsed = false;
        this.context = context;
    }

    public CharacterModel(Context context, int initiative, int initiativeBonus, String skills, String attackRoutine, String ac, String saves, int hp, int maxHp, String characterName,
                            String characterNotes, boolean isFirstInRound, boolean isPC, int fastHealing, int regeneration,
                            boolean isDying, int dyingValue, int maxDyingValue, int woundedValue, int doomedValue, int recoveryDC, int heroPoints, boolean reactionAvailable, boolean reactionCollapsed) {
        this.initiative = initiative;
        this.initiativeBonus = initiativeBonus;
        this.skills = skills;
        this.attackRoutine = attackRoutine;
        this.ac = ac;
        this.saves = saves;
        this.hp = hp;
        this.maxHp = maxHp;
        this.characterName = characterName;
        this.characterNotes = characterNotes;
        this.isFirstInRound = isFirstInRound;
        this.isPC = isPC;
        this.fastHealing = fastHealing;
        this.regeneration = regeneration;
        this.isDying = isDying;
        this.dyingValue = dyingValue;
        this.maxDyingValue = maxDyingValue;
        this.woundedValue = woundedValue;
        this.doomedValue = doomedValue;
        this.recoveryDC = recoveryDC;
        this.heroPoints = heroPoints;
        this.reactionAvailable = reactionAvailable;
        this.reactionCollapsed = reactionCollapsed;

        this.context = context;
    }

    /**
     * Called by the {@link com.rekijan.initiativetrackersecondedition.character.adapter.CharacterAdapter} when its a characters turn <br>
     * Lowers all (de)buff values by one, but never to negative. <br>
     * Checks when a debuff goes to 0 duration for the first time and asks user if they want to remove those.
     * @param context
     */
    public void updateDebuffs(Context context) {
        int debuffPosition = 0;
        for (DebuffModel d: debuffList) {
            int duration = d.getDuration();
            duration--;
            if (duration == 0) askRoundResetConfirmation(context, d, debuffPosition);
            if (duration < 0) duration = 0;
            d.setDuration(duration);
            debuffPosition++;
        }
        this.setDebuffList(debuffList);
    }

    public void updateReactions() {
        setReactionAvailable(true);
        for (ReactionModel r: reactionsList) {
            r.setAvailable(true);
        }
    }

    /**
     * Create a dialog so the user can choose to keep or remove a debuff that just went to 0 duration
     * @param context used to create dialog
     * @param debuff Debuff that has just been expired
     * @param debuffPosition position in list for debuff
     */
    private void askRoundResetConfirmation(Context context, DebuffModel debuff, final int debuffPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        builder.setMessage(String.format(context.getString(R.string.debuff_expired), debuff.getName()))
                .setTitle(context.getString(R.string.dialog_debuff_expired_title));
        builder.setPositiveButton(context.getString(R.string.dialog_debuff_remove), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                debuffList.remove(debuffPosition);
            }
        });
        builder.setNegativeButton(context.getString(R.string.dialog_debuff_keep), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    public boolean isAlreadyDead()
    {
        return getDyingValue() >= getMaxDyingValue()-getDoomedValue();
    }

    public boolean doesCharacterHavePersistentDamage()
    {
        for (DebuffModel d: debuffList) {
            if (d.isPersistentDamage()) return true;
        }
        return false;
    }

    public ArrayList<DebuffModel> getPersistentDamageModels()
    {
        ArrayList<DebuffModel> returnList = new ArrayList<>();
        for (DebuffModel d: debuffList) {
            if (d.isPersistentDamage()) {
                returnList.add(d);
            }
        }
        return returnList;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getParty_id() {
        return party_id;
    }

    public void setParty_id(long party_id) {
        this.party_id = party_id;
    }

    public int getInitiative() {
        return initiative;
    }

    public void setInitiative(int initiative) {
        this.initiative = initiative;
    }

    public int getInitiativeBonus() {
        return initiativeBonus;
    }

    public void setInitiativeBonus(int initiativeBonus) {
        this.initiativeBonus = initiativeBonus;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getAttackRoutine() {
        return attackRoutine;
    }

    public void setAttackRoutine(String attackRoutine) {
        this.attackRoutine = attackRoutine;
    }

    public String getAc() { return ac; }

    public void setAc(String ac) { this.ac = ac; }

    public String getSaves() { return saves; }

    public void setSaves(String saves) { this.saves = saves; }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public String getCharacterName() {
        return characterName;
    }

    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }

    public String getCharacterNotes() {
        return characterNotes;
    }

    public void setCharacterNotes(String characterNotes) {
        this.characterNotes = characterNotes;
    }

    public Context getContext() { return context; }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setIsFirstRound(boolean isFirstInRound) {
        this.isFirstInRound = isFirstInRound;
    }

    public boolean isFirstRound() { return isFirstInRound; }

    public void setIsPC(boolean isPC) {
        this.isPC = isPC;
    }

    public boolean isPC() { return isPC; }

    public void setFastHealing(int fastHealing) {
        this.fastHealing = fastHealing;
    }

    public int getFastHealing() {
        return fastHealing;
    }

    public void setRegeneration(int regeneration) {
        this.regeneration = regeneration;
    }

    public int getRegeneration() {
        return regeneration;
    }

    public boolean isDying() {
        return isDying;
    }

    public void setIsDying(boolean dying) {
        isDying = dying;
    }

    public int getDyingValue() {
        return dyingValue;
    }

    public void setDyingValue(int dyingValue) {
        this.dyingValue = dyingValue;
    }

    public int getMaxDyingValue() {
        return maxDyingValue;
    }

    public void setMaxDyingValue(int maxDyingValue) {
        this.maxDyingValue = maxDyingValue;
    }

    public int getWoundedValue() {
        return woundedValue;
    }

    public void setWoundedValue(int woundedValue) {
        this.woundedValue = woundedValue;
    }

    public int getDoomedValue() {
        return doomedValue;
    }

    public void setDoomedValue(int doomedValue) {
        this.doomedValue = doomedValue;
    }

    public int getRecoveryDC() {
        return recoveryDC;
    }

    public void setRecoveryDC(int recoveryDC) {
        this.recoveryDC = recoveryDC;
    }

    public int getHeroPoints() {
        return heroPoints;
    }

    public void setHeroPoints(int heroPoints) {
        this.heroPoints = heroPoints;
    }

    public boolean isReactionAvailable() {
        return reactionAvailable;
    }

    public void setReactionAvailable(boolean reactionAvailable) {
        this.reactionAvailable = reactionAvailable;
    }

    public boolean isReactionCollapsed() {
        return reactionCollapsed;
    }

    public void setReactionCollapsed(boolean reactionCollapsed) {
        this.reactionCollapsed = reactionCollapsed;
    }

    public ArrayList<DebuffModel> getDebuffList() {
        //Need to check for null because previous iteration didn't have this array list
        if (debuffList == null) debuffList = new ArrayList<>();
        return debuffList;
    }

    public ArrayList<ReactionModel> getReactionList() {
        //Need to check for null because previous iteration didn't have this array list
        if (reactionsList == null) reactionsList = new ArrayList<>();
        return reactionsList;
    }

    public void setDebuffList(ArrayList<DebuffModel> debuffList) {
        this.debuffList = debuffList;
    }

    public void setReactionList(ArrayList<ReactionModel> reactionsList) {
        this.reactionsList = reactionsList;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeLong(this.party_id);
        dest.writeInt(this.initiative);
        dest.writeInt(this.initiativeBonus);
        dest.writeString(this.skills);
        dest.writeString(this.attackRoutine);
        dest.writeString(this.ac);
        dest.writeString(this.saves);
        dest.writeInt(this.hp);
        dest.writeInt(this.maxHp);
        dest.writeByte(this.isFirstInRound ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isPC ? (byte) 1 : (byte) 0);
        dest.writeString(this.characterName);
        dest.writeString(this.characterNotes);
        dest.writeTypedList(this.debuffList);
        dest.writeInt(this.fastHealing);
        dest.writeInt(this.regeneration);
        dest.writeByte(this.isDying ? (byte) 1 : (byte) 0);
        dest.writeInt(this.dyingValue);
        dest.writeInt(this.maxDyingValue);
        dest.writeInt(this.woundedValue);
        dest.writeInt(this.doomedValue);
        dest.writeInt(this.recoveryDC);
        dest.writeInt(this.heroPoints);
        dest.writeByte(this.reactionAvailable ? (byte) 1 : (byte) 0);
        dest.writeByte(this.reactionCollapsed ? (byte) 1 : (byte) 0);

    }

    protected CharacterModel(Parcel in) {
        this.id = in.readLong();
        this.party_id = in.readLong();
        this.initiative = in.readInt();
        this.initiativeBonus = in.readInt();
        this.skills = in.readString();
        this.attackRoutine = in.readString();
        this.ac = in.readString();
        this.saves = in.readString();
        this.hp = in.readInt();
        this.maxHp = in.readInt();
        this.isFirstInRound = in.readByte() != 0;
        this.isPC = in.readByte() != 0;
        this.characterName = in.readString();
        this.characterNotes = in.readString();
        this.debuffList = in.createTypedArrayList(DebuffModel.CREATOR);
        this.fastHealing = in.readInt();
        this.regeneration = in.readInt();
        this.isDying = in.readByte() != 0;
        this.dyingValue = in.readInt();
        this.maxDyingValue = in.readInt();
        this.woundedValue = in.readInt();
        this.doomedValue = in.readInt();
        this.recoveryDC = in.readInt();
        this.heroPoints = in.readInt();
        this.reactionAvailable = in.readByte() != 0;
        this.reactionCollapsed = in.readByte() != 0;
    }

    public static final Parcelable.Creator<CharacterModel> CREATOR = new Parcelable.Creator<CharacterModel>() {
        @Override
        public CharacterModel createFromParcel(Parcel source) {
            return new CharacterModel(source);
        }

        @Override
        public CharacterModel[] newArray(int size) {
            return new CharacterModel[size];
        }
    };
}