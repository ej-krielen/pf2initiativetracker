package com.rekijan.initiativetrackersecondedition.feature.condition;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.util.Dialogs;
import com.rekijan.initiativetrackersecondedition.core.util.NumberInput;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;
import com.rekijan.initiativetrackersecondedition.feature.health.HealthDialogs;
import com.rekijan.initiativetrackersecondedition.feature.health.HpChange;

import java.util.function.Consumer;

/**
 * Persistent damage: the dialog that creates it, and the one at the end of the character's turn
 * where the damage is taken and the DC 15 flat check can end it.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 24-3-2021
 */
public final class PersistentDamageDialogs {

    private PersistentDamageDialogs() {
    }

    /** Asks for the damage type, damage, DC, duration and description of new persistent damage. */
    public static void create(Context context, Consumer<DebuffModel> onCreated) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_persistent_damage_create, null);
        EditText typeEditText = view.findViewById(R.id.persistent_damage_dialog_type_editText);
        EditText damageEditText = view.findViewById(R.id.persistent_damage_dialog_value_editText);
        EditText dcEditText = view.findViewById(R.id.persistent_damage_dialog_dc_editText);
        EditText durationEditText = view.findViewById(R.id.persistent_damage_dialog_duration_editText);
        EditText descriptionEditText = view.findViewById(R.id.persistent_damage_dialog_description_editText);
        TextView warningTextView = view.findViewById(R.id.dialog_persistent_input_warning_textView);
        warningTextView.setVisibility(View.GONE);

        AlertDialog dialog = Dialogs.builder(context)
                .setTitle(R.string.persistent_dialog_title)
                .setView(view)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();

        view.findViewById(R.id.dialog_add_persistent_damage_button).setOnClickListener(v -> {
            String type = typeEditText.getText().toString().trim();
            if (type.isEmpty()) {
                warningTextView.setVisibility(View.VISIBLE);
                return;
            }
            String damage = damageEditText.getText().toString().trim();
            if (damage.isEmpty()) damage = context.getString(R.string.persistent_damage_hint);
            int dc = NumberInput.parseInt(dcEditText.getText().toString(), DebuffModel.DEFAULT_PERSISTENT_DC,
                    0, DebuffModel.MAX_DC);
            int duration = NumberInput.parseInt(durationEditText.getText().toString(), 0, 0,
                    DebuffModel.MAX_DURATION);
            String name = context.getString(R.string.persistent_name, type, damage);
            onCreated.accept(DebuffModel.persistentDamage(name, type, damage, dc, duration,
                    descriptionEditText.getText().toString()));
            dialog.dismiss();
        });
        dialog.show();
    }

    /**
     * End of turn: the character takes the damage (typed in when it is a dice expression), then
     * a successful flat check removes it.
     */
    public static void askEndOfTurn(Context context, Encounter encounter, CharacterModel character,
                                    DebuffModel debuff) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_persistent_damage_activate, null);
        TextView nameTextView = view.findViewById(R.id.persistent_damage_apply_name_textView);
        TextView valuesTextView = view.findViewById(R.id.persistent_damage_apply_values_textView);
        TextView descriptionTextView = view.findViewById(R.id.persistent_damage_apply_description_textView);
        EditText damageEditText = view.findViewById(R.id.persistent_damage_apply_overridden_editText);
        Button damageButton = view.findViewById(R.id.persistent_damage_take_damage_button);
        Button flatCheckButton = view.findViewById(R.id.persistent_damage_flat_check_button);

        nameTextView.setText(debuff.getName());
        valuesTextView.setText(context.getString(R.string.apply_persistent_values, debuff.getDamageType(),
                debuff.getDamageValue(), debuff.getDifficultyClass(), debuff.getDuration()));
        if (TextUtils.isEmpty(debuff.getDescription())) {
            descriptionTextView.setVisibility(View.GONE);
        } else {
            descriptionTextView.setText(debuff.getDescription());
        }
        // A fixed number is filled in; for dice the user types what they rolled
        if (debuff.getDamageValue().matches("\\d+")) {
            damageEditText.setText(debuff.getDamageValue());
        } else {
            damageEditText.setHint(debuff.getDamageValue());
        }

        AlertDialog dialog = Dialogs.builder(context)
                .setTitle(context.getString(R.string.persistent_damage_dialog_title, character.getCharacterName()))
                .setView(view)
                .setNegativeButton(R.string.dialog_close, null)
                .create();

        damageButton.setOnClickListener(v -> {
            String input = damageEditText.getText().toString().trim();
            if (!input.matches("\\d+")) {
                damageEditText.setError(context.getString(R.string.persistent_damage_must_override));
                return;
            }
            int damage = NumberInput.parseInt(input, 0, 0, CharacterModel.MAX_HP);
            damageButton.setVisibility(View.GONE);
            HealthDialogs.onHpChanged(context, encounter, character,
                    HpChange.apply(character, character.getHp() - damage));
        });
        flatCheckButton.setOnClickListener(v -> {
            character.getDebuffList().remove(debuff);
            flatCheckButton.setVisibility(View.GONE);
            encounter.notifyChanged();
            Toast.makeText(context, R.string.removed_debuff, Toast.LENGTH_LONG).show();
        });
        dialog.show();
    }
}
