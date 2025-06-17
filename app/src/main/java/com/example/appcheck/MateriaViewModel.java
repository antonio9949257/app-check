package com.example.appcheck;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import org.jspecify.annotations.NonNull;

import java.util.List;

public class MateriaViewModel extends AndroidViewModel {
    private MateriaDao materiaDao;
    private LiveData<List<Materia>> allMaterias;

    public MateriaViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        materiaDao = db.materiaDao();
        allMaterias = materiaDao.getAllMaterias();
    }

    public LiveData<List<Materia>> getAllMaterias() {
        return allMaterias;
    }

    public void insert(Materia materia) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            materiaDao.insert(materia);
        });
    }
}