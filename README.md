# Biosur Terreno – Sistema de gestión operacional en terreno

Aplicación móvil nativa Android desarrollada para la empresa **Biosur SpA**, especializada en servicios sanitarios industriales, mantenimiento de plantas elevadoras de aguas servidas (PEAS), limpieza de fosas sépticas y atención de contingencias sanitarias en la Región del Biobío.

Proyecto correspondiente a la asignatura **DSY1105 – Desarrollo de Aplicaciones Móviles** (Duoc UC, Experiencia 2 y Experiencia 3 / Evaluación Parcial 4).
Repositorio del *backend* en Spring Boot: [BiosurTerreno_Backend_Grupo3](https://github.com/FerB22/BiosurTerreno_Backend_Grupo3).

---

## Integrantes del equipo

* **Fernando Barra Muñoz** (Diseño de interfaz en Jetpack Compose, navegación adaptativa, validaciones de seguridad, recursos nativos de hardware y temas visuales).
* **Esteban Medina** (Arquitectura de persistencia local fuera de línea con Room Database, DAO y repositorios).

---

## Arquitectura y tecnologías aplicadas

* **Lenguaje:** Kotlin 2.0+ (orientado a inmutabilidad y corrutinas).
* **Interfaz de usuario:** Jetpack Compose con directrices de **Material 3**.
* **Patrón de diseño:** **MVVM** (Model-View-ViewModel) con gestión de estado reactivo mediante `StateFlow`.
* **Diseño adaptativo:** Soporte dinámico para pantallas compactas (teléfonos) mediante `NavigationBar` inferior y pantallas extendidas (tabletas/horizontal) mediante `NavigationRail` evaluado con `WindowSizeClass`.
* **Apariencia visual:** Selector de 3 modos de contraste (Claro, Según sistema y Oscuro) gestionado en el ViewModel y redibujado dinámicamente en `AppInventarioTheme`.
* **Seguridad institucional:** Modelo de inicio de sesión con políticas de **denegación por defecto** (*Default Deny*) y **menor privilegio** (*Least Privilege*) validado en `AuthSecurityValidator`.
* **Recursos nativos de hardware:**
  1. **Cámara fotográfica:** Integración con contrato nativo `ActivityResultContracts.TakePicture()`, proveedor de archivos seguro `FileProvider` (`androidx.core.content.FileProvider`), captura pericial dual («Foto antes» y «Foto después»), submuestreo seguro contra desbordamiento de memoria (`inSampleSize`) y visualizador modal de imagen completa.
  2. **Georreferenciación satelital (GPS):** API oficial de Google Play Services (`FusedLocationProviderClient`) con arquitectura de adquisición híbrida (lectura de caché `lastLocation`, adquisición activa satelital `getCurrentLocation` de alta precisión y respaldo operativo defensivo para interiores).
* **Animaciones funcionales:** Desvanecimiento suave en mensajes de validación (`AnimatedVisibility` con `fadeIn`/`fadeOut`), animaciones cromáticas con `animateColorAsState` en el selector de tema y despliegue animado del catálogo de faenas sanitarias (`expandVertically`/`shrinkVertically`).

* **Red y consumo de servicios REST (Retrofit + Gson):**
  * **Microservicio de usuarios (`ms-usuarios`, puerto `8082`):** `POST /api/usuarios/login` y `GET /api/usuarios` conectados desde `UsuarioRemoteRepository` y `AuthViewModel` con respaldo local fuera de línea (*offline-first*).
  * **Microservicio de servicios (`ms-servicios`, puerto `8081`):** `GET/POST/PUT/DELETE /api/servicios` en Spring Boot con persistencia H2.
  * **API externa pública (Open-Meteo):** Consulta meteorológica en tiempo real (`GET https://api.open-meteo.com/v1/forecast`) integrada en `ClimaRepository` y desplegada en `HomeScreen` utilizando las coordenadas GPS de la faena sin interferir con los microservicios propios.
* **Pruebas unitarias (JUnit 4 + MockK + Coroutines Test):** Batería de **56 pruebas** cubriendo validación de seguridad, `AuthViewModel`, `ServicioViewModel`, `ServicioRepository`, `UsuarioRemoteRepository` y `ClimaRepository`.

---

## Estructura modular del proyecto

```text
com.example.appinventario
├── data/remote/            # Cliente RetrofitClient, UsuarioApiService y ClimaApiService
├── model/                  # Entidades Room e inmutables (Servicio, Usuario, ModoTema, Screen)
├── repository/             # Repositorios locales (Room) y remotos (UsuarioRemoteRepository, ClimaRepository)
├── ui/
│   ├── navigation/         # Grafo de navegación Compose adaptativo (BiosurNavGraph)
│   ├── screens/            # Pantallas (LoginScreen, HomeScreen, RegistroServicioScreen, DetalleServicioScreen, ConfiguracionScreen)
│   └── theme/              # Tokens de diseño Material 3 (Color, Theme, Type)
├── util/                   # Utilidades (CameraUtil, AuthSecurityValidator, ClimaUtil, UserPreferencesManager)
└── viewmodel/              # Lógica de negocio y gestión de estado (ServicioViewModel, AuthViewModel, ClimaUiState)
```

---

## Instrucciones de compilación y ejecución

### Prerrequisitos
* Android Studio Ladybug (o superior).
* Android SDK Platform 34+ / JDK 21.
* Dispositivo físico Android (con depuración USB activada) o emulador Android 14+ (API 34).

### 1. Compilación de depuración (Debug)
```powershell
./gradlew assembleDebug --no-daemon
```

Instalación mediante ADB:
```powershell
adb install -r "app/build/outputs/apk/debug/app-debug.apk"
```

### 2. Compilación de producción firmada (Release)
El proyecto incluye configuración de firmado oficial con el almacén de claves institucional [`app/biosur-release.jks`](app/biosur-release.jks):
```powershell
./gradlew assembleRelease --no-daemon
```

Instalación mediante ADB:
```powershell
adb install -r "app/build/outputs/apk/release/app-release.apk"
```

### 3. Conexión con el *backend* local (Spring Boot)
1. En [`BiosurTerreno_Backend_Grupo3`](https://github.com/FerB22/BiosurTerreno_Backend_Grupo3), iniciar `ms-usuarios` (`8082`) y `ms-servicios` (`8081`) con `.\mvnw.cmd spring-boot:run`.
2. En emulador Android, la aplicación apunta automáticamente a `http://10.0.2.2:8082/` y `8081/`. En teléfono físico por USB, ejecutar:
   ```powershell
   adb reverse tcp:8082 tcp:8082
   adb reverse tcp:8081 tcp:8081
   ```

### 4. Ejecución de pruebas unitarias
```powershell
./gradlew testDebugUnitTest
```
