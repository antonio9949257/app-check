package com.example.appcheck;

public class Estudiante {
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String matricula;
    private String cedulaIdentidad;
    private String celular;
    private String correoElectronico;
    private String codigoAsistencia;

    public Estudiante(String nombres, String apellidoPaterno, String apellidoMaterno, String matricula, String cedulaIdentidad, String celular, String correo) {
    }

    // Constructor
    public String getNombreCompleto() {
        return nombres + " " + apellidoPaterno + " " + apellidoMaterno;
    }

    public String getNombres() {
        return nombres;
    }
    // Getters y setters

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {

    }

    public String getApellidoMaterno() {
        return apellidoMaterno;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;

    }

    public String getCedulaIdentidad() {
        return cedulaIdentidad;
    }

    public String getCelular() {
        return celular;
    }

    public String getCodigoAsistencia() {
        return codigoAsistencia;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setApellidoMaterno(String apellidoMaterno) {
        this.apellidoMaterno = apellidoMaterno;
    }

    public void setApellidoPaterno(String apellidoPaterno) {
        this.apellidoPaterno = apellidoPaterno;
    }

    public void setCedulaIdentidad(String cedulaIdentidad) {
        this.cedulaIdentidad = cedulaIdentidad;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public void setCodigoAsistencia(String codigoAsistencia) {
        this.codigoAsistencia = codigoAsistencia;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }
}