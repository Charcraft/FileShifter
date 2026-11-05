# FileShifter — Estado actual frente a lo prometido

Auditoría hecha el **5 de noviembre de 2026** comparando la *Propuesta y diseño de
interfaces* (Etapa 1) con el código que existe hoy en el repositorio.

> Resumen: la app **compila, se instala y funciona en un teléfono real**. La base
> MVVM está completa y las funciones principales ya generan archivos de verdad.
> Quedan tres funciones con alcance reducido (corrector, escáner y modo entrega)
> y una mejora de interacción (reordenar arrastrando).

---

## 1. Verificación técnica ejecutada

| Prueba | Comando | Resultado |
| --- | --- | --- |
| Compilación | `./gradlew assembleDebug` | ✅ BUILD SUCCESSFUL |
| Unitarias | `./gradlew testDebugUnitTest` | ✅ 6/6 |
| Instrumentadas (PDFBox real) | `./gradlew connectedDebugAndroidTest` | ✅ 6/6 |
| Instalación | `adb install app-debug.apk` | ✅ en SM-A525M / Android 14 |
| Imagen → PDF | UI real | ✅ `IMG-..._1.pdf`, A4, 1 página, 1 imagen (verificado con PyMuPDF) |
| Rotar PDF | UI real | ✅ 2 páginas, rotación 180° |
| Historial | UI real | ✅ registro en Room visible tras reiniciar |
| QR | UI real | ✅ PNG guardado en `FileShifter/` |
| Modo Entrega | UI real | ✅ PDF con materia, título, alumno e índice |
| Corrector | UI real | ✅ detecta «aquí», «también», «había», «información» |

Los archivos generados se guardan en:
`Android/data/com.carlos.fileshifter/files/FileShifter/`

---

## 2. Función por función

### F1 — Convertir universal ✅

- Entrada: PDF, DOCX, XLSX, PPTX, TXT, CSV, HTML, JPG/PNG/WEBP/HEIC.
- Salida: **PDF** (texto o imágenes) y **TXT** (extracción).
- DOCX/XLSX/PPTX se leen con un extractor ZIP+XML propio: texto y tablas, sin macros
  (exactamente el límite que anuncia la propuesta).
- **No** convierte PDF → DOCX/XLSX (la propuesta tampoco lo promete: el menú muestra
  solo formatos compatibles).

### F2 — Imagen a PDF ✅

- Unir en un solo PDF o generar uno por imagen.
- Página A4 vertical u horizontal según la orientación de la foto.
- Imágenes grandes se reducen a 2400 px para no agotar memoria.
- Control de calidad Baja / Media / Alta (JPEG 60 %, JPEG 82 %, sin pérdida).

### F3 — Editar PDF ✅

| Edición | Estado |
| --- | --- |
| Unir (2+ PDF) | ✅ |
| Dividir (un PDF por página) | ✅ |
| Comprimir | ✅ (re-guardado sin objetos de seguridad) |
| Rotar 90/180/270 | ✅ (verificado en dispositivo) |
| Eliminar páginas (`2,4-6`) | ✅ |
| Reordenar (`3,1,2`) | ✅ por números — **falta** la cuadrícula arrastrable |
| Marca de agua | ✅ texto diagonal en todas las páginas |

### F4 — Corrector 🟡 básico

Implementado con reglas deterministas (sin IA, sin conexión):

- tildes en palabras de uso común (aqui → aquí, tambien → también…),
- terminaciones `-cion` / `-sion`,
- palabras repetidas («la la»),
- errores frecuentes (havia → había),
- mayúscula al inicio de oración.

Lo que **no** hace todavía: diccionario completo de 2.5 MB ni gramática avanzada.
La arquitectura (`domain/CorrectorOrtografico.kt`) permite sustituir el diccionario
sin tocar la UI.

### F5 — Modo Entrega 🟡

- ✅ Tres plantillas (Escolar, Oficio, Minimal).
- ✅ Formulario: nombre, matrícula, materia, título e índice.
- ✅ Vista previa paginada y PDF real.
- ❌ «Importar mi plantilla PDF» (usar un PDF del usuario como fondo) queda pendiente.

### F6 — Validador ✅

- Semáforo VERDE/AMARILLO/ROJO con tamaño, páginas y dimensiones.
- Hash SHA-256 del archivo.
- QR con el hash para verificación offline.
- Compartir el resumen con el hash por cualquier app.

### F7 — Escáner 🟡

- ✅ Cámara CameraX a pantalla completa, marco guía, captura, OCR con ML Kit y
  salida a PDF o al corrector. También admite una imagen de la galería.
- ❌ Detección automática de bordes, corrección de perspectiva, filtros y
  multipágina: pendiente (requiere un paso de visión por computadora).

### F8 — QR y barras ✅

- Lectura con cámara (ML Kit) y desde una imagen de la galería.
- Generación de QR y CODE-128 con ZXing.
- Pestaña de historial de códigos y todo lo generado entra al historial.

---

## 3. Interfaz y logotipo

Rediseño basado en el prototipo de Figma del alumno
(`https://input-chop-13093493.figma.site`), manteniendo el flujo propio de la app:

| Elemento | Estado |
| --- | --- |
| Barra superior azul marino con logo y botón de regreso | ✅ |
| Franjas informativas, tarjetas blancas y chips de icono | ✅ |
| Splash con logotipo, beneficios y botón Comenzar | ✅ verificado en teléfono |
| Rejilla de herramientas adaptativa (2/3 columnas) | ✅ |
| Insets de borde a borde (notch, gestos, landscape) | ✅ |
| Ancho máximo de 620 dp en tablets | ✅ |
| Icono de app con el logotipo (adaptive + legacy + monochrome) | ✅ |
| Pasos numerados y pestañas como el prototipo | ✅ |

Diferencias conscientes con el prototipo (el flujo es distinto): la app usa el
selector de archivos de Android en lugar de listar todos los PDF del teléfono;
«Importar mi plantilla» se muestra como acción pendiente.

---

## 4. Arquitectura MVVM

| Requisito de la materia | Estado |
| --- | --- |
| View separada de la lógica | ✅ cada pantalla solo pinta estado y emite eventos |
| ViewModel por pantalla / flujo | ✅ 11 ViewModels |
| `UiState` inmutable + `StateFlow` | ✅ |
| Repository como contrato | ✅ `DocumentoRepository` (interfaz) |
| Datos aislados | ✅ Room, DataStore, PDFBox y ML Kit solo en `data/` |
| Inyección de dependencias | ✅ `FileShifterApp` + `AppContainer` + `appViewModel` |
| Persistencia | ✅ Room (historial) y DataStore (ajustes) |
| Flujos multipaso con estado compartido | ✅ Editar y Entrega con grafo anidado |
| Pruebas | ✅ 6 unitarias + 6 instrumentadas |

---

## 5. Pendientes, en orden de impacto

1. **Reordenar arrastrando** en Editar PDF (hoy se escribe el orden).
2. **Importar plantilla PDF/DOCX** en Modo Entrega.
3. **Escáner con detección de bordes** y corrección de perspectiva.
4. **Corrector con diccionario completo** (asset de ~2.5 MB) y reglas gramaticales.
5. Compartir el QR del Validador como imagen.
6. Multihilo por lotes con WorkManager y notificación (la propuesta lo menciona
   para 20 archivos).

---

## 6. Cómo se probó en el teléfono

1. `./gradlew assembleDebug`
2. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. Flujo manual: Inicio → Convertir → Añadir archivo → Convertir → resultado.
4. Historial → el archivo aparece con operación, peso y fecha.
5. Editar PDF → elegir PDF → Rotar → 180° → Aplicar.
6. QR / Barras → Generar → QR visible y guardado.
7. Modo Entrega → Escolar → datos → Generar PDF.
8. Los PDF se extrajeron del teléfono y se validaron con PyMuPDF (páginas,
   tamaño, rotación e imágenes).
