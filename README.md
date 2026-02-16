# 🧩 CleanStream

**Autor:** Elias Roig  
**Módulo:** Desarrollo de Interfaces — FP DAM 2025-26  
**Entregas:** `DI01` · `DI01_2` · `DI03` · `DI04`

---

## 📋 Descripción del Proyecto
**CleanStream** es una aplicación de escritorio desarrollada en **Java Swing** que actúa como interfaz gráfica avanzada para la gestión multimedia. El proyecto ha evolucionado desde un prototipo GUI básico hasta una aplicación modular, sincronizada con la nube y centrada en la experiencia de usuario.

### Funcionalidades principales:
* 🎬 **Descarga de medios:** Integración con `yt-dlp`.
* ☁️ **Sincronización Cloud:** Conexión con la *DI Media NET API*.
* 📚 **Gestión Local:** Biblioteca multimedia con filtros avanzados.
* 🔄 **Componente JavaBean:** Integración de un componente personalizado para polling de datos.
* 🎨 **UX/UI Optimizada:** Mejora completa de usabilidad bajo principios de diseño profesional.

---

## 🛠️ Tecnologías utilizadas
* **IDE:** NetBeans 27 / 28
* **JDK:** 24
* **Build Tool:** Maven
* **GUI:** Java Swing (EDT, SwingWorker)
* **Networking:** `java.net.http.HttpClient`
* **JSON:** Jackson Databind 3.0.0
* **CLI Tools:** `yt-dlp`, `ffmpeg`, `ffprobe`

---

## 📌 Evolución por Unidades

### ✔️ DI01 — Prototipo inicial
* GUI creada con **NetBeans Designer** usando *Null layout*.
* Invocación del reproductor del sistema y gestión de preferencias.
* **Resolución de problemas:**
    | Problema | Solución |
    | :--- | :--- |
    | Bloqueo de UI al descargar | Uso de `ProcessBuilder` + `SwingWorker` |
    | Rutas inválidas | Validaciones previas y `JOptionPane` |
    | Acceso a recursos Maven | Uso correcto de `src/main/resources` |

### ✔️ DI01_2 — Gestión de Biblioteca
* Implementación de `JList`, `JComboBox` y `JTable` (vía `AbstractTableModel`).
* **Aprendizaje clave:** Gestión de eventos duplicados mediante `getValueIsAdjusting()`.

### ✔️ DI03 — Integración Cloud + JavaBean
* **Auth:** Login manual con persistencia de token JWT (72h) y "Remember Me".
* **Media Polling Component:** Creación de un componente independiente (JPanel) con `javax.swing.Timer` y eventos personalizados.
* **Arquitectura:** Eliminación de lógica de API del proyecto principal para delegarla en el componente.

### ✔️ DI04 — Mejora de Usabilidad y UX
Enfoque en los 5 pilares de diseño: *Colour & Style, Feedback, Affordance, Restricciones y Consistencia*.

---

## 🎨 Sección UX (Obligatoria DI04)

### 1️⃣ Aspecto, color e iconografía
* **Cambios:** Paleta de colores oscura coherente, iconos consistentes y tooltips descriptivos.
* **Justificación:** Aplicación de principios de **Consistencia** y **Mínima sorpresa** para reducir la carga cognitiva.

### 2️⃣ Affordance y Feedback
* **Implementación:** Botones deshabilitados contextualmente, barras de progreso reales, confirmación de Logout y spinners de carga.
* **Justificación:** Mejora la **Visibility** del sistema y permite la **Recuperabilidad** ante acciones accidentales.

### 3️⃣ Gestión de errores
* Manejo de errores HTTP 401 (Token expirado).
* Protección ante `NullPointerException` en el procesamiento de listas.
* Logs estructurados para depuración rápida.

---

## 🧱 Arquitectura del Proyecto

```text
cleanstream
├── app          # Punto de entrada
├── controller   # Lógica de control y eventos
├── models       # POJOs y Modelos de tablas
├── services     # Lógica de negocio y yt-dlp
├── ui           # Interfaz gráfica
│   ├── panels   # Paneles modulares
│   ├── dialogs  # Ventanas modales (About, etc)
│   └── renderers # Renderizado personalizado de celdas
└── utils        # Clases de apoyo y constantes
🔌 Integración con DI Media NET API
Endpoints principales consumidos por el componente JavaBean:

POST /api/Auth/login

GET /api/Users/me

GET /api/Files/all

POST /api/Files/upload

📚 Recursos externos utilizados
Documentación: yt-dlp, Jackson Project.

Comunidad: StackOverflow (Eventos JList, Custom Events en Swing).

IA (ChatGPT): Utilizada para asistencia conceptual, revisión de arquitectura y optimización de diseño UX. Todo el código ha sido adaptado y comprendido íntegramente.

🚀 Instalación y Uso
Clonar el repositorio:

Bash

git clone [https://github.com/xesgan/cleanstream.git](https://github.com/xesgan/cleanstream.git)
Abrir el proyecto en NetBeans 27/28.

Asegurarse de tener configurado el JDK 24.

Compilar con Maven para descargar las dependencias.

🏁 Estado final
[x] Cumple requisitos DI01, DI01_2, DI03 y DI04.

[x] Arquitectura modular y limpia.

[x] Componente independiente funcional.

[x] UX/UI profesional.

Licencia: Proyecto educativo para el módulo Desarrollo de Interfaces.