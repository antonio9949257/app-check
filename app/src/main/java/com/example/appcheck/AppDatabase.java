package com.example.appcheck;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Usuario.class, Materia.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    // Definimos el Executor para operaciones de escritura
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract UsuarioDao usuarioDao();
    public abstract MateriaDao materiaDao();

    public static AppDatabase getDatabase(final Context context) {
        // Eliminar esta línea en producción:
        // context.deleteDatabase("app_database");

        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "app_database")
                            .fallbackToDestructiveMigration()
                            .fallbackToDestructiveMigrationOnDowngrade()
                            //.allowMainThreadQueries() // Mejor no usarlo en producción
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}