package com.pancreatitis.ui.characteristic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.R;
import com.pancreatitis.databinding.FragmentHelpsBinding;
import com.pancreatitis.databinding.FragmentProfileBinding;
import com.pancreatitis.models.Characteristic;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.database.DatabaseModule;

public class CharacteristicFragment extends Fragment {
    private FragmentHelpsBinding binding;
    private RecyclerView recyclerView;
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
            nameCharacteristic = view.findViewById(R.id.name_characteristic);
            loadData();
        } catch (Exception e) {
            showError(e.toString());
        }
    }
    private void loadData() {
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        Characteristic characteristic = databaseModule.getCharacteristicById(idCharacteristic);
        nameCharacteristic.setText(characteristic.getOpis());
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