package com.sbi.modelo;

public class Marcacion {
    public Long id;
    public Integer empleadoId;
    public String codigoBiometrico;
    public String empleado;
    public String dispositivo;
    public String fechaHora;   // formato yyyy-MM-dd HH:mm:ss
    public String tipo;        // ENTRADA / SALIDA
    public String metodo;      // HUELLA / ROSTRO / TARJETA / PASSWORD / OTRO
    public String origen;      // DISPOSITIVO / IMPORTACION / MANUAL
}
