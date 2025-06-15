package com.example.appcheck;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UsuarioDao {
    @Insert
    void insert(Usuario usuario);

    @Query("SELECT * FROM usuarios")
    List<Usuario> getAllUsuarios();

    @Query("SELECT * FROM usuarios WHERE cedulaIdentidad = :cedula")
    Usuario getUsuarioByCedula(String cedula);

    @Query("SELECT * FROM usuarios WHERE androidId = :androidId LIMIT 1")
    Usuario getUsuarioPorAndroidId(String androidId);
}