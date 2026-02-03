package com.pancreatitis.ui.characteristic;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.R;
import com.pancreatitis.adapters.CharacteristicItemAdapter;
import com.pancreatitis.databinding.FragmentCharacteristicBinding;
import com.pancreatitis.models.Characteristic;
import com.pancreatitis.models.CharacteristicItem;
import com.pancreatitis.models.CharacteristicItemNonNumeric;
import com.pancreatitis.models.CharacteristicItemNumeric;
import com.pancreatitis.models.CharacterizationAnketPatient;
import com.pancreatitis.models.CharacterizationValue;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.database.DatabaseModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CharacteristicFragment extends Fragment {
    private FragmentCharacteristicBinding binding;
    private Characteristic characteristic;
    private RecyclerView recyclerView;
    private ImageView btnHelp;
    private CharacteristicItemAdapter adapter;
    private TextView nameCharacteristic;
    private int idQuestionnaire, idCharacteristic;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_characteristic, container, false);
        receiveArguments();
        initViews(view);
        return view;
    }
    private void receiveArguments() {
        Bundle args = getArguments();
        idQuestionnaire = args.getInt("idQuestionnaire", -1);
        idCharacteristic = args.getInt("idCharacteristic", -1);
    }
    private void initViews(View view) {
        try {
            recyclerView = view.findViewById(R.id.recycler_view_value_of_characteristic);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            btnHelp = view.findViewById(R.id.get_helps);
            nameCharacteristic = view.findViewById(R.id.name_characteristic);
            setupHelpButton();
            loadData();
        } catch (Exception e) {
            showError(e.toString());
        }
    }
    private void setupHelpButton() {
        String dialogMessage = buildDialogMessage();
        btnHelp.setOnClickListener(v -> showHelpDialog(dialogMessage));
    }

    private String buildDialogMessage() {
        StringBuilder message = new StringBuilder();
        DatabaseModule databaseModule = DatabaseModule.getInstance();

        Characteristic characteristic = databaseModule.getCharacteristicById(idCharacteristic);
        String hints = characteristic.getHints();
        if (hints != null && !hints.trim().isEmpty()) {
            message.append(hints);
        }

        // Показываем доступные опции
        if (characteristic.getIdType() == 1 || characteristic.getIdType() == 2) {
            List<CharacterizationValue> characterizationValues = databaseModule.getValuesForCharacteristic(idCharacteristic);
            List<String> options = characterizationValues.stream()
                    .map(CharacterizationValue::getValue)
                    .collect(Collectors.toList());
            if (options != null && !options.isEmpty()) {
                message.append("\n\nДоступные варианты:\n• Нет данных");
                for (String option : options) {
                    if (!option.isEmpty()) {
                        message.append("\n• ").append(option);
                    }
                }
            }
        }
        return message.toString();
    }

    private void showHelpDialog(String message) {
        new AlertDialog.Builder(getContext())
                .setTitle("Справка")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
    private void loadData() {
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        Characteristic characteristic = databaseModule.getCharacteristicById(idCharacteristic);
        nameCharacteristic.setText(characteristic.getOpis());
        List<CharacteristicItem> characteristicItems = loadCharacteristicItems(characteristic);
        adapter = new CharacteristicItemAdapter(characteristicItems);
        recyclerView.setAdapter(adapter);
    }
    private List<CharacteristicItem> loadCharacteristicItems(Characteristic characteristic){
        List<CharacteristicItem> characteristicItems = new ArrayList<>();
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        List<CharacterizationAnketPatient> characterizationAnketPatients = databaseModule.getCharacterizationsForAnket(idQuestionnaire);

        for (CharacterizationAnketPatient characterizationAnketPatient: characterizationAnketPatients) {
            if (characterizationAnketPatient.getIdCharacteristic() == characteristic.getId()) {
                if (characteristic.getIdType() == 1) {
                    List<CharacterizationValue> characterizationValues = databaseModule.getValuesForCharacteristic(characteristic.getId());
                    List<String> options = new ArrayList<>(Arrays.asList("Нет данных"));
                    options.addAll(characterizationValues.stream()
                            .map(CharacterizationValue::getValue)
                            .collect(Collectors.toList()));

                    CharacteristicItemNonNumeric characteristicItem = new CharacteristicItemNonNumeric();
                    characteristicItem.setIdCharacteristic(characteristic.getId());
                    characteristicItem.setIdType(characteristic.getIdType());
                    characteristicItem.setName(characteristic.getOpis());
                    characteristicItem.setHint(characteristic.getHints());
                    characteristicItem.setOptions(options);

                    characteristicItems.add(characteristicItem);
                } else if (characteristic.getIdType() == 2) {
                    List<CharacterizationValue> characterizationValues = databaseModule.getValuesForCharacteristic(characteristic.getId());
                    List<String> options = new ArrayList<>(Arrays.asList("Нет данных"));
                    options.addAll(characterizationValues.stream()
                            .map(CharacterizationValue::getValue)
                            .collect(Collectors.toList()));

                    CharacteristicItemNonNumeric characteristicItem = new CharacteristicItemNonNumeric();
                    characteristicItem.setIdCharacteristic(characteristic.getId());
                    characteristicItem.setIdType(characteristic.getIdType());
                    characteristicItem.setName(characteristic.getOpis());
                    characteristicItem.setHint(characteristic.getHints());
                    characteristicItem.setOptions(options);

                    characteristicItems.add(characteristicItem);
                } else if (characteristic.getIdType() == 3) {
                    CharacteristicItemNumeric characteristicItem = new CharacteristicItemNumeric();
                    characteristicItem.setIdCharacteristic(characteristic.getId());
                    characteristicItem.setIdType(characteristic.getIdType());
                    characteristicItem.setName(characteristic.getOpis());
                    characteristicItem.setHint(characteristic.getHints());

                    characteristicItems.add(characteristicItem);
                }
            }
        }
        return characteristicItems;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    private void showMessage(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void showError(String error) {
        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
    }
}