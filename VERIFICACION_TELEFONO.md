# Lista de verificación (Poco F8 Pro, PS5 por cable, Wi-Fi 5 GHz)

Antes: en una máquina con red, `git submodule update --init` y compilar con Gradle (compileSdk 36, NDK 26.1.10909125, CMake 3.22.1). Guarda el log completo de la compilación. Nada de esto se ha compilado todavía.

1. Instala el APK: aparece como **PLAYSTREAM**, paquete `com.neganboy.playstream`, junto a Momiji (no lo reemplaza). Registra la consola de nuevo.
2. Ajustes → Acerca de: se lee "PLAYSTREAM by neganboy" y el crédito de fork de Momiji con la licencia AGPL-3.0.
3. Idioma del teléfono en español: sin textos en inglés sueltos; revisa registro de consola, Ajustes, menú del stream, mensajes de error.
4. Ajustes → Calidad: aparecen Competitivo, Historia, Ultra 50 y Ultra 100. Cada uno conecta y se ve.
   - Competitivo: la imagen NO pasa por shaders aunque debanding/superresolución estén activados en Ajustes.
   - Historia: con la estadística visible, aparece "escalado con superresolución".
   - Ultra 100: mira la pérdida de paquetes en las estadísticas (se espera que pueda perder; ver `ANALYSIS.md` §2.2).
5. Códec H265 HDR: en el teléfono, ¿conecta? ¿se ve bien o lavado? Si el teléfono no lo decodifica, debe salir el aviso y usar H265.
6. Nitidez: mover el slider, salir y volver a Ajustes: el valor se conserva. Con superresolución activada se nota el cambio en el stream.
7. Calidad fuera de casa: no ofrece Ultra.
8. Registrar en el log cualquier cierre inesperado (`adb logcat`).
