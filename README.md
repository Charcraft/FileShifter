# FileShifter

**Suite ofimática offline para Android** — Kotlin + Jetpack Compose + Material 3.
Convierte, edita, escanea y valida documentos sin enviar nada a Internet.

- **Alumno:** Carlos de Jesús Martínez Pineda
- **Matrícula:** 253432 — Grupo 4C
- **Materia:** Desarrollo de Aplicaciones Móviles
- **Arquitectura:** MVVM (View → ViewModel → Repository → Datos)

---

## Estado del proyecto

| Comprobación | Resultado |
| --- | --- |
| `./gradlew assembleDebug` | ✅ compila |
| Instalación en dispositivo real (SM-A525M, Android 14) | ✅ |
| Pruebas unitarias (`testDebugUnitTest`) | ✅ 6/6 |
| Pruebas en dispositivo (`connectedDebugAndroidTest`) | ✅ 6/6 |
| Conversión imagen → PDF verificada | ✅ PDF A4 de 1 página con la imagen |
| Rotación de PDF verificada | ✅ 2 páginas a 180° |
| Historial (Room) verificado | ✅ registro persistente |
| QR y portada de entrega verificados | ✅ archivos generados |

Detalle honesto de qué está terminado y qué falta: **[docs/ESTADO_ACTUAL.md](docs/ESTADO_ACTUAL.md)**.

---

## Las 8 funciones

| # | Función | Estado |
| --- | --- | --- |
| F1 | Convertir universal (PDF, DOCX, XLSX, PPTX, TXT, CSV, HTML, imágenes → PDF/TXT) | ✅ |
| F2 | Imagen a PDF (unir o uno por imagen) | ✅ |
| F3 | Editar PDF (unir, dividir, comprimir, rotar, eliminar, reordenar, marca de agua) | ✅ |
| F4 | Corrector ortográfico offline (tildes, repeticiones, errores frecuentes) | 🟡 básico |
| F5 | Modo Entrega (3 plantillas, formulario, vista previa, PDF) | 🟡 sin importar plantilla |
| F6 | Validador con semáforo, SHA-256 y QR | ✅ |
| F7 | Escáner con cámara + OCR + PDF | 🟡 sin detección de bordes |
| F8 | QR / barras (leer, generar, historial) | ✅ |

Extras: **Historial** con Room, **Ajustes** con DataStore y flujo configurable
(los interruptores ya ocultan las sugerencias de Corregir / Entrega / Validar
en las tarjetas de resultado).

---

## Arquitectura MVVM

```
View (Compose)  →  ViewModel  →  Repository  →  Datos
```

```
com.example.fileshifter
├── FileShifterApp.kt          # Application + AppContainer (inyección manual)
├── data
│   ├── model/                 # modelos puros (sin Android)
│   ├── local/                 # Room (historial) + DataStore (ajustes)
│   ├── source/                # PDFBox, extractor Office, OCR, QR, archivos
│   └── repository/             # DocumentoRepository, HistorialRepository
├── domain/                    # CorrectorOrtografico (lógica pura)
├── presentation
│   ├── home/ convert/ image/ edit/ corrector/
│   ├── delivery/ validator/ scanner/ qr/ history/ settings/
│   └── common/                # pantalla base, cámara, componentes
├── navigation/AppNav.kt
└── ui/theme/                  # Material 3 celeste #0EA5E9
```

Reglas que se respetan en todo el código:

1. La pantalla **no** abre archivos ni convierte nada: manda eventos.
2. El **ViewModel** guarda el estado y llama al repositorio.
3. El **repositorio** es la única puerta a PDFBox, Room, DataStore y ML Kit.
4. Los flujos de varios pasos (Editar, Entrega) comparten un ViewModel dentro
   de un grafo de navegación anidado.

Documento completo: **[docs/ARQUITECTURA_MVVM.md](docs/ARQUITECTURA_MVVM.md)**.

---

## Interfaz

La app reproduce el prototipo de Figma (`https://input-chop-13093493.figma.site`):

- Barra superior azul marino con el logotipo y el botón de regreso cuadrado.
- Franjas informativas celestes, tarjetas blancas con borde suave y chips de icono.
- Pantalla de bienvenida (splash) con el logotipo, tres beneficios y botón **Comenzar**.
- Pasos numerados en Editar PDF y Modo Entrega; pestañas en QR.
- **Pantalla completa real (edge-to-edge)**: la app dibuja detrás de la barra de
  estado y de navegación, con insets correctos en notch, gestos y landscape.
- Rejilla adaptativa (2 o 3 columnas según el ancho) y contenido centrado con
  ancho máximo de 620 dp en tablets y plegables.
- **Icono de la app** con el logotipo: adaptive icon (fondo azul marino + tarjeta
  blanca con el documento), PNG para Android 7 y variante monocromática para
  iconos temáticos.

---

## Compilar y ejecutar

Requisitos: Android Studio, Android SDK 35, **JDK 21 para Gradle**.

> El daemon de Gradle se fija en Java 21 con
> `gradle/gradle-daemon-jvm.properties` porque el JDK 25 rompe el procesador
> KSP de Room. Si Gradle no encuentra el JDK, ajusta
> `org.gradle.java.installations.paths` en `gradle.properties`.

```bash
./gradlew assembleDebug          # APK de depuración
./gradlew testDebugUnitTest      # lógica pura (corrector)
./gradlew connectedDebugAndroidTest   # PDFBox real en el teléfono
```

APK resultante: `app/build/outputs/apk/debug/app-debug.apk`.

---

## Dependencias principales

| Área | Librería |
| --- | --- |
| UI | Jetpack Compose + Material 3 + Navigation Compose |
| Estado | Lifecycle ViewModel + StateFlow |
| PDF | PDFBox-Android (`com.tom-roush`) |
| Office | Lector ZIP/XML propio (DOCX/XLSX/PPTX, sin macros) |
| OCR y códigos | ML Kit Text Recognition + Barcode Scanning |
| Cámara | CameraX |
| Datos | Room (historial) + DataStore (ajustes) |
| QR | ZXing core |

---

## Documentación

- `docs/ARQUITECTURA_MVVM.md` — capas, paquetes, inyección y flujos.
- `docs/ESTADO_ACTUAL.md` — auditoría de lo prometido vs. lo implementado.
- `README.md` — este archivo.
