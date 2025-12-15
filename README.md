# 🧩 CleanStream

**Desarrollo de Interfaces** — DI01 · DI01_2 · DI03

## 📋 Descripción general

CleanStream es una aplicación de escritorio desarrollada en Java Swing que actúa como interfaz gráfica avanzada para yt-dlp y que integra sincronización bidireccional con la DI Media NET API, permitiendo gestionar contenidos multimedia tanto en local como en la nube.

### 🛠️ Tecnologías utilizadas

- **NetBeans** 27 / 28
- **JDK** 24
- **Maven**
- **Java Swing** (con y sin Designer)

## 📌 Estado del Proyecto

### ✔ DI01
- Interfaz gráfica inicial
- Configuración de rutas
- Descarga de audio y vídeo con yt-dlp

### ✔ DI01_2
- Biblioteca local multimedia
- Escaneo de carpetas
- Filtros dinámicos
- Renderers personalizados
- Visualización de metadatos
- Uso de JList, JComboBox y JTable con AbstractTableModel

### ✔ DI03 — COMPLETADO
Integración completa con la DI Media NET API mediante un componente externo, con sincronización local ↔ cloud y acciones desde la interfaz gráfica.

## 🚀 Funcionalidades Principales

### 🔐 Autenticación (JWT)
- Login con email y contraseña
- Autenticación contra la API (JWT)
- Carga de datos del usuario
- Sistema Remember Me con expiración automática (72h)
- Auto-login seguro con validación del token
- Logout con limpieza de sesión y conservación opcional del email

### ☁️ Sincronización Local ↔ Cloud (DI03 Parte 3)
- Carga de medios desde la nube (getAllMedia)
- Escaneo de biblioteca local
- Identificación automática del estado de cada recurso:
  - `LOCAL`
  - `CLOUD`
  - `LOCAL + CLOUD`
- Visualización unificada de recursos locales y cloud
- Renderizado personalizado con estado visible

### 🔁 Acciones sobre recursos
Desde la interfaz gráfica:
- **Download** → Cloud → Local
- **Upload** → Local → Cloud
- **Delete** → Eliminación local + refresco de estado
- Refresco automático tras cada acción
- Sincronización consistente usando SwingWorker

### ⚙️ Persistencia de preferencias
Mediante `java.util.prefs.Preferences`:
- Carpeta de descargas
- Carpeta de escaneo
- Rutas de yt-dlp y ffmpeg
- Email recordado (Remember Me)

Las preferencias se mantienen entre ejecuciones.

### 🎬 Descarga avanzada con yt-dlp
- Descarga de audio o vídeo
- Selector de calidad
- Botón Stop para detener descargas activas
- Logs en tiempo real
- Creación automática de playlists `.m3u`
- Validación de rutas y binarios
- Uso de flags avanzados para estabilidad

## 🧱 Arquitectura del Proyecto

```
└── cleanstream
    ├── app
    │   └── CleanStreamApp.java
    ├── controller
    │   ├── DownloadExecutionController.java
    │   ├── DownloadsController.java
    │   └── MainController.java
    ├── main
    │   ├── MainFrame.form
    │   └── MainFrame.java
    ├── models
    │   ├── MetadataTableModel.java
    │   ├── ResourceDownloaded.java
    │   ├── ResourceState.java
    │   └── VideoQuality.java
    ├── services
    │   ├── AuthManager.java
    │   ├── DownloadsScanner.java
    │   └── UserPreferences.java
    ├── ui
    │   ├── AboutDialog.form
    │   ├── AboutDialog.java
    │   ├── LoginPanel.java
    │   ├── PreferencesPanel.form
    │   ├── PreferencesPanel.java
    │   └── renderers
    │       └── ResourceDownloadedRenderer.java
    └── utils
        ├── CommandExecutor.java
        └── DetectOS.java
```

La arquitectura separa claramente UI, lógica de negocio y servicios, facilitando mantenimiento y escalabilidad.

## 🔌 Integración con DI Media NET API

Se utiliza el componente proporcionado por el profesor (`ApiClient`) y un wrapper propio.

### Endpoints utilizados:
- `/api/Auth/login`
- `/api/Users/me`
- `/api/Files/all`
- `/api/Files/upload`
- `/api/Files/{id}`

## 🔎 Pruebas con Postman

Colección completa incluida:

📁 `postman/DI03_DI_Media_NET_EliasRoig.postman_collection.json`

Incluye:
- Login
- Upload
- Listado de archivos
- Descarga por ID
- Verificación de endpoints

## 🧠 Concurrencia y estabilidad

- Uso de `SwingWorker` para operaciones de red y disco
- La UI nunca se bloquea
- Gestión correcta de errores (401, rutas inválidas, archivos inexistentes)

## 🧪 Problemas relevantes resueltos

| Problema | Solución |
|----------|----------|
| UI bloqueada | Uso de SwingWorker |
| Token caducado | Validación previa y limpieza automática |
| Lista no seleccionable | Corrección de enable/disable en JList |
| Estados desincronizados | Refresco automático tras acciones |
| Persistencia inconsistente | Centralización en UserPreferences |

## 🧠 Estado final del proyecto

- ✔ Cumple todos los requisitos de DI03
- ✔ Integra componente externo
- ✔ Permite interacción real con la nube
- ✔ Arquitectura limpia y mantenible
- ✔ Preparado para ampliaciones futuras

## 👨‍💻 Créditos

**Autor:** Elias Roig  
**Curso:** Desarrollo de Interfaces — FP DAM 2025-26  
**Asistencia técnica:** ChatGPT (OpenAI), GitHub Copilot

> Todo el código asistido ha sido comprendido, adaptado y documentado conforme a las normas del módulo.

## 🚀 Instalación

```bash
git clone https://github.com/tu-usuario/cleanstream.git
```

Abrir en NetBeans 27/28 con JDK 24 y ejecutar con Maven.

## 📝 Licencia

Proyecto de uso educativo para el módulo de Desarrollo de Interfaces.
