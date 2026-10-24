# Biosur Terreno – Sistema de gestión operacional en terreno

Aplicación móvil nativa Android desarrollada para la empresa **Biosur SpA**, especializada en servicios sanitarios industriales, mantenimiento de plantas elevadoras de aguas servidas (PEAS), limpieza de fosas sépticas y atención de contingencias sanitarias en la Región del Biobío.

Proyecto correspondiente a la **Evaluación Parcial 2** de la asignatura **DSY1105 – Desarrollo de Aplicaciones Móviles** (Duoc UC).

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

---

## Estructura modular del proyecto

```text
com.example.appinventario
├── model/                  # Entidades inmutables de dominio (Servicio, Usuario, ModoTema, Screen)
├── ui/
│   ├── navigation/         # Grafo de navegación Compose adaptativo (BiosurNavGraph)
│   ├── screens/            # Pantallas (LoginScreen, HomeScreen, RegistroServicioScreen, DetalleServicioScreen, ConfiguracionScreen)
│   └── theme/              # Tokens de diseño Material 3 (Color, Theme, Type)
├── util/                   # Utilidades de hardware y seguridad (CameraUtil, AuthSecurityValidator)
└── viewmodel/              # Lógica de negocio y gestión de estado reactivo (ServicioViewModel, AuthViewModel)
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
El proyecto incluye configuración de firmado oficial con el almacén de claves institucional `biosur-release.jks`:
```powershell
./gradlew assembleRelease --no-daemon
```

Instalación mediante ADB:
```powershell
adb install -r "app/build/outputs/apk/release/app-release.apk"
```

### 3. Ejecución de pruebas unitarias
```powershell
./gradlew testDebugUnitTest
```
