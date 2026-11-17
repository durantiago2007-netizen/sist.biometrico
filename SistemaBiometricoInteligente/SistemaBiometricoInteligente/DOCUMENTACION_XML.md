# Archivos XML del biométrico Dahua (pendrive)

Equipo: **Dahua DHI-ASA3223A-W** (SN AE06085PAJ13AAD). Al exportar al pendrive genera **hojas de Excel 2003 guardadas como XML**
(formato *SpreadsheetML*: `<Workbook>` → `<Worksheet>` → `<Table>` → `<Row>` → `<Cell>` → `<Data>`). No son XML "de datos" con etiquetas
propias: cada archivo es una tabla con filas y columnas, igual que una planilla.

## Los 4 tipos de archivo

| Archivo | Hoja | Qué contiene | ¿Lo importa el programa? |
|---|---|---|---|
| `Card_Punch_Records_N.xml` | Registros de asistencia | **Cada marcación**: quién, cuándo, cómo, si fue correcta, ingreso o salida | **Sí, es el principal** → tabla `marcaciones` |
| `Attendance_Shift.xml` | Turno de trabajo | Turnos: hora de ingreso/salida, tolerancia de llegada tarde | **Sí** → tabla `horarios` |
| `Statistics_Report_MM.xml` | Informe de asistencia | Resumen del mes por persona (horas, tardanzas, ausencias, salario) | No: lo calcula el equipo; el sistema lo recalcula |
| `Time_Card1_MM.xml` | Tabla de Tarjetas de Asistencia | Grilla por persona y día del mes (MMDD) con ingreso/salida | No: es la misma información que las marcaciones |

## Card_Punch_Records (marcaciones) — columnas

| Columna | Tipo de dato | Ejemplo | Qué hace el programa |
|---|---|---|---|
| No. | entero | `1` | Se ignora (número de fila) |
| **Núm.** | texto/número (ID de usuario del equipo) | `69` | Busca al empleado por `codigo_biometrico`; si no existe lo crea |
| **Nombre** | texto | `Vargas Josias Uriel Reinaldo` | Solo para crear empleados nuevos (1ª palabra = apellido) |
| **Tiempo registrado** | fecha y hora `yyyy-MM-dd HH:mm:ss` | `2026-10-09 05:24:07` | `marcaciones.fecha_hora` (aplicando el ajuste de hora) |
| **Modo de reconocimiento** | texto: `Cara`, `Huella`, `Tarjeta`, `Contraseña` | `Cara` | `marcaciones.metodo` → ROSTRO / HUELLA / TARJETA / PASSWORD |
| **Estado** | texto: `Se ha realizado correctamente` / `Con errores` | | Los intentos **con errores** (cara no reconocida, clave mal) se cuentan y se ignoran |
| **Estado de Asistencia** | texto: `Ingreso` / `Salida` | `Ingreso` | `marcaciones.tipo` → ENTRADA / SALIDA |
| Mascarilla facial | texto (vacío) | | Se ignora |

Encabezado del archivo: fila 1 título, fila 2 rango de fechas exportado (`Hora 2026-10-01 … Hora final 2026-11-03`), fila 3 nombres de columnas.

Archivo de muestra (`Card_Punch_Records_0.xml`): 91 filas → **74 marcaciones válidas** de 11 personas y **17 intentos fallidos**;
83 por rostro y 8 por contraseña.

## Attendance_Shift (turnos) — columnas
`No. de turno`, `Datos del Turno` (nombre), `Periodo 1 Ingreso/Salida`, `Periodo 2 Ingreso/Salida`, `Tiempo extra Ingreso/Salida`,
`Llegada tardía` (min de tolerancia), `Salida temprana` (min). Ej.: **Turno 1 "PRIMERO CB" 07:30–13:00, tolerancia 5 min**.
Los turnos llamados `Predet.` (08:00–17:00) son el valor por defecto del equipo; se guarda uno solo.

## ⚠️ Reloj del equipo adelantado 11 horas
La misma marcación aparece en el XML como `2026-10-09 05:24:07` y en SmartPSS como `2026-10-08 18:24:07`.
El biométrico tiene mal configurada la zona horaria. Por eso la pantalla **Importar Dahua** trae **Ajuste de hora = -11**.
Lo ideal es corregir la hora/zona horaria en el equipo (Menú → Sistema → Hora) y después usar ajuste **0**.

## Qué guarda el programa
- **empleados**: código biométrico, documento, nombres, apellidos, área, horario.
- **marcaciones**: empleado, fecha y hora, ENTRADA/SALIDA, método, equipo, origen (IMPORTACION / MANUAL / DISPOSITIVO). Nunca se repiten.
- **horarios**: turnos importados del equipo o cargados a mano.
- **importaciones**: historial de cada archivo cargado (fecha, nombre, nuevas, duplicadas, errores).
- **vista_asistencia_diaria**: primera entrada y última salida de cada día con estado PUNTUAL / TARDANZA según el horario asignado.

## Código
- `servicio/ImportadorDahuaXML.java`: detecta el tipo de archivo y lo procesa. También acepta XML genérico con etiquetas
  `UserID`, `CreateTime`, `Type`, `Method` (ver `database/ejemplo_dahua.xml`).
- Seguridad: el lector bloquea `DOCTYPE` y entidades externas (ataques XXE).
