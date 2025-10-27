package com.pancreatitis.ui.questionnaire;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.DatabaseHelper;
import com.pancreatitis.adapters.CharacteristicAdapter;
import com.pancreatitis.adapters.TitleAdapter;
import com.pancreatitis.models.Characteristic;
import com.pancreatitis.R;
import com.pancreatitis.models.QuestionnaireItem;

import java.util.ArrayList;
import java.util.List;

public class PatientInformationFragment extends Fragment {

    private RecyclerView recyclerView;
    private int questionnaireId = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questionnaire, container, false);

        receiveArguments();
        initRecyclerView(view);

        return view;
    }

    private void receiveArguments() {
        Bundle args = getArguments();
        if (args != null) {
            questionnaireId = args.getInt("questionnaireId");
            Toast.makeText(getContext(), String.format("%d", questionnaireId), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Данные не переданы", Toast.LENGTH_SHORT).show();
        }
    }

    private void initRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_characteristics);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Загружаем данные с учетом questionnaireId
        loadAndDisplayData();
    }

    private void loadAndDisplayData() {
        // Загружаем данные из БД по questionnaireId
        List<Characteristic> patientInfo = loadPatientInfoFromDB();
        List<Characteristic> clinicalInfo = loadClinicalInfoFromDB();
        List<Characteristic> diagnosisInfo = loadDiagnosisInfoFromDB();

        // Создаём адаптеры с реальными данными
        TitleAdapter title1 = new TitleAdapter("Patient Information");
        CharacteristicAdapter chars1 = new CharacteristicAdapter(patientInfo);

        TitleAdapter title2 = new TitleAdapter("Clinical Characteristics");
        CharacteristicAdapter chars2 = new CharacteristicAdapter(clinicalInfo);

        TitleAdapter title3 = new TitleAdapter("Predict Diagnosis");
        CharacteristicAdapter chars3 = new CharacteristicAdapter(diagnosisInfo);

        androidx.recyclerview.widget.ConcatAdapter concat =
                new androidx.recyclerview.widget.ConcatAdapter(title1, chars1, title2, chars2, title3, chars3);

        recyclerView.setAdapter(concat);
    }

    private List<Characteristic> loadPatientInfoFromDB() {
        List<Characteristic> list = new ArrayList<>();

        if (questionnaireId != -1) {
            // Заглушка - в реальности делайте запрос к БД
            DatabaseHelper dbHelper = new DatabaseHelper(getContext());
            SQLiteDatabase database = dbHelper.getReadableDatabase();

            // Пример запроса - адаптируйте под вашу структуру БД
            String query = String.format("select patients.fio, ankets.date_of_completion, ankets.admitted_from from ankets join patients on ankets.id_patient = patients.id where ankets.id = %d", questionnaireId);
            Cursor cursor = database.rawQuery(query, null);
            cursor.moveToFirst();
            String fullName = cursor.getString(0);
            String completionDate = cursor.getString(1);
            String admittedFrom = cursor.getString(2);

            list.add(new Characteristic("Full Name", fullName, android.text.InputType.TYPE_CLASS_TEXT));
            list.add(new Characteristic("Date of completion", completionDate, android.text.InputType.TYPE_CLASS_TEXT));
            list.add(new Characteristic("Admitted from", admittedFrom, android.text.InputType.TYPE_CLASS_TEXT));

            cursor.close();
            database.close();
        } else {
            // Если данных нет, используем значения по умолчанию
            list.add(new Characteristic("Full Name", "Не указано", android.text.InputType.TYPE_CLASS_TEXT));
            list.add(new Characteristic("Date of completion", "Не указано", android.text.InputType.TYPE_CLASS_TEXT));
            list.add(new Characteristic("Admitted from", "Не указано", android.text.InputType.TYPE_CLASS_TEXT));
        }
        return list;
    }

    private List<Characteristic> loadClinicalInfoFromDB() {
        List<Characteristic> list = new ArrayList<>();

        // Запрос для получения клинических характеристик по questionnaireId
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        if (questionnaireId != -1) {
            String query = "select characteristic.name, characterization_anket_patient.value from ankets join characterization_anket_patient on characterization_anket_patient.id_anket = ankets.id join characteristic on characterization_anket_patient.id_characteristic = characteristic.id where ankets.id = ?";
            Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(questionnaireId)});
            cursor.moveToFirst();
            do {
                String name = cursor.getString(0);
                String value = cursor.getString(1);
                list.add(new Characteristic(name, value != null ? value : "...",
                        android.text.InputType.TYPE_CLASS_TEXT));
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            // Если нет данных, загружаем базовый список характеристик
            String baseQuery = "SELECT name FROM characteristic";
            Cursor baseCursor = database.rawQuery(baseQuery, null);
            if (baseCursor.moveToFirst()) {
                do {
                    String name = baseCursor.getString(0);
                    list.add(new Characteristic(name, "...", android.text.InputType.TYPE_CLASS_TEXT));
                } while (baseCursor.moveToNext());
            }
            baseCursor.close();
        }

        database.close();
        return list;
    }

    private List<Characteristic> loadDiagnosisInfoFromDB() {
        List<Characteristic> list = new ArrayList<>();

        // Запрос для получения диагноза
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        if (questionnaireId != -1) {
            String query = "SELECT diagnosis FROM ankets WHERE id = ?";
            Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(questionnaireId)});
            cursor.moveToFirst();
            String diagnosis = cursor.getString(0);

            list.add(new Characteristic("Diagnosis", diagnosis != null ? diagnosis : "Не установлен",
                    android.text.InputType.TYPE_CLASS_TEXT));

            cursor.close();
            database.close();
        } else {
            list.add(new Characteristic("Diagnosis", "Не установлен", android.text.InputType.TYPE_CLASS_TEXT));
        }

        return list;
    }
}