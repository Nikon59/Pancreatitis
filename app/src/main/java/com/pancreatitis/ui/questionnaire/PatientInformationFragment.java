package com.pancreatitis.ui.questionnaire;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.adapters.CharacteristicAdapter;
import com.pancreatitis.models.Characteristic;
import com.pancreatitis.R;

import java.util.ArrayList;
import java.util.List;

public class PatientInformationFragment extends Fragment {

    private RecyclerView recyclerView;
    private CharacteristicAdapter adapter;
    private List<Characteristic> characteristics = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questionnaire, container, false);

        receiveArguments();

        initRecyclerView(view);
        loadCharacteristics();

        return view;
    }

    private void receiveArguments() {
        Bundle args = getArguments();
        if (args != null) {
            String patientName = args.getString("patientName", "");
            String diagnosis = args.getString("diagnosis", "");
            int questionnaireId = args.getInt("questionnaireId", 0);

            // Выводим через Toast
            String message = "Получены данные:\n" +
                    "Имя: " + patientName + "\n" +
                    "Диагноз: " + diagnosis + "\n" +
                    "ID: " + questionnaireId;

            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Данные не переданы", Toast.LENGTH_SHORT).show();
        }
    }

    private void initRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_characteristics);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CharacteristicAdapter(characteristics);
        recyclerView.setAdapter(adapter);
    }

    private void loadCharacteristics() {
        // Заглушка с примерами характеристик
        characteristics.add(new Characteristic("Full Name", "", android.text.InputType.TYPE_CLASS_TEXT));
        characteristics.add(new Characteristic("Age", "", android.text.InputType.TYPE_CLASS_NUMBER));
        characteristics.add(new Characteristic("Gender", "", android.text.InputType.TYPE_CLASS_TEXT));
        characteristics.add(new Characteristic("Phone", "", android.text.InputType.TYPE_CLASS_PHONE));
        characteristics.add(new Characteristic("Email", "", android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS));
        characteristics.add(new Characteristic("Address", "", android.text.InputType.TYPE_CLASS_TEXT));
        characteristics.add(new Characteristic("Medical History", "", android.text.InputType.TYPE_CLASS_TEXT));

        adapter.notifyDataSetChanged();
    }

    // Метод для получения заполненных данных
    private List<Characteristic> getPatientData() {
        return adapter.getCharacteristics();
    }

    // Метод для обновления данных
    public void updateCharacteristics(List<Characteristic> newCharacteristics) {
        adapter.updateData(newCharacteristics);
    }
}