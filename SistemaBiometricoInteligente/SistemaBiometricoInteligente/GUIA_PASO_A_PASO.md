# Guía paso a paso — Sistema Biométrico Inteligente

*Versión para **Apache NetBeans IDE 17***

Herramientas: **MySQL 8** · **HeidiSQL** · **Apache NetBeans IDE 17** · **Claude Code** (opcional, para seguir desarrollando).

---

## PASO 0. Instalar lo necesario

| Programa | Dónde | Nota |
|---|---|---|
| JDK 17 (LTS) | adoptium.net (Temurin 17) | Usa la **17**: NetBeans 17 está pensado para JDK 11–19 |
| MySQL Server 8 | dev.mysql.com/downloads/installer | Durante la instalación anota la contraseña de `root` |
| HeidiSQL | heidisql.com | Cliente gráfico para MySQL |
| Apache NetBeans IDE 17 | archive.apache.org/dist/netbeans/netbeans-installers/17/ | Ya trae Maven integrado. Durante la instalación elige el JDK 17 |

Comprueba que el servicio **MySQL80** esté iniciado (Windows: `Win + R` → `services.msc`).

---

## PASO 1. Crear la base de datos en HeidiSQL

1. Abre HeidiSQL → **Nuevo** (abajo a la izquierda).
2. Configura la sesión:
   - Tipo de red: **MySQL (TCP/IP)**
   - Host: `127.0.0.1` · Usuario: `root` · Contraseña: la tuya · Puerto: `3306`
3. Pulsa **Abrir**.
4. Menú **Archivo → Cargar archivo SQL…** y elige `database/sistema_biometrico_inteligente.sql`.
5. Pulsa **Ejecutar (F9)**.
6. Pulsa **F5** (refrescar). Debe aparecer `sistema_biometrico_inteligente` con estas tablas:
   `areas, horarios, empleados, dispositivos, marcaciones, justificaciones, usuarios`,
   la vista `vista_asistencia_diaria` y los procedimientos `sp_registrar_marcacion` y `sp_reporte_asistencia`.
7. Prueba en la pestaña **Consulta**:
   ```sql
   USE sistema_biometrico_inteligente;
   SELECT * FROM vista_asistencia_diaria;
   ```
   Verás 3 empleados de prueba con estado PUNTUAL / TARDANZA.

> Nota: si la base ya existe, para agregar el historial de importaciones ejecuta solo `database/migracion_importaciones.sql`.

> ⚠️ El script empieza con `DROP DATABASE IF EXISTS`. Si ya tienes datos reales, **no** lo vuelvas a ejecutar.

### Qué guarda cada tabla
- **empleados** → `codigo_biometrico` es el **User ID** que tiene la persona en el equipo Dahua. Deben coincidir.
- **dispositivos** → cada equipo Dahua (número de serie, IP).
- **marcaciones** → cada huella/rostro/tarjeta registrada. No se repite (empleado + fecha_hora es único).
- **horarios** → hora de entrada y minutos de tolerancia para calcular tardanzas.
- **usuarios** → acceso a la web (por defecto `admin` / `admin123`).

---

## PASO 2. Abrir el proyecto en NetBeans 17

### 2.1 Verificar el JDK en NetBeans 17
1. Menú **Tools → Java Platforms**. Debe aparecer **JDK 17**. Si no está: **Add Platform… → Java Standard Edition** → busca la carpeta del JDK (ej. `C:\Program Files\Eclipse Adoptium\jdk-17...`) → **Finish**.
2. Si NetBeans 17 se abrió con otro JDK, edita `C:\Program Files\NetBeans-17\netbeans\etc\netbeans.conf` y cambia la línea `netbeans_jdkhome="..."` por la ruta de tu JDK 17. Reinicia NetBeans.

### 2.2 Abrir el proyecto
1. Descomprime `SistemaBiometricoInteligente.zip` (por ejemplo en `C:\Proyectos\`).
2. NetBeans → **File → Open Project…** → selecciona la carpeta `SistemaBiometricoInteligente` (tiene el ícono de Maven).
3. La primera vez NetBeans descarga las dependencias (driver MySQL y Gson). Espera a que termine la barra inferior. Si sale en rojo: clic derecho al proyecto → **Build with Dependencies**.
4. Clic derecho al proyecto → **Properties**:
    - **Sources** → *Source/Binary Format*: **17**
    - **Build → Compile** → *Java Platform*: **JDK 17**
    - **Run** → *Main Class*: `com.sbi.Main` (ya viene configurado en el `pom.xml`; si NetBeans lo pide, pulsa **Select Main Class** y elige `com.sbi.Main`)
5. En la ventana **Projects** verás: *Source Packages* (código Java), *Other Sources* (`db.properties` y la web), *Dependencies* y *Project Files* (`pom.xml`).

### Estructura
```
SistemaBiometricoInteligente/
├── pom.xml                         ← dependencias Maven
├── database/
│   ├── sistema_biometrico_inteligente.sql
│   └── ejemplo_exportacion_dahua.csv
└── src/main/
    ├── java/com/sbi/
    │   ├── Main.java               ← ejecuta todo
    │   ├── config/  Configuracion, Conexion
    │   ├── modelo/  Empleado, Marcacion, Dispositivo, AsistenciaDiaria, Usuario
    │   ├── dao/     un DAO por tabla (EmpleadoDAO, MarcacionDAO...)
    │   ├── servicio/ImportadorDahuaCSV
    │   └── api/     ServidorApi    ← rutas REST + sirve la web
    └── resources/
        ├── db.properties           ← usuario/contraseña de MySQL
        └── web/  index.html, app.js, styles.css
```

---

## PASO 3. Configurar la conexión

Abre `src/main/resources/db.properties` (en NetBeans: *Other Sources*) y pon tu contraseña:

```properties
db.url=jdbc:mysql://localhost:3306/sistema_biometrico_inteligente?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
db.usuario=root
db.password=TU_CONTRASEÑA
servidor.puerto=8080
dahua.apiKey=CAMBIAR-ESTA-CLAVE
```

Prueba la conexión: abre `config/Conexion.java` → clic derecho → **Run File (Shift + F6)**.
Debe decir: `Conexión exitosa a: sistema_biometrico_inteligente`.

---

## PASO 4. Ejecutar el sistema

1. Clic derecho al proyecto → **Run (F6)**.
2. En la consola de NetBeans aparece:
   ```
   [OK] Conectado a MySQL: sistema_biometrico_inteligente
   [OK] Sistema Biométrico Inteligente en http://localhost:8080
   ```
3. Abre el navegador en **http://localhost:8080** → usuario `admin`, contraseña `admin123`.

Pestañas de la web:
- **Panel**: empleados, presentes, tardanzas, ausentes del día + marcación manual.
- **Asistencia**: reporte por rango de fechas (entrada, salida, PUNTUAL/TARDANZA).
- **Marcaciones**: todos los registros crudos.
- **Empleados**: alta, edición y baja.
- **Importar Dahua**: sube el CSV exportado del biométrico.

Para detener: botón cuadrado rojo en la consola de NetBeans.

---

## PASO 5. Registrar al personal en el Dahua

1. En el equipo Dahua (o en **SmartPSS / SmartPSS Lite**) crea a cada persona con un **User ID** (ej. `1001`) y registra su huella/rostro/tarjeta.
2. En la web → **Empleados**, crea a la misma persona con ese mismo código en *Código biométrico*.
3. Registra el equipo en la tabla `dispositivos` con su **número de serie** (está en la etiqueta o en *Sistema → Información* del equipo). En HeidiSQL:
   ```sql
   INSERT INTO dispositivos (nombre, modelo, numero_serie, ip, ubicacion)
   VALUES ('Puerta 2', 'ASI1212F', 'TU_NUMERO_DE_SERIE', '192.168.1.109', 'Segundo piso');
   ```

---

## PASO 6. Pasar las marcaciones del Dahua a la base de datos

### Opción A — Archivo XML del pendrive (la forma actual)
1. Copia al pendrive los archivos que exporta el equipo y conéctalo a la PC.
2. Si la base ya existía antes de esta versión, ejecuta una vez `database/migracion_importaciones.sql` (crea el historial de importaciones).
3. En la web → **Importar Dahua** → elige **Card_Punch_Records_X.xml** → deja **Ajuste de hora = -11** → **Importar**.
4. Opcional: importa **Attendance_Shift.xml** para cargar los turnos como horarios.
5. Los reportes *Statistics_Report* y *Time_Card* no se importan (son resúmenes del equipo).

El reloj del biométrico está 11 h adelantado; si lo corrigen, usa ajuste **0**. Detalle de los datos del XML: `DOCUMENTACION_XML.md`.

### Opción A2 — Importar archivo CSV
1. En el equipo: **Registros / Records → Exportar** a USB, o en SmartPSS: **Attendance → Export**.
2. Si sale en `.xlsx`, ábrelo en Excel → **Guardar como → CSV (delimitado por comas)**.
3. Web → **Importar Dahua** → elige el archivo, escribe el número de serie del equipo → **Importar**.
4. Resultado: `importadas`, `duplicadas` (ya existían) y `errores` (ej. User ID no registrado en *Empleados*).

El importador reconoce columnas como `User ID / Person ID / ID / No.` y `Time / Date Time / Fecha`, separadores `,` `;` o tabulación, y fechas `yyyy-MM-dd HH:mm:ss`, `yyyy/MM/dd HH:mm:ss` o `dd/MM/yyyy HH:mm`. Prueba con `database/ejemplo_exportacion_dahua.csv`.

### Opción B — Tiempo real (cuando el equipo o un script envía cada marcación)
Endpoint: `POST http://IP_DE_TU_PC:8080/api/dahua/evento`
Cabecera: `X-Api-Key: CAMBIAR-ESTA-CLAVE`
Cuerpo JSON:
```json
{ "codigoBiometrico": "1001", "numeroSerie": "DAHUA-SN-0001", "fechaHora": "2026-10-28 08:01:00", "metodo": "HUELLA" }
```
Prueba desde CMD:
```bat
curl -X POST http://localhost:8080/api/dahua/evento -H "X-Api-Key: CAMBIAR-ESTA-CLAVE" -H "Content-Type: application/json" -d "{\"codigoBiometrico\":\"1002\",\"numeroSerie\":\"DAHUA-SN-0001\",\"metodo\":\"HUELLA\"}"
```
Respuesta: `{"resultado":"OK: SALIDA"}` (o ENTRADA si es la primera del día).

> Para que el equipo llegue a tu PC: misma red, IP fija en la PC y abrir el puerto 8080 en el Firewall de Windows.

---

## PASO 7. API REST (para la interfaz web del equipo)

Primero `POST /api/login` con `{"usuario":"admin","password":"admin123"}` → devuelve `token`.
Las demás rutas llevan la cabecera `Authorization: Bearer <token>`.

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/resumen` | Totales del día |
| GET/POST | `/api/empleados` | Listar / crear |
| GET/PUT/DELETE | `/api/empleados/{id}` | Ver / editar / dar de baja |
| GET/POST | `/api/dispositivos` | Listar / registrar equipos |
| GET | `/api/marcaciones?desde=2026-10-01&hasta=2026-10-31` | Marcaciones |
| POST | `/api/marcaciones` | Marcación manual |
| GET | `/api/asistencia?desde=...&hasta=...` | Reporte diario |
| POST | `/api/importar?serie=DAHUA-SN-0001` | Cuerpo = texto del CSV |
| POST | `/api/dahua/evento` | Marcación en tiempo real (API key) |

---

## PASO 8. Generar el ejecutable para entregar

NetBeans → clic derecho al proyecto → **Clean and Build (Shift + F11)**.
Se genera `target/SistemaBiometricoInteligente-1.0.jar`. Ejecuta en cualquier PC con Java:
```bat
java -jar SistemaBiometricoInteligente-1.0.jar
```
Si pones un `db.properties` en la misma carpeta que el `.jar`, usa ese en lugar del interno (útil para cambiar la contraseña sin recompilar).

---

## PASO 9. Seguir desarrollando con Claude Code

El proyecto incluye `CLAUDE.md`, que le explica a Claude Code la estructura y las reglas del proyecto.

1. Instala Claude Code (Windows PowerShell):
   ```powershell
   irm https://claude.ai/install.ps1 | iex
   ```
   (macOS/Linux: `curl -fsSL https://claude.ai/install.sh | bash`)
2. Abre una terminal en la carpeta del proyecto (en NetBeans también puedes usar *Window → IDE Tools → Terminal*):
   ```bat
   cd C:\Proyectos\SistemaBiometricoInteligente
   claude
   ```
3. Inicia sesión con tu cuenta de Claude y pide cambios, por ejemplo:
   - "Agrega un reporte de faltas por mes y exportación a Excel"
   - "Crea el CRUD de justificaciones en la API y en la web"
   - "Agrega la columna `cargo` a empleados y actualiza el script SQL"
4. Después de cada cambio, vuelve a NetBeans y ejecuta con **F6**.

---

## Problemas comunes

| Error | Solución |
|---|---|
| `Access denied for user 'root'` | Contraseña incorrecta en `db.properties` |
| `Communications link failure` | MySQL apagado → inicia el servicio MySQL80 |
| `Unknown database` | No ejecutaste el script del PASO 1 |
| `Public Key Retrieval is not allowed` | Mantén `allowPublicKeyRetrieval=true` en la URL |
| `Address already in use: bind` | El puerto 8080 está ocupado → cambia `servidor.puerto` o detén la ejecución anterior |
| Importación: `empleado no encontrado` | Ese User ID del Dahua no está en *Empleados* |
| `invalid target release: 17` o `release version 17 not supported` | NetBeans compila con un JDK viejo → *Properties → Build → Compile → Java Platform: JDK 17* |
| `Unsupported class file major version` | NetBeans 17 se está ejecutando con JDK 21 o superior → cambia `netbeans_jdkhome` a JDK 17 (PASO 2.1) |
| NetBeans 17 no encuentra `com.sbi.Main` al pulsar F6 | *Properties → Run → Main Class* → `com.sbi.Main` |
| Dependencias en rojo en NetBeans | Clic derecho → *Build with Dependencies* (requiere internet) |
| Acentos raros en la consola | Tools → Options → Java → Maven → agregar `-Dfile.encoding=UTF-8` |
