# 🧩 CleanStream

**Desarrollo de Interfaces — Semanas 2 y 3 (DI01_1 + DI01_2)**

## 📋 Descripción general

CleanStream es una aplicación de escritorio desarrollada en Java Swing con NetBeans 27 y JDK 24, que actúa como interfaz gráfica para la herramienta yt-dlp.

Su objetivo es ofrecer una interfaz limpia y funcional para descargar vídeos o audios desde plataformas online, con configuración personalizable, ejecución en segundo plano y registro visual del proceso.

- **Semana 2 (DI01_1)**: Implementación de la estructura principal, la interfaz gráfica y la ejecución asíncrona de comandos.
- **Semana 3 (DI01_2)**: Ampliación con funcionalidades de gestión de la biblioteca multimedia, utilizando JList, JComboBox y JTable con modelos personalizados.

## 🧩 Nuevas funcionalidades (Semana 3 avanzada)

Durante la tercera semana se ha ampliado el alcance de la aplicación añadiendo nuevas funciones que mejoran la experiencia de usuario, la estabilidad y la capacidad de gestión de contenidos descargados:

- **Botón Stop**: permite detener una descarga en ejecución de forma segura desde la interfaz
- **Selector de calidad**: muestra un JOptionPane con la calidad detectada al finalizar la descarga, facilitando la validación del proceso
- **Creación automática de playlists .m3u**: al finalizar una serie de descargas, la aplicación genera un archivo de lista de reproducción en la carpeta de destino
- **Gestión de audio y vídeo**: se añade compatibilidad con descargas de tipo audio (-x) o vídeo completo, según selección del usuario
- **Flags de estabilidad**: el CommandExecutor añade soporte para opciones avanzadas de yt-dlp como `--force-ipv4`, `--http-chunk-size 10M`, `--concurrent-fragments 1` y `--retries infinite`
- **Renderizado básico de la JList**: se implementa un ListCellRenderer que mejora la visualización de los archivos descargados
- **Validación mejorada de rutas**: el PreferencesPanel comprueba la existencia de las rutas de yt-dlp, ffmpeg y la carpeta de salida antes de ejecutar el proceso
- **Gestión sincronizada**: entre JList, JComboBox y JTable para mantener la coherencia entre las vistas y los detalles de la biblioteca multimedia

## 🧱 Estructura actual de la aplicación

### Ventanas y paneles

#### 🪟 MainFrame (ventana principal)

- Contiene el menú superior (File, Edit, Help)
- Desde Edit > Preferences se abre el panel de configuración
- Permite introducir una URL y ejecutar la descarga mediante yt-dlp
- Incluye un área de texto (`txaLogArea`) para mostrar los logs en tiempo real
- Incorpora una JList y un JTable que muestran los archivos descargados

#### ⚙️ PreferencesPanel (panel de preferencias)

**Permite definir rutas de:**
- yt-dlp
- ffmpeg
- Carpeta de salida

**Opciones adicionales:**
- Límite de velocidad
- Creación de .m3u para playlists

**Funcionalidades:**
- Botones Browse que usan JFileChooser para seleccionar archivos o carpetas
- Botón Volver, que devuelve al panel principal sin crear nuevas instancias

#### 💡 AboutDialog (pendiente de implementación)

Modal JDialog que mostrará:
- Autor
- Curso
- Recursos utilizados

## ⚙️ Lógica implementada

### 🔹 Ejecución de yt-dlp

- Construcción dinámica del comando con rutas y flags personalizados
- Ejecución asíncrona mediante `SwingWorker` y `ProcessBuilder`
- Lectura en tiempo real de la salida estándar, mostrando el progreso en el log
- Gestión de interrupción de descarga mediante botón Stop

### 🔹 CommandExecutor

Clase utilitaria (`cat.dam.roig.cleanstream.utils.CommandExecutor`) encargada de:
- Ejecutar el proceso externo
- Leer su salida línea a línea
- Pasar cada línea a la interfaz mediante un `Consumer<String>`
- Aplicar opciones de estabilidad y compatibilidad con YouTube

### 🔹 Gestión de archivos descargados

Nueva clase `ResourceDownloaded` con los campos:
```java
private String name;
private String route;
private long size;
private String mimeType;
private LocalDateTime downloadDate;
private String extension;
```

- Clase `DownloadsScanner` que recorre la carpeta configurada y devuelve una lista de objetos `ResourceDownloaded`
- Integración con los componentes de la interfaz (JList, JComboBox, JTable)
- Renderizado personalizado en JList y sincronización de selección entre componentes

## 🧭 Instrucciones de uso

### 1. Configurar rutas

1. Abrir **Edit > Preferences** y establecer las rutas de yt-dlp, ffmpeg y la carpeta de salida
2. Guardar los cambios con el botón **Volver**

### 2. Descargar contenido

1. Introducir la URL del vídeo o playlist en el campo principal
2. Pulsar **Download** para iniciar el proceso
3. Observar el progreso en tiempo real en el área de logs

### 3. Detener descarga

- Pulsar el botón **Stop** para interrumpir la descarga en curso

### 4. Consultar la biblioteca multimedia

- Visualizar los archivos descargados desde la JList o JTable
- Filtrar resultados mediante la JComboBox

### 5. Generar playlists

- Al finalizar las descargas, se creará automáticamente un archivo `.m3u` en la carpeta de destino

## 🧠 Estado actual del proyecto

### ✅ Completado

- Interfaz gráfica funcional (JFrame + JPanel)
- Menú con navegación y panel de preferencias
- Ejecución real de yt-dlp con logs en tiempo real
- Botón Stop funcional
- Carga de archivos descargados y visualización en JList/JTable
- Creación automática de listas .m3u
- Validación de campos y control básico de errores

### 🚧 Pendiente

- Refinar renderizado visual con ListCellRenderer avanzado y estilos coherentes
- Integrar **PO-Token generator** para obtener calidades superiores en descargas futuras
- Ampliar el sistema de descargas con **más opciones de formato**, incluyendo audio de alta calidad y combinaciones personalizadas de vídeo + audio  

> El proyecto se encuentra en fase estable de prototipo funcional, con base sólida para ampliaciones futuras.

## 🪛 Problemas encontrados y soluciones

| Problema | Causa | Solución aplicada |
|----------|-------|-------------------|
| Paneles superpuestos al iniciar | Ambos añadidos al contentPane desde el Designer | Se controló la visibilidad en el constructor de MainFrame |
| Congelamiento al ejecutar yt-dlp | Ejecución en el hilo principal | Implementación de SwingWorker con `publish()` |
| No se accedía a `txtYtDlpPath` desde MainFrame | Campo en otra clase | Getters públicos en PreferencesPanel |
| Error 403 al descargar de YouTube | Cambios en la API | Se añadieron flags: `--compat-options youtube-disable-po-token`, `--force-ipv4`, `--user-agent Mozilla/5.0` |
| CommandExecutor creaba nuevas ventanas ocultas | Inicializaba MainFrame internamente | Se eliminó la dependencia, ahora es una clase utilitaria |
| Detección de calidad no funcional con PO-Tokens | Incompatibilidad con yt-dlp actual | Se documentó la limitación y se aplicó una alternativa con detección final por log |

> El proyecto ha sido probado en **Linux Manjaro**, ejecutando binarios locales de yt-dlp y ffmpeg, confirmando compatibilidad y estabilidad del sistema.

## 📚 Recursos y referencias

### Oficiales y docentes

- Enunciado Tarea para DI01_1 25-26
- Enunciado Tarea para DI01_2 25-26
- DI01 Support Notes 25-26
- [Documentación oficial de yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [Documentación oficial de ffmpeg](https://ffmpeg.org/)

### Consultas externas y soporte

- **ChatGPT** (modelo GPT-5, OpenAI): resolución de errores, documentación y guía de implementación
- **StackOverflow**: ejemplos sobre ProcessBuilder, SwingWorker y AbstractTableModel
- Pruebas realizadas en **Linux Manjaro**, ejecutando binarios locales de yt-dlp y ffmpeg

## 👨‍💻 Créditos

- **Autor**: Elias Roig
- **Asistencia técnica y documentación**: ChatGPT (OpenAI GPT-5)
- **Curso**: Desarrollo de Interfaces — FP DAM 2025-26

---

## 🚀 Instalación y uso
```bash
# Clonar el repositorio
git clone https://github.com/tu-usuario/cleanstream.git

# Abrir el proyecto en NetBeans 27 con JDK 24
# Compilar y ejecutar
```

### Requisitos previos

- **NetBeans 27** o superior
- **JDK 24**
- **yt-dlp** instalado en el sistema
- **ffmpeg** instalado en el sistema

## 📝 Licencia

Este proyecto es de uso educativo para el curso de Desarrollo de Interfaces.
