package com.example.appcheck;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MateriaDao {
    @Insert
    void insert(Materia materia);

    @Update
    void update(Materia materia);

    @Delete
    void delete(Materia materia);

    @Query("DELETE FROM materias")
    void deleteAllMaterias();

    @Query("SELECT * FROM materias ORDER BY nombre ASC")
    LiveData<List<Materia>> getAllMaterias();


}