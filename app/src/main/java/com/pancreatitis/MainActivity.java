package com.pancreatitis;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.pancreatitis.modules.database.DatabaseModule;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private ImageView backButton;
    private TextView toolbarTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DatabaseModule databaseModule = DatabaseModule.getInstance();
        databaseModule.setContext(getApplicationContext());
        databaseModule.initWork();

        // Убираем ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        setupNavigation();
        setupToolbar();
        setupBackPressedCallback();
    }

    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() != R.id.navigation_home &&
                        navController.getCurrentDestination().getId() != R.id.navigation_profile &&
                        navController.getCurrentDestination().getId() != R.id.navigation_helps) {
                    navController.navigateUp();
                } else {
                    finish();
                }
            }
        });
    }

    private void initViews() {
        backButton = findViewById(R.id.btn_back);
        toolbarTitle = findViewById(R.id.toolbar_title);
    }

    private void setupNavigation() {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);

            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();

                BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
                if (bottomNavigation != null) {
                    // Простая настройка без сложной логики
                    NavigationUI.setupWithNavController(bottomNavigation, navController);
                }

                // Упрощенный слушатель для тулбара
                navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                    updateToolbar(destination.getId());
                });
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Navigation setup error", e);
        }
    }

    private void setupToolbar() {
        // Проверяем что элементы не null перед использованием
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (navController != null) {
                    navController.navigateUp();
                }
            });
        }
    }

    private void updateToolbar(int destinationId) {
        if (backButton != null) {
            Set<Integer> hiddenBackButtonFragments = new HashSet<>(Arrays.asList(
                    R.id.navigation_home,
                    R.id.navigation_profile,
                    R.id.navigation_helps
            ));

            boolean showBackButton = !hiddenBackButtonFragments.contains(destinationId);
            backButton.setVisibility(showBackButton ? View.VISIBLE : View.GONE);
        }

        if (toolbarTitle != null) {
            String title = getFragmentTitle(destinationId);
            toolbarTitle.setText(title);
        }
    }

    private String getFragmentTitle(int destinationId) {
        if (destinationId == R.id.navigation_home) {
            return "Welcome";
        } else if (destinationId == R.id.navigation_profile) {
            return "Profile";
        } else if (destinationId == R.id.navigation_helps) {
            return "Helps";
        } else if (destinationId == R.id.navigation_questionnaires) {
            return "Questionnaire Management";
        } else if (destinationId == R.id.navigation_anket) {
            return "Questionnaire";
        } else {
            return "Another";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}