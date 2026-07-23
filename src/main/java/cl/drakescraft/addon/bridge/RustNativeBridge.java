package cl.drakescraft.addon.bridge;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class RustNativeBridge {
    private static final Logger LOGGER = Logger.getLogger("Addon-RustBridge");
    private static boolean isNativeLoaded = false;

    public static void initialize(Path nativeLibPath) {
        try {
            System.load(nativeLibPath.toAbsolutePath().toString());
            isNativeLoaded = true;
            LOGGER.info("⚡ Bound to Slimefun-Rust FFM Engine!");
        } catch (Throwable t) {
            LOGGER.warning("⚠️ Slimefun-Rust native library not loaded: " + t.getMessage());
        }
    }

    public static boolean isNativeLoaded() { return isNativeLoaded; }
}
