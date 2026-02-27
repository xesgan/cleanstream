# 🧩 CleanStream

> Advanced Java Swing Multimedia Manager with Cloud Synchronization

**Autor:** Elias Roig  
**Módulo:** Desarrollo de Interfaces — FP DAM 2025-26  
**Entregas:** DI01 · DI01_2 · DI03 · DI04 · DI06

---

## 📸 Preview

*(Añadir aquí una captura principal de la aplicación)*

---

## 🚀 Descripción General

**CleanStream** es una aplicación de escritorio desarrollada en **Java Swing (JDK 24)** que proporciona:

- 🎬 Descarga de medios mediante `yt-dlp`
- ☁ Sincronización con la **DI Media NET API**
- 📚 Gestión avanzada de biblioteca local
- 🔌 Componente JavaBean personalizado para polling automático
- 🎨 Rediseño UX completo siguiendo principios profesionales

El proyecto ha evolucionado desde un prototipo GUI básico hasta una aplicación modular sincronizada con la nube y optimizada en experiencia de usuario.

---

## 🏗️ Arquitectura

CleanStream sigue una arquitectura modular en capas:

```text
cleanstream
├── app          → Punto de entrada de la aplicación
├── controller   → Lógica de control y orquestación de eventos
├── models       → POJOs y TableModels
├── services     → Lógica de negocio (yt-dlp, escaneo, procesamiento)
├── ui           → Componentes Swing
│   ├── panels
│   ├── dialogs
│   └── renderers
└── utils        → Clases auxiliares y constantes
```

### Decisiones Arquitectónicas

- Separación de la lógica de API en un **JavaBean independiente**
- Instancia única del componente de polling
- Uso de `SwingWorker` para evitar bloqueo de la EDT
- Implementación de `AbstractTableModel` para mayor flexibilidad

---

## ☁ Integración Cloud

La aplicación se integra con la **DI Media NET REST API**:

- `POST /api/Auth/login`
- `GET /api/Files/all`
- `POST /api/Files/upload`
- `GET /api/Users/me`

Autenticación mediante **JWT (72h de validez)** con opción "Remember Me".

---

## 🎨 Sección UX (DI04)

Rediseño basado en principios de usabilidad:

### ✔ Consistencia
Tema oscuro coherente, iconografía uniforme y espaciado consistente.

### ✔ Feedback
Barras de progreso reales, botones contextualmente habilitados y etiquetas de estado.

### ✔ Restricciones
Acciones bloqueadas cuando el estado no es válido.

### ✔ Recuperabilidad
Confirmación de Logout y manejo claro de errores.

### ✔ Mínima Sorpresa
Identificación clara de estados: LOCAL / CLOUD / BOTH.

---

## 🛠️ Tecnologías Utilizadas

| Área | Tecnología |
|------|------------|
| Lenguaje | Java 24 |
| UI | Swing |
| Build | Maven |
| JSON | Jackson Databind 3.0.0 |
| HTTP | java.net.http.HttpClient |
| CLI | yt-dlp, ffmpeg, ffprobe |
| Concurrencia | SwingWorker |

---

## 📦 Instalación

### 1️⃣ Clonar el repositorio

```bash
git clone https://github.com/xesgan/cleanstream.git
```

### 2️⃣ Requisitos

- JDK 24
- NetBeans 27/28
- yt-dlp instalado en el sistema
- ffmpeg y ffprobe instalados

### 3️⃣ Compilar

```bash
mvn clean package
```

La documentación Javadoc se genera automáticamente en:

```
/doc
```

---

## 📚 Documentación Técnica

La documentación Javadoc se genera automáticamente mediante `maven-javadoc-plugin` y se encuentra en:

```
/doc/index.html
```

Incluye clases, métodos y propiedades relevantes del sistema.

---

## 🧠 Aprendizajes Clave

- Gestión correcta de eventos Swing para evitar disparos duplicados.
- Implementación de eventos personalizados sin `PropertyChangeSupport`.
- Uso adecuado de hilos para mantener la UI fluida.
- Importancia crítica del rediseño UX en aplicaciones desktop.

---

## 📌 Futuras Mejoras

- Paginación en bibliotecas grandes
- Búsqueda avanzada con expresiones regulares
- Drag & Drop para subida de archivos
- Métricas de rendimiento
- Selector Dark / Light Theme

---

## 📚 Recursos Externos

- Documentación oficial de yt-dlp
- Documentación del proyecto Jackson
- StackOverflow (eventos Swing y eventos personalizados)
- IA (ChatGPT) para revisión arquitectónica y mejoras UX

Todo el código ha sido adaptado, comprendido y documentado íntegramente.

---

## 🏁 Estado Actual

- [x] Cumple requisitos DI01, DI01_2, DI03 y DI04
- [x] Arquitectura modular limpia
- [x] Componente independiente funcional
- [x] Documentación Javadoc generada automáticamente
- [x] Repositorio público y listo para revisión

---

## 📜 Licencia

Proyecto educativo para el módulo Desarrollo de Interfaces — FP DAM.