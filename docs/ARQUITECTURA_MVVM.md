# FileShifter — Arquitectura MVVM

Documento de arquitectura del proyecto, alineado a la Unidad II de la materia
**Desarrollo de Aplicaciones Móviles** (4C) y al proyecto base `mvvm_c`.

## 1. Capas

```
View / UI  →  ViewModel  →  Repository  →  Datos
 (Compose)     (estado)      (contratos)    (PDFBox, Room, DataStore, ML Kit)
```

| Capa | Paquete | Responsabilidad |
| --- | --- | --- |
| View | `presentation/<feature>/*Screen.kt` | Dibuja la pantalla, emite eventos, observa el estado. No decide nada. |
| ViewModel | `presentation/<feature>/*ViewModel.kt` | Guarda el estado de la pantalla, valida, llama al repositorio y expone `StateFlow`. |
| Repository | `data/repository/` | Contrato único de acceso a datos. La UI nunca toca PDFBox, Room ni DataStore. |
| Datos | `data/local/`, `data/source/`, `data/model/` | Room (historial), DataStore (ajustes), motores PDF/Office, ML Kit, archivos. |

## 2. Regla de oro

La pantalla **no** abre archivos, **no** convierte PDF y **no** guarda nada.
Solo hace dos cosas:

1. Mandar eventos al ViewModel (`onArchivosSeleccionados`, `onConvertir`, ...).
2. Pintar el `UiState` que el ViewModel produce.

## 3. Flujo de una acción (ejemplo: convertir imágenes a PDF)

```
Usuario toca "Convertir a PDF"
        ↓ evento
ImageToPdfViewModel.convertir()
        ↓
DocumentRepository.imagesToPdf(...)
        ↓
PdfEngine (PDFBox) escribe el archivo
        ↓
HistoryRepository guarda el registro (Room)
        ↓
ViewModel publica nuevo estado: ConversionUiState(exito = ...)
        ↓
La pantalla muestra el resultado
```

## 4. Estructura de paquetes

```
com.example.fileshifter
├── FileShifterApp.kt              # Application + contenedor de dependencias
├── MainActivity.kt
├── data
│   ├── model/                     # modelos de dominio (no Android)
│   ├── local/                     # Room: historial
│   ├── source/                    # motores: PDFBox, Office, OCR, QR
│   └── repository/                # DocumentRepository, HistoryRepository, SettingsRepository
├── domain/                        # reglas puras (corrector ortográfico, validador)
├── presentation
│   ├── home/  convert/  image/  edit/  corrector/
│   ├── delivery/  validator/  scanner/  qr/  history/  settings/
└── ui/theme/                      # Material 3 celeste
```

Cada feature repite el mismo trío:

```
ConvertScreen.kt      ← View
ConvertViewModel.kt   ← ViewModel (+ ConvertUiState)
```

## 5. Inyección de dependencias

Sin Hilt, para mantenerlo legible en clase:

- `FileShifterApp` crea un `AppContainer` con los repositorios (Room, DataStore,
  PDFBox y ML Kit viven ahí).
- La función `appViewModel { ... }` (en `presentation/common/ViewModelExt.kt`)
  construye el ViewModel con las dependencias del contenedor y lo guarda en el
  `ViewModelStore` de la pantalla o del grafo de navegación.

```kotlin
val vm: ConvertViewModel = appViewModel { ConvertViewModel(it.documentoRepository) }
```

## 6. Flujos con varios pasos

`Editar PDF` y `Modo Entrega` usan un **grafo de navegación anidado** y un ViewModel
compartido (`EditFlowViewModel`, `DeliveryFlowViewModel`) obtenido con
`navController.getBackStackEntry("edit_flow")`. Así la selección sobrevive a los
cambios de paso y a la rotación de pantalla.

## 7. Persistencia

| Qué | Cómo |
| --- | --- |
| Historial de archivos generados | Room (`HistoryEntity` + `HistoryDao`) |
| Preferencias del flujo (sugerir corrector/entrega/validador) | DataStore Preferences |
| Archivos generados | carpeta externa privada `FileShifter/` + `FileProvider` para compartir |

## 8. Pruebas

- `./gradlew assembleDebug` compila toda la app.
- `./gradlew testDebugUnitTest` ejecuta las pruebas de lógica pura
  (`CorrectorOrtograficoTest`: tildes, repeticiones, errores frecuentes).
- `./gradlew connectedDebugAndroidTest` ejecuta `PdfEngineTest` en el teléfono:
  crear/leer PDF, rotar, marca de agua, comprimir, unir, dividir, eliminar,
  reordenar y parseo de rangos de páginas.

## 9. Flujo configurable

Los interruptores de Ajustes se guardan con DataStore. `TarjetaResultado` los lee
y muestra u oculta las sugerencias **Corregir / Entrega / Validar** al terminar
una conversión, tal como describe la propuesta.
