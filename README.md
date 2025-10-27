🧩 CleanStream — Elias Roig

Desarrollo de Interfaces — Semana 2

📋 Descripción general

CleanStream es una aplicación de escritorio creada en Java Swing con NetBeans 27 y JDK 24, que actúa como interfaz gráfica para la herramienta yt-dlp.
El objetivo es facilitar la descarga de vídeos o audios desde plataformas online, con un diseño simple y un flujo de uso claro.

Durante esta segunda semana, el foco ha estado en el diseño funcional de la interfaz, la navegación entre paneles, y la ejecución real de comandos yt-dlp mediante ProcessBuilder y SwingWorker.

🧱 Estructura actual de la aplicación
Ventanas y paneles

MainFrame (ventana principal)

Contiene el menú superior (File, Edit, Help).

Desde “Edit > Preferences” se abre el panel de configuración.

Desde aquí se introduce la URL del vídeo y se ejecuta la descarga.

Tiene un área de texto (txaLogArea) que muestra los logs en tiempo real.

PreferencesPanel (panel de preferencias)

Permite definir rutas para yt-dlp, ffmpeg, carpeta de salida y opciones como:

Límite de velocidad

Crear .m3u para playlists

Incluye botones Browse que abren un JFileChooser para seleccionar archivos o carpetas.

Tiene un botón Volver que devuelve al panel principal.

AboutDialog (pendiente de implementar)

Será el cuadro modal con información del autor, curso y recursos utilizados.

⚙️ Lógica implementada

Ejecución de yt-dlp

Se construye el comando dinámicamente con las opciones básicas y rutas configuradas.

Se ejecuta en segundo plano usando SwingWorker, evitando que la interfaz se congele.

Las líneas de salida se muestran en tiempo real en el área de log.

CommandExecutor

Clase utilitaria (cat.dam.roig.cleanstream.utils.CommandExecutor) encargada de ejecutar el proceso y leer su salida.

Implementa un Consumer<String> para procesar cada línea y mostrarla en la interfaz.

Validación de campos

Antes de ejecutar, se comprueba que haya ruta de yt-dlp y una URL válida.

Navegación entre paneles

Funcionalidad completa entre MainFrame y PreferencesPanel sin duplicar instancias.

Uso de setVisible(true/false) para alternar vistas.

🧩 Problemas encontrados y soluciones aplicadas
Problema	Causa	Solución aplicada
Al abrir la app se mostraban ambos paneles superpuestos	NetBeans añadía ambos paneles al contentPane desde el Designer	Se añadió control de visibilidad en el constructor del MainFrame
La app se congelaba al ejecutar yt-dlp	El proceso se ejecutaba en el hilo principal	Se implementó SwingWorker con publish() para lectura asíncrona
No se podía acceder al txtYtDlpPath desde el MainFrame	El campo estaba en otra clase (PreferencesPanel)	Se añadieron getters públicos para obtener los valores
Error 403 al descargar vídeos de YouTube	Cambios recientes en la API de YouTube	Se añadieron flags como --compat-options youtube-disable-po-token, --force-ipv4, --user-agent Mozilla/5.0
CommandExecutor creaba nuevas ventanas ocultas	Inicializaba MainFrame dentro de la clase	Se eliminó esa dependencia y se simplificó a una clase utilitaria pura
🧠 Estado actual del proyecto

✅ Interfaz gráfica funcional (JFrame + JPanel)
✅ Menú con navegación y panel de preferencias
✅ Ejecución de yt-dlp real desde Swing
✅ Logs en tiempo real
✅ Control de errores básicos

🚧 Pendiente para siguientes semanas:

Incorporar más opciones de descarga (audio, listas, subtítulos, etc.).

🤖 Créditos y fuentes

Autor: Elias Roig

Asistencia técnica y documentación: ChatGPT (modelo GPT-5, OpenAI)

Recursos consultados:

yt-dlp GitHub

Documentación oficial de ffmpeg

Apuntes “DI01 Support Notes 25-26”

Enunciado oficial “Tarea para DI01_1 25-26”

Varias pruebas de consola y ejecución en Linux (Manjaro)
