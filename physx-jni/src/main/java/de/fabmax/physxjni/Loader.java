package de.fabmax.physxjni;

import physx.JniThreadManager;

import java.util.concurrent.atomic.AtomicBoolean;

public class Loader {

    private static final String version = "2.0.6";

    private static final AtomicBoolean isLoaded = new AtomicBoolean(false);

    public static void load() {
        if (!isLoaded.getAndSet(true)) {
            Platform platform = Platform.getPlatform();
            try {
                NativeLib lib = platform.getLib();
                if (!version.equals(lib.getVersion())) {
                    throw new IllegalStateException("Native lib version " + lib.getVersion() +
                            " differs from main version " + version);
                }
                lib.load();
                JniThreadManager.init();
            } catch (Throwable t) {
                throw new IllegalStateException("Failed loading native PhysX libraries for platform " + platform, t);
            }
        }
    }
}
