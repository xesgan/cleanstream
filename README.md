# 🧩 CleanStream

**Desarrollo de Interfaces — Semanas 2 y 3 (DI01_1 + DI01_2)**

## 📋 Descripción general

CleanStream es una aplicación de escritorio desarrollada en Java Swing con NetBeans 27 y JDK 24, que actúa como interfaz gráfica para la herramienta yt-dlp.

Su objetivo es ofrecer una interfaz limpia y funcional para descargar vídeos o audios desde plataformas online, con configuración personalizable, ejecución en segundo plano y registro visual del proceso.

- **Semana 2 (DI01_1)**: Implementación de la estructura principal, la interfaz gráfica y la ejecución asíncrona de comandos.
- **Semana 3 (DI01_2)**: Ampliación con funcionalidades de gestión de la biblioteca multimedia, utilizando JList, JComboBox y JTable con modelos personalizados.

## 🧱 Estructura actual de la aplicación

### Ventanas y paneles

#### 🪟 MainFrame (ventana principal)

- Contiene el menú superior (File, Edit, Help)
- Desde Edit > Preferences se abre el panel de configuración
- Permite introducir una URL y ejecutar la descarga mediante yt-dlp
- Incluye un área de texto (`txaLogArea`) para mostrar los logs en tiempo real
- Incorpora una JList y un JTable que muestran los archivos descargados

#### ⚙️ PreferencesPanel (panel de preferencias)

Permite definir rutas de:
- yt-dlp
- ffmpeg
- Carpeta de salida

Opciones adicionales:
- Límite de velocidad
- Creación de .m3u para playlists

Funcionalidades:
- Botones Browse que usan JFileChooser para seleccionar archivos o carpetas
- Botón Volver, que devuelve al panel principal sin crear nuevas instancias

#### 💡 AboutDialog (pendiente de implementación)

Modal JDialog que mostrará:
- Autor
- Curso
- Recursos utilizados

## 🧩 Componentes añadidos (Semana 3)

| Componente | Uso principal | Modelo asociado | Descripción |
|-----------|---------------|-----------------|-------------|
| `JList<ResourceDownloaded>` | Listado rápido de recursos descargados | `DefaultListModel` | Permite visualizar los archivos descargados y seleccionar uno |
| `JComboBox<String>` | Filtro o categoría de archivos | `DefaultComboBoxModel` | Filtra los resultados por tipo o extensión |
| `JTable` | Tabla principal de la biblioteca | `AbstractTableModel` | Muestra los detalles: nombre, tamaño, fecha, tipo MIME y ruta |

Cada componente responde a eventos de selección (`ListSelectionListener`, `ActionListener`) que sincronizan la información entre la lista, la tabla y el área de detalles.

## ⚙️ Lógica implementada

### 🔹 Ejecución de yt-dlp

- Construcción dinámica del comando con rutas y flags personalizados
- Ejecución asíncrona mediante `SwingWorker` y `ProcessBuilder`
- Lectura en tiempo real de la salida estándar, mostrando el progreso en el log

### 🔹 CommandExecutor

Clase utilitaria (`cat.dam.roig.cleanstream.utils.CommandExecutor`) encargada de:
- Ejecutar el proceso externo
- Leer su salida línea a línea
- Pasar cada línea a la interfaz mediante un `Consumer<String>`

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

## 🧠 Estado actual del proyecto

### ✅ Completado

- Interfaz gráfica funcional (JFrame + JPanel)
- Menú con navegación y panel de preferencias
- Ejecución real de yt-dlp con logs en tiempo real
- Carga de archivos descargados y visualización en JList/JTable
- Validación de campos y control básico de errores

### 🚧 Pendiente

- Implementar AboutDialog modal
- Agregar funciones extra (descarga de audio, subtítulos, gestión de eliminación)
- Refinar renderizado visual con ListCellRenderer y estilos coherentes

## 🪛 Problemas encontrados y soluciones

| Problema | Causa | Solución aplicada |
|----------|-------|-------------------|
| Paneles superpuestos al iniciar | Ambos añadidos al contentPane desde el Designer | Se controló la visibilidad en el constructor de MainFrame |
| Congelamiento al ejecutar yt-dlp | Ejecución en el hilo principal | Implementación de SwingWorker con `publish()` |
| No se accedía a `txtYtDlpPath` desde MainFrame | Campo en otra clase | Getters públicos en PreferencesPanel |
| Error 403 al descargar de YouTube | Cambios en la API | Se añadieron flags: `--compat-options youtube-disable-po-token`, `--force-ipv4`, `--user-agent Mozilla/5.0` |
| CommandExecutor creaba nuevas ventanas ocultas | Inicializaba MainFrame internamente | Se eliminó la dependencia, ahora es una clase utilitaria |

## 📚 Recursos y referencias

### Oficiales y docentes

- Enunciado Tarea para DI01_1 25-26
- Enunciado Tarea para DI01_2 25-26
- DI01 Support Notes 25-26
- [Documentación oficial de yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [Documentación oficial de ffmpeg](https://ffmpeg.org/)

### Consultas externas y soporte

- ChatGPT (modelo GPT-5, OpenAI): resolución de errores, documentación y guía de implementación
- StackOverflow: ejemplos sobre ProcessBuilder, SwingWorker y AbstractTableModel
- Pruebas realizadas en Linux Manjaro, ejecutando binarios locales de yt-dlp y ffmpeg

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

## 📝 Licencia

Este proyecto es de uso educativo para el curso de Desarrollo de Interfaces.
