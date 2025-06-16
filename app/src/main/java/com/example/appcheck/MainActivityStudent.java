package com.example.appcheck;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivityStudent extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_student);

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        // Load initial fragment (HomeFragment by default)
        if (savedInstanceState == null) {
            loadFragment(new EstudianteEscaneoFragment());
        }
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                int id = item.getItemId();

                if (id == R.id.nav_escaneo) {
                    selectedFragment = new EstudianteEscaneoFragment();
                } else if (id == R.id.nav_registro_estudiante) {
                    selectedFragment = new EstudianteHistorialFragment();
                } else if (id == R.id.nav_historial_estudiante) {
                    selectedFragment = new EstudianteRegistroFragment();
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                }

                return true;
            };

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_container_student, fragment)
                .commit();
    }
}