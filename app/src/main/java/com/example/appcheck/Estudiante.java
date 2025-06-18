package com.example.appcheck;

public class Estudiante {
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String matricula;
    private String cedulaIdentidad;
    private String celular;
    private String correo;
    private String horaRegistro; // Opcional: puedes generarla al crear el objeto

    public Estudiante(String nombres, String apellidoPaterno, String apellidoMaterno,
                      String matricula, String cedulaIdentidad,
                      String celular, String correo) {
        this.nombres = nombres;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.matricula = matricula;
        this.cedulaIdentidad = cedulaIdentidad;
        this.celular = celular;
        this.correo = correo;
        this.horaRegistro = android.text.format.DateFormat.format("HH:mm", new java.util.Date()).toString();
    }

    // Métodos getter
    public String getNombres() {
        return nombres;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;
    }

    public String getApellidoMaterno() {
        return apellidoMaterno;
    }

    public String getMatricula() {
        return matricula;
    }

    public String getCedulaIdentidad() {
        return cedulaIdentidad;
    }

    public String getCelular() {
        return celular;
    }

    public String getCorreo() {
        return correo;
    }

    public String getHoraRegistro() {
        return horaRegistro;
    }

    // Métodos útiles para la UI
    public String getNombreCompleto() {
        return nombres + " " + apellidoPaterno +
                (apellidoMaterno != null && !apellidoMaterno.isEmpty() ? " " + apellidoMaterno : "");
    }

    // Métodos setter si necesitas modificar los datos
    public void setHoraRegistro(String horaRegistro) {
        this.horaRegistro = horaRegistro;
    }

    // Opcional: método toString() para logging
    @Override
    public String toString() {
        return "Estudiante{" +
                "nombre='" + getNombreCompleto() + '\'' +
                ", matricula='" + matricula + '\'' +
                ", hora='" + horaRegistro + '\'' +
                '}';
    }
}