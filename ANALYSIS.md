# ANALYSIS.md — PLAYSTREAM by neganboy (fork de Momiji 1.1.3)

Fecha: 2026-11-23. Fuente analizada: `momiji-1_1_3-source.zip` (599 entradas, sin `.git`).
Todo lo de abajo sale de leer el código. Lo que no pude medir ni compilar está marcado como **NO VERIFICADO**.

## 0. Entorno de esta entrega (importante)

- **No pude compilar.** En este entorno no hay Android SDK, ni NDK, ni Gradle, ni CMake, y no hay red. Solo hay JDK 21.
- `third-party/` viene **vacío** en el zip (`mbedtls`, `json-c`, `miniupnp`, `curl`, `nanopb`, `jerasure`, `gf-complete` son carpetas vacías: son submódulos de git y el zip no los incluye). `build.gradle` usa `FETCHCONTENT_FULLY_DISCONNECTED=ON` apuntando a esas carpetas, así que **el build nativo falla sin ejecutar antes `git submodule update --init`** en una máquina con red.
- Hay un `android/build.log` en el zip que no es de este proyecto (rutas `/Users/mark/antigravity-projects/chiaki-ng/...`, con un error de `sharedPreferences` privado). Lo dejé sin tocar; conviene borrarlo.
- Por eso, **ningún cambio de esta entrega está compilado ni probado en el teléfono**. Ver `CHANGELOG.md` para el estado de cada cambio.

## 1. Confirmación / corrección de la sección 3 del encargo

| Dato del encargo | Resultado | Evidencia |
|---|---|---|
| Chiaki 1.9.9 | Confirmado | `CMakeLists.txt:23-26` (1.9.9) |
| 80 Kotlin, 162 XML | Confirmado | `find` |
| namespace `com.metallic.chiaki`, compileSdk 36, NDK 26.1.10909125, CMake 3.22.1, Java 17, flavors fdroid/play | Confirmado | `android/app/build.gradle` |
| `applicationId` original | `io.github.yazanj082.momiji` (el encargo dice versionCode 25 / minSdk 24 / targetSdk 36: confirmado) | `build.gradle:29-33` |
| 399 strings | **Corrección: 398** `<string>` en `values/strings.xml`, de las cuales 41 son `translatable="false"` → **357 traducibles** (igual que `values-ar`, 357). Además hay 1 `<plurals>` (`test_rumble_motors`), 1 `<string-array>` en `strings.xml` (`stream_stats_thermal_levels`) y 2 arrays en `values/arrays.xml` (`mapping_button_entries/values`, 42 ítems en total) | `grep` |
| Ping se mide y se usa en vivo | **Corrección importante: el ping NO es en vivo.** `pingMs` es `session.rtt_us / 1000`, el promedio de la prueba RTT que se hace una sola vez al conectar (`senkusha.c:385`, `chiaki-jni.c:773`). Durante la partida es un número constante | `lib/Chiaki.kt:484-485` ("Measured when connecting") |
| Estadísticas "nunca se usan para cambiar nada" | Confirmado | `StreamActivity.kt:612-642` solo las pinta |
| `sessionGetStats` | **Matiz: tiene estado.** La pérdida es "desde la última llamada" y `frameGapMsMax` se pone a 0 al leerlo (`chiaki-jni.c:759-781`). **Solo puede haber un lector** (si el controlador automático y el Modo Prueba lo llaman a la vez, se roban datos) | `chiaki-jni.c` |
| Congestion control intervalo fijo 200 ms | Confirmado | `congestioncontrol.c:5` |
| `packet_loss_max = 0.05` fijo | Confirmado | `chiaki-jni.c:494`. Además `video_profile_auto_downgrade = true` (`:491`), pero solo baja 1080p→720p y códec en **PS4 normal** (`ctrl.c:1384-1400`); en PS5 no hace nada |
| `TAKION_A_RWND = 0x19000` (~100 KB) | Confirmado: 0x19000 = **102 400 bytes** (`takion.c:38`), usado en `SO_RCVBUF` (`:247`, `:335`) | |
| Reorder queue 16 paquetes | Confirmado (`takion.c:42`) | |
| `IP_DONTFRAGMENT`/`IP_MTU_DISCOVER` y sin DSCP | Confirmado; no hay `IP_TOS` ni `DSCP` en `lib/src` | `grep` |
| FEC mínimo 1 unidad por frame | Confirmado (`frameprocessor.c:78-79`) | |
| Decodificador: low-latency, priority 0, switches Qualcomm, timeout 10 ms | Confirmado (`video-decoder.c:16,101-109,273`). Hay además `vendor.rtc-ext-dec-low-latency.enable` y `vendor.low-latency.enable` | |
| Audio 32 chunks × 1024 B, catch-up a 20 ms cada 4 s | Confirmado (`audio-output.cpp:18-19,26-28`). Los "~170 ms" son **mi cálculo asumiendo 48 kHz, estéreo, 16 bit** (32 768 B / 192 000 B/s); el formato real no lo verifiqué | |
| `CODEC_H265_HDR(2)` sin botón | Confirmado (`Chiaki.kt:47`). El enum de Ajustes (`Preferences.Codec`) solo tiene H264/H265 | |
| `sharpnessIntensity` sin slider | Confirmado (`Preferences.kt:197-200`; se pasa a `VideoRenderer` en `StreamActivity.kt:279`) | |
| **"Si no hay más remedio, ajusta el bitrate vía el feedback de congestión que ya existe"** | **Falso en la práctica.** El único "feedback" es el paquete de congestión que manda la librería (C) con pérdida recortada a ≤5 %. **No existe ninguna función JNI ni Kotlin para cambiar el bitrate o la resolución con la sesión abierta.** El bitrate/resolución van fijos en `connect_info.video_profile` al conectar (`chiaki-jni.c:484`) | ver §4 |
| "Fase 1 solo Kotlin/XML" y renombre completo | **Contradicción:** el renombre del paquete obliga a tocar `android/app/src/main/cpp/chiaki-jni.h` (`BASE_PACKAGE` y `JNI_FCN`, que contienen `com_metallic_chiaki`). Sin eso la app crashea con `UnsatisfiedLinkError`. Ese archivo **no** está en la lista prohibida de 5G, así que lo cambié (2 líneas) y forzosamente hay que recompilar `libchiaki-jni.so` | `chiaki-jni.h:8-9` |
| `feedback_email` | **Aviso:** apunta al correo del desarrollador original (`momiji.yallav@gmail.com`, `strings.xml:323`). Los comentarios de PLAYSTREAM le llegarían a yazanj082. No lo cambié porque no sé qué correo quiere Alejandro | |

## 2. Cuellos de botella para red local (por razonamiento, **NO medidos**)

No hay forma de medir latencia real desde el código; el orden es por impacto esperado, con los números que sí salen del código. Lo más probable es que el mayor factor real sea el Wi-Fi mismo (aire, canal, ahorro de energía), que no es código nuestro.

1. **Reporte de pérdida: 200 ms y recorte al 5 %** (`congestioncontrol.c:5`, `chiaki-jni.c:494`). La consola se entera de una pérdida hasta 200 ms tarde (12 frames a 60 fps), y si pierdes más del 5 % la librería le dice "5 %". Con ráfagas en Wi-Fi esto retrasa la reacción de la consola.
2. **`SO_RCVBUF` de 102 400 B** (`takion.c:38`). A 25 Mbps (3,125 MB/s) eso son ~33 ms de datos; a 50 Mbps ~16 ms; a 100 Mbps **~8 ms** (cálculo mío). Una pausa de 8 ms del hilo de lectura a 100 Mbps ya puede desbordar el socket y perder paquetes en el propio teléfono. Android además puede limitar el valor real que concede el kernel (**NO VERIFICADO**).
3. **Reorder queue de 16 paquetes** (`takion.c:42`). Suponiendo paquetes de ~1400 B (el MTU real se negocia, **supuesto mío**), un frame a 25 Mbps/60 fps son ~37 paquetes y a 100 Mbps ~150. La cola es menor que un frame; con reordenamiento de Wi-Fi, paquetes que llegan tarde se cuentan como perdidos.
4. **FEC con mínimo 1 unidad por frame** (`frameprocessor.c:78-79`). Es sobrecarga fija que decide la consola; la app no la controla. Sin medida disponible.
5. **Timeout de entrada del decodificador de 10 ms** (`video-decoder.c:16,273`). Si el códec no tiene un buffer de entrada libre, el hilo espera hasta 10 ms (más de medio frame a 60 fps). Con el Snapdragon 8 Elite en baja latencia es poco probable que ocurra, pero es el peor caso. Menor: sin DSCP/QoS (`takion.c`), el router no prioriza el stream.

## 3. Qué se puede tocar sin recompilar la librería nativa

**Solo Kotlin/XML (no obliga a recompilar `.so`):** `Preferences.kt`, `res/xml/preferences.xml`, `strings.xml`/`values-es`, temas y colores, `MainActivity`/adaptadores/layouts, `StreamActivity` (lectura de stats ya expuestas), `ControllerProfiles.kt`, `touchcontrols/*`, `VideoRenderer.kt` (shaders GL), selección de códec.

**Obliga a recompilar `libchiaki-jni.so`:** cualquier cambio en `lib/src/*.c`, `chiaki-jni.c/.h`, `video-decoder.c`, `audio-output.cpp`. En particular **cambiar bitrate/resolución en caliente, exponer pérdida por FEC, huecos de audio, RTT en vivo, o la ventana/intervalo de congestión** requieren código C nuevo → Fase 2.

Nota: como el renombre ya toca `chiaki-jni.h`, **esta entrega ya requiere recompilar** el `.so`; pero no cambia ninguna lógica nativa.

## 4. Consecuencias para lo que pide la Fase 1

- **5D (bitrate adaptativo):** con lo que existe **no se puede bajar bitrate ni resolución durante la partida**. Lo honesto en Kotlin es un controlador que *observa* cada 1 s (pérdida, decode, huecos) y, al detectar problemas sostenidos, **guarda un escalón recomendado para la *próxima* conexión** (por consola, con el preset del usuario como techo). No hay cambio en caliente. El criterio `ping > 30 ms` del encargo **no es utilizable** en vivo porque el ping es constante (ver §1); solo sirve como dato de la conexión.
- **5F (Modo Prueba)** — lo que sí existe: pérdida %, bitrate medido, decode prom./máx., FPS renderizados (derivados de `framesRendered`), FPS mostrados, hueco máx. entre frames, nivel de buffer de audio. **Lo que NO existe en las estadísticas actuales** y por tanto no se puede informar sin código nativo: ping promedio/mín/máx en vivo (solo hay un valor medido al conectar), frames perdidos o recuperados por FEC, cortes de audio (solo nivel de buffer). Las llamadas a `getStats()` deben salir de **un único lector** compartido por el stream normal y el modo prueba.
- **Umbrales de veredicto:** los del encargo se usan tal cual. Con ping constante, "ping promedio" = el RTT de conexión.

## 5. Riesgos de los cambios (sin compilar)

| Cambio | Riesgo |
|---|---|
| Renombre de paquete | Un `import`/ruta de XML olvidado rompe el build; JNI mal renombrado = crash al abrir. Revisado con `grep` (0 restos de `metallic` fuera de `build.log`). Nuevo `applicationId` = app distinta, hay que re-registrar la consola |
| `values-es` | Un `%1$s`/apóstrofo sin escapar rompe el build de recursos (`aapt`). Plurals/arrays deben respetar el mismo número de ítems |
| Temas | Atributos Material3 faltantes dejan vistas sin color; el tema debe aplicarse antes de `setContentView` |
| H.265 HDR | Si el decodificador no lo soporta, la sesión puede fallar al conectar; hay que caer a H.265 con aviso. El render HDR en pantalla SDR puede verse lavado (**NO VERIFICADO**) |
| Slider de nitidez | `VideoRenderer` usa shader solo con debanding/super-resolución; sin ellos el slider no hace nada y debe indicarse |
| Presets Ultra | 50/100 Mbps pueden desbordar `SO_RCVBUF` (§2.2); es esperable que 100 Mbps pierda paquetes aun en LAN |
| Controlador adaptativo | Dos lectores de `getStats()` corrompen las lecturas |

## 6. Fase 2 propuesta (solo valoración, nada implementado)

1. **Congestión local 200→50 ms y `packet_loss_max` 0.05→0.01 local:** cambio pequeño en `congestioncontrol.c`/`chiaki-jni.c`. A 50 ms hay 4× más paquetes de control (insignificante). Riesgo: la librería ya "miente" recortando la pérdida; con 1 % la consola bajará bitrate antes. En LAN conviene reportar la real; en remoto, mantener el recorte. Requiere distinguir local/remoto en `connect_info`.
2. **FEC adaptativo:** el FEC lo fija la consola en el paquete (`units_in_frame_fec`); el cliente solo lo recibe. **No se puede "bajar el FEC" desde el cliente** sin un mecanismo del protocolo que no he visto en el código. Valoración: probablemente inviable; no recomendado.
3. **DSCP EF (46):** `setsockopt(IP_TOS, 0xB8)` en el socket UDP. Bajo riesgo de código; resultado depende del router (algunos lo ignoran, otros lo degradan) y de Android. Probar A/B.
4. **`SO_RCVBUF` 256 KB para Ultra 100 Mbps:** pasaría de ~8 ms a ~20 ms de margen a 100 Mbps (cálculo mío). Android puede capar el valor (`net.core.rmem_max`); hay que leer con `getsockopt` el valor concedido. Recomendado.
5. **`INPUT_BUFFER_TIMEOUT_MS` 10→0/1 ms:** si no hay buffer libre, el frame se descarta o reintenta en vez de esperar; en un códec lento provoca más frames descartados y artefactos. Solo con medición A/B.
6. **`CATCH_UP_KEEP_MS` 20→10 ms:** menos latencia de audio, más riesgo de underrun/chasquidos con Wi-Fi inestable. Solo en local.
7. **Chiaki 1.9.9 → chiaki-ng 1.10.1:** no he visto el código de chiaki-ng 1.10.1 en este entorno, así que **no puedo valorar con datos qué cambia ni qué se rompe**. El proyecto base ya incorpora parches propios (stats, catch-up de audio, etc.), por lo que es de esperar conflictos grandes en `lib/src` y el JNI. Trabajo estimado: alto (días), no verificado.

## 7. Estado de implementación — ver `CHANGELOG.md`
