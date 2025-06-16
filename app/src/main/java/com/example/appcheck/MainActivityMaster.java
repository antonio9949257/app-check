package com.example.appcheck;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivityMaster extends AppCompatActivity {


    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_master);

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        // Load initial fragment (HomeFragment by default)
        if (savedInstanceState == null) {
            loadFragment(new DocenteAsistenciaFragment());
        }
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                int id = item.getItemId();

                if (id == R.id.nav_asistencia) {
                    selectedFragment = new DocenteAsistenciaFragment();
                } else if (id == R.id.nav_registro) {
                    selectedFragment = new DocenteRegistroFragment();
                } else if (id == R.id.nav_historial) {
                    selectedFragment = new DocenteHistorialFragment();
                } else if (id == R.id.nav_Materia) {
                    selectedFragment = new DocenteMateriaFragment();
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                }

                return true;
            };

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_container_master, fragment)
                .commit();
    }
}
