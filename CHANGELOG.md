# CHANGELOG — PLAYSTREAM by neganboy (fork de Momiji 1.1.3)

Estado general: **nada de esto está compilado ni probado.** En el entorno donde se hizo no hay Android SDK/NDK/Gradle ni red, y `third-party/` viene vacío en el zip (ver `ANALYSIS.md` §0). Lo que sí se comprobó con scripts: que el XML de recursos es válido, que `values-es` tiene exactamente las mismas claves y placeholders que `values`, y que no quedan referencias a `metallic` fuera de `android/build.log`.

## Hecho en esta entrega

### Rediseño del inicio, opción A (v1.1.5, 2026-11-23)
- `res/layout/item_display_host.xml`: tarjeta grande con la PS5 más grande (arte 160dp, imagen 76dp), borde con el color del tema, nombre y estado centrados ("En reposo • 192.168.1.230"), chips de calidad (Competitivo/Equilibrada/Nítida/Ultra) y botón grande de jugar a todo lo ancho con el color del tema.
- `DisplayHostRecyclerViewAdapter`: los chips ponen la calidad de ESA consola (`setConsoleQuality` por MAC) y el chip elegido va relleno con el color del tema; el estado ya no va en una etiqueta sobre el arte sino en la línea centrada, como la vista previa A.
- `res/layout/activity_main.xml`: cabecera con "by neganboy • PS5 Remote Play" bajo PLAYSTREAM, y barra inferior de acceso rápido (Añadir consola / Ajustes / Soporte) en vez del botón flotante.
- `MainActivity`: la barra inferior abre añadir consola, Ajustes y la página de soporte; se quitó el botón flotante y su animación al desplazar.
- Versión 1.1.5 (vc 27). GameSir X5 Lite: DESCARTADO por Alejandro, no se hace el perfil 5E.

### 5B — Temas (v1.1.4, 2026-11-23)
- `common/Preferences.kt`: nuevo `Theme` (NEON_BLUE por defecto, VIOLET, GOLD), guardado en `app_theme`.
- `res/values/styles.xml`: `AppTheme.NeonBlue` (cian #00E5FF, fondo #0A1418), `AppTheme.Violet` (#B388FF) y `AppTheme.Gold` (#FFC94D), cada uno con su fondo y sus superficies.
- `common/Theme.kt` (nuevo): `applyPlaystreamTheme()` se llama al inicio de `onCreate` en Main, Settings, registro y consolas manuales, antes de `super.onCreate`. `MainActivity` se recrea en `onStart` si el tema cambió en Ajustes.
- Selector en Ajustes > General: `Tema` (ListPreference, `app_theme`), con sus textos en inglés y español.
- PS5 con glow del tema: `console_art_standby_neon/violet/gold.xml` (gradiente radial con el color del tema) y `DisplayHostRecyclerViewAdapter` elige el de la consola en reposo según el tema. En reposo la PS5 se ve con el glow cian/violeta/dorado, como la vista previa aprobada.
- Versión 1.1.4 (vc 26). El stream no cambia de tema: sigue negro.

### Renombre a PLAYSTREAM (sección 0 del encargo)
- `android/app/build.gradle`: `namespace` y `applicationId` → `com.neganboy.playstream` (antes `com.metallic.chiaki` / `io.github.yazanj082.momiji`). Por qué: renombre pedido; la app queda como app distinta.
- `android/app/src/main/java/com/metallic/chiaki/` movido a `.../com/neganboy/playstream/` y 88 archivos (Kotlin, layouts, `preferences.xml`, proguard) con `com.metallic.chiaki` → `com.neganboy.playstream`.
- `android/app/src/main/cpp/chiaki-jni.h`: `BASE_PACKAGE` y `JNI_FCN` con el paquete nuevo. **Esto es C, no solo Kotlin/XML**: sin este cambio la app falla con `UnsatisfiedLinkError`. No cambia ninguna lógica; obliga a recompilar `libchiaki-jni.so`.
- `res/values/strings.xml` y `values-ar/strings.xml`: `app_name` = `PLAYSTREAM`; "Momiji" → "PLAYSTREAM" en los textos visibles. "Momiji" queda solo en los créditos.
- Acerca de (`preferences_about_version`): "PLAYSTREAM by neganboy · Versión …". `preferences_about_credits_summary`: dice que es un fork de Momiji de yazanj082 (con enlace) bajo AGPL-3.0, y que no está afiliado a Sony. (en/es/ar)
- `README.md`: cabecera de PLAYSTREAM con crédito, licencia y enlace al original; el README de Momiji se conserva debajo. `COPYING` y `LICENSES/` intactos.
- **Pendiente / aviso:** `feedback_email` sigue apuntando al correo del autor original (`momiji.yallav@gmail.com`); no puse otro porque no sé cuál quiere Alejandro. Los enlaces de apoyo/donación (`support_url`) y `issues_url` también siguen apuntando a yazanj082. Icono: **sin cambios** (sigue el de Momiji); no hice un icono nuevo.

### 5A — Español
- `res/values-es/strings.xml` (nuevo): las **357** strings traducibles, más el `plurals` y el `string-array` de temperatura. `res/values-es/arrays.xml` (nuevo): 21 nombres de botones. Español neutro, "control" en vez de "mando".
- Verificado por script: mismas claves, mismos placeholders (`%s`, `%1$d`, `%%`…) y mismos `\n` que el original. Las 41 strings `translatable="false"` no se traducen (nombres, claves).
- Revisión pendiente: una lectura humana del tono; la traducción es mía, sin revisor externo.
- Sin textos en inglés escritos a mano en layouts ni en `Toast`/`setText` de Kotlin (búsqueda por `grep`).

### 5C — Calidad
- `common/Preferences.kt`:
  - Nuevos presets en `Quality`: **Competitivo** (720p 60 fps, H.264, 15 000 kbps), **Historia** (1080p 60 fps, H.265, 25 000 kbps en PS5 / 15 000 en PS4), **Ultra 50** (1080p 60, 50 000 kbps) y **Ultra 100** (1080p 60, 100 000 kbps). Suave/Equilibrada/Nítida no se tocaron.
  - Ultra usa el códec elegido en Ajustes (por defecto H.265); Historia fuerza H.265; Competitivo fuerza H.264.
  - `Quality.localOnly` (Ultra) y `effectiveQuality(...)`: **fuera de casa un Ultra cae a Nítida**, y la lista "Calidad fuera de casa" (`SettingsInternetPlayFragment.kt`) ya no ofrece Ultra.
  - `RenderPath` + `Quality.renderPath`: Competitivo ⇒ `DIRECT` (SurfaceView sin shaders); Historia ⇒ `UPSCALE`.
  - Códec **H.265 HDR** (`Codec.CODEC_H265_HDR`, ya existente en `lib/Chiaki.kt`) expuesto en Ajustes. `isHdrDecodingSupported()` consulta `MediaCodecList` por HEVC Main10; si no hay, `codecEffective` pide H.265 normal y `MainActivity` muestra un aviso (`codec_hdr_fallback`). Sin crash.
  - `sharpnessPercent` (entero 0-100, misma clave que `sharpnessIntensity`, sin errores de redondeo del `Float`).
- `settings/SettingsFragment.kt`: `DataStore.getInt/putInt` ahora leen/guardan la nitidez (antes devolvían siempre el valor por defecto y no guardaban nada: un slider no habría funcionado).
- `res/xml/preferences.xml`: `SeekBarPreference` de **Nitidez** (0-100) bajo Códec. Solo actúa cuando hay shaders (suavizado de degradados o superresolución); el texto del ajuste lo dice.
- `main/MainActivity.kt` (`startStream`) y `stream/StreamActivity.kt` (`setupVideoOutput`): nuevo extra `EXTRA_RENDER_PATH`; Competitivo ignora debanding/superresolución y usa SurfaceView directo; Historia fuerza superresolución (si el dispositivo la soporta).
- Strings nuevas (en/es/ar): nombres y resúmenes de presets, `H265 HDR`, aviso de HDR, Nitidez. **Árabe escrito por mí, sin revisar.**
- **No verificado:** que el HDR se vea como HDR. El decodificador nativo trata `H265_HDR` como HEVC normal (`video-decoder.c:149-150`) y no veo código que configure metadatos HDR en la superficie; en pantalla SDR puede verse lavado. Que Ultra 100 Mbps no pierda paquetes (ver `ANALYSIS.md` §2.2).

## NO hecho en esta entrega (pendiente, sin código)
- **5B** selector de tema (4 temas), tarjetas de consola nuevas ("Listo/En reposo", última sesión), quitar pie de soporte, icono propio. Solo se hizo la marca (nombre + "by neganboy" en Acerca de). La pantalla principal todavía no muestra "PLAYSTREAM — by neganboy" aparte del nombre de la app.
- **5D** calidad automática. Además, con lo que existe **no se puede cambiar bitrate ni resolución en caliente** (ver `ANALYSIS.md` §4); solo cabe "recomendar para la próxima conexión".
- **5E** perfil GameSir X5 Lite y editor táctil. No tengo el mapeo real de teclas de ese control; inventarlo sería adivinar.
- **5F** Modo Prueba / "Buscar mi mejor configuración". Con las estadísticas actuales no hay ping en vivo, ni frames recuperados por FEC, ni cortes de audio (ver `ANALYSIS.md` §4); el informe saldría incompleto sin cambios nativos.
- **Fase 2**: solo valorada en `ANALYSIS.md` §6, nada implementado.
