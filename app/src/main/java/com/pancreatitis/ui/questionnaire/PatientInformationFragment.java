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
            String patientName = args.getString("patientName", "");
            String diagnosis = args.getString("diagnosis", "");
            int questionnaireId = args.getInt("questionnaireId", 0);

            // Выводим через Toast
            String message = "Получены данные:\n" +
                    "Имя: " + patientName + "\n" +
                    "Диагноз: " + diagnosis + "\n" +
                    "ID: " + questionnaireId;

            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Данные не переданы", Toast.LENGTH_SHORT).show();
        }
    }

    private void initRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_characteristics);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Данные
        List<Characteristic> patientInfo = loadCharacteristicsAnket();
        List<Characteristic> clinicalInfo = loadCharacteristics();

        // Создаём адаптеры
        TitleAdapter title1 = new TitleAdapter("Patient Information");
        CharacteristicAdapter chars1 = new CharacteristicAdapter(patientInfo); // ваш исходный адаптер без заголовка

        TitleAdapter title2 = new TitleAdapter("Clinical Characteristics");
        CharacteristicAdapter chars2 = new CharacteristicAdapter(clinicalInfo);

        // Конкатенируем: title1, chars1, title2, chars2
        androidx.recyclerview.widget.ConcatAdapter concat =
                new androidx.recyclerview.widget.ConcatAdapter(title1, chars1, title2, chars2);

        recyclerView.setAdapter(concat);
    }

    private List<Characteristic> loadCharacteristicsAnket() {
        List<Characteristic> list = new ArrayList<>();

        list.add(new Characteristic("Full Name", "...", android.text.InputType.TYPE_CLASS_TEXT));
        list.add(new Characteristic("Date of completion", "...", android.text.InputType.TYPE_CLASS_TEXT));
        list.add(new Characteristic("Admitted from", "...", android.text.InputType.TYPE_CLASS_TEXT));

        return list;
    }

    private List<Characteristic> loadCharacteristics() {
        List<Characteristic> list = new ArrayList<>();

        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String query = "select name from characteristic";
        Cursor cursor = database.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                list.add(new Characteristic(name, "...", android.text.InputType.TYPE_CLASS_TEXT));
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.close();

        return list;
    }

}