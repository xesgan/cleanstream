# 🧩 CleanStream

**Desarrollo de Interfaces — DI01 · DI01_2 · DI03**

## 📋 Descripción general

CleanStream es una aplicación de escritorio creada en **Java Swing**, diseñada como una interfaz gráfica (GUI) moderna para la herramienta **yt-dlp**, e integrada posteriormente con la **DI Media NET API** para sincronizar archivos multimedia en la nube.

El proyecto se desarrolla utilizando:
- **NetBeans 27 / 28**
- **JDK 24**
- **Maven**
- **Swing + Designer**

## 📌 Estado del Proyecto

### ✔ DI01

GUI inicial + configuración + descarga de vídeos/audio mediante yt-dlp.

### ✔ DI01_2

Gestión de biblioteca local con:
- `JList<Object>`
- `JComboBox<Object>`
- `JTable` con `AbstractTableModel`
- Renderers personalizados
- Filtrado dinámico
- Escaneo de carpetas
- Metadatos locales

### 🟦 DI03 — Parte 1 completada

**Funcionalidad implementada:**

- Formulario de Login sin Designer
- Autenticación contra **DI Media NET API** (JWT)
- Recuperación de datos del usuario con `/api/Users/me`
- Sistema **Remember Me** con expiración automática de 3 días
- Logout con limpieza de sesión
- Refactor de navegación: `pnlContent` como contenedor único
- Preparación para integración futura del componente de polling

## 🚀 Funcionalidades Principales

### 🔐 Login con JWT (DI03 Parte 1)

- Captura de email y contraseña
- Validación básica
- Llamada a `ApiClient.login(email, password)`
- Obtención del token JWT
- Llamada a `getMe(token)` para cargar datos del usuario
- Transición limpia al panel principal usando callback (`onLoginSuccess`)

### ✔ Remember Me avanzado

- Guarda: email, token y timestamp
- Expira automáticamente si pasan 3 días
- Pre-rellena el Login si el token sigue siendo válido
- Previene auto-login si se ha caducado

### ✔ Logout

- Limpieza del Remember Me
- Limpieza visual de la interfaz
- Retorno al Login

### 🗂️ Navegación unificada

Todo el proyecto ahora usa un único contenedor central: **`pnlContent`**, donde se cargan:
- `LoginPanel`
- `MainPanel`
- `PreferencesPanel`

Con un método único:
```java
private void showInContentPanel(Component comp)
```

Permitiendo una navegación estable, limpia y mantenible.

### 🔌 Integración con la DI Media NET API

Se utiliza la clase proporcionada por el profesor:
- `ApiClient.java`
- `Usuari.java`
- `Media.java`

**Endpoints usados en esta fase:**
- `/api/Auth/login`
- `/api/Users/me`
- `/api/Files/me` (Postman)
- `/api/Files/upload` (Postman)
- `/api/Files/all` (Postman)
- `/api/Files/{id}` (Postman)
- `/api/Users/{id}/nickname` (Postman)

### 🔎 Pruebas Postman (Requisito DI03 Parte 1)

He creado una colección completa con todas las peticiones necesarias:

📁 `postman/DI03_DI_Media_NET_EliasRoig.postman_collection.json`

**Incluye:**
- Registro de usuario
- Login
- Upload (1 vídeo + 2 audios)
- Listado de ficheros
- Descarga por ID
- Nickname por ID

> Esta colección se puede importar directamente en Postman para validar la conectividad y endpoints.

### 🎬 Descarga de vídeos/audio con yt-dlp

- **Botón Stop**: permite detener una descarga en ejecución de forma segura desde la interfaz
- **Selector de calidad**: muestra un JOptionPane con la calidad detectada al finalizar la descarga
- **Creación automática de playlists .m3u**: al finalizar una serie de descargas, la aplicación genera un archivo de lista de reproducción
- **Gestión de audio y vídeo**: compatibilidad con descargas de tipo audio (-x) o vídeo completo
- **Flags de estabilidad**: soporte para opciones avanzadas de yt-dlp como `--force-ipv4`, `--http-chunk-size 10M`, `--concurrent-fragments 1` y `--retries infinite`
- **Validación mejorada de rutas**: comprueba la existencia de las rutas de yt-dlp, ffmpeg y la carpeta de salida

## 🧱 Arquitectura del Proyecto
```
cleanstream/
│
├── src/main/java/cat/dam/roig/cleanstream
│   ├── ui
│   │   ├── MainFrame.java
│   │   ├── LoginPanel.java
│   │   ├── MainPanel.java
│   │   └── PreferencesPanel.java
│   │
│   ├── models
│   │   ├── Media.java
│   │   ├── Usuari.java
│   │   └── ResourceDownloaded.java
│   │
│   ├── services
│   │   ├── ApiClient.java   (proporcionado)
│   │   └── [ApiService.java para DI03 Parte 3]
│   │
│   └── utils
│       ├── CommandExecutor.java
│       └── DownloadsScanner.java
│
└── resources/
```

## 🧭 Instrucciones de uso

### 1. Iniciar sesión

1. Introducir **email** y **contraseña**
2. Marcar **Remember Me** si se desea mantener la sesión (3 días)
3. Pulsar **Login**
4. El sistema cargará automáticamente los datos del usuario

### 2. Configurar rutas (Primera vez)

1. Abrir **Edit > Preferences**
2. Establecer las rutas de:
   - yt-dlp
   - ffmpeg
   - Carpeta de salida
3. Configurar opciones adicionales (límite de velocidad, creación de .m3u)
4. Guardar los cambios con el botón **Volver**

### 3. Descargar contenido

1. Introducir la URL del vídeo o playlist en el campo principal
2. Pulsar **Download** para iniciar el proceso
3. Observar el progreso en tiempo real en el área de logs
4. Usar **Stop** para interrumpir si es necesario

### 4. Consultar la biblioteca multimedia

- Visualizar los archivos descargados desde la JList o JTable
- Filtrar resultados mediante la JComboBox
- Ver detalles de cada archivo seleccionado

### 5. Cerrar sesión

- Usar **File > Logout** para cerrar sesión y limpiar credenciales guardadas

## 🧠 Estado actual del proyecto

### ✅ Completado

- Interfaz gráfica funcional (JFrame + JPanel)
- Sistema de autenticación con JWT
- Remember Me con expiración automática
- Navegación unificada con `pnlContent`
- Menú con navegación y panel de preferencias
- Ejecución real de yt-dlp con logs en tiempo real
- Botón Stop funcional
- Carga de archivos descargados y visualización en JList/JTable
- Creación automática de listas .m3u
- Validación de campos y control de errores
- Integración con DI Media NET API
- Colección Postman completa

### 🚧 Pendiente (Próximos pasos DI03 Parte 2)

- Crear `SessionManager` para gestionar token/usuario
- Implementar el **Polling Component** externo
- Crear proyecto independiente para el componente
- Empaquetar el componente con maven-shade
- Probar el componente dentro de CleanStream
- Funcionalidad de sincronización:
  - Ver red vs local
  - Subir ficheros
  - Descargar ficheros de otros usuarios

> El proyecto se encuentra en fase estable con integración básica de API completada.

## 🪛 Problemas encontrados y soluciones

| Problema | Causa | Solución aplicada |
|----------|-------|-------------------|
| Paneles superpuestos al iniciar | Ambos añadidos al contentPane desde el Designer | Se controló la visibilidad en el constructor de MainFrame |
| Congelamiento al ejecutar yt-dlp | Ejecución en el hilo principal | Implementación de SwingWorker con `publish()` |
| Navegación entre paneles inconsistente | Múltiples métodos de cambio de panel | Refactor con método único `showInContentPanel()` |
| Error 403 al descargar de YouTube | Cambios en la API | Flags: `--compat-options youtube-disable-po-token`, `--force-ipv4`, `--user-agent Mozilla/5.0` |
| Remember Me persistía indefinidamente | Falta de control de expiración | Sistema de timestamp con validación de 72h |
| Token no se limpiaba al logout | Falta de método de limpieza | Implementación de `clearRememberMe()` |

> El proyecto ha sido probado en **Linux Manjaro** y **Windows**, ejecutando binarios locales de yt-dlp y ffmpeg.

## 📚 Recursos y referencias

### 📌 Tecnologías

- Java Swing
- Maven
- NetBeans Designer
- yt-dlp
- ffmpeg
- HttpClient (Java 11+)
- JSON Jackson Databind

### 📌 API

- **DI Media NET** — Azure

### 📌 Documentación oficial

- [Documentación oficial de yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [Documentación oficial de ffmpeg](https://ffmpeg.org/)
- [HttpClient Documentation](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html)
- [Jackson Databind](https://github.com/FasterXML/jackson-databind)

### 📌 Apuntes y material del curso

- Apuntes oficiales del módulo DI
- Videos soporte DI01 / DI02 / DI03
- Enunciado Tarea para DI01_1 25-26
- Enunciado Tarea para DI01_2 25-26
- Enunciado Tarea para DI03 25-26
- DI01 Support Notes 25-26
- DI03 Support Notes 25-26
- Tutorias realizadas por el profesor

### 📌 Consultas externas y soporte

- **ChatGPT** (modelo GPT-5, OpenAI): resolución de errores, documentación y guía de implementación
- **GitHub Copilot**: sugerencias de código
- **StackOverflow**: ejemplos sobre ProcessBuilder, SwingWorker, AbstractTableModel y HttpClient

> **Aclaración**: Todo el código extra generado con asistencia (ChatGPT / Copilot) ha sido comprendido, adaptado, modificado y documentado, conforme a las normas del módulo.

## 🧩 Funcionalidades extra / mejoras

- Expiración automática temporal para Remember Me (72h)
- Sistema de navegación unificado con `pnlContent`
- Refactor del MainFrame para simplificar la UI
- Limpieza de eventos y renderers
- Preparación para SessionManager (Parte 2–3)
- Corrección de errores de selección y renderizado
- Validación de rutas antes de ejecutar yt-dlp
- Renderizado personalizado en JList

## 👨‍💻 Créditos

- **Autor**: Elias Roig
- **Asistencia técnica y documentación**: ChatGPT (OpenAI GPT-5), GitHub Copilot
- **Curso**: Desarrollo de Interfaces — FP DAM 2025-26

---

## 🚀 Instalación y uso
```bash
# Clonar el repositorio
git clone https://github.com/tu-usuario/cleanstream.git

# Abrir el proyecto en NetBeans 27/28 con JDK 24
# Compilar con Maven y ejecutar
```

### Requisitos previos

- **NetBeans 27 o 28**
- **JDK 24**
- **Maven** (integrado en NetBeans)
- **yt-dlp** instalado en el sistema
- **ffmpeg** instalado en el sistema
- Conexión a Internet para acceder a **DI Media NET API**

```

## 📝 Licencia

Este proyecto es de uso educativo para el curso de Desarrollo de Interfaces.
