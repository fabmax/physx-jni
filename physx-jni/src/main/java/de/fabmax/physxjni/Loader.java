package de.fabmax.physxjni;

import physx.PlatformChecks;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Loader {

    private static final String version = "2.7.1";

    private static final AtomicBoolean isLoaded = new AtomicBoolean(false);

    static List<String> libraryPaths = null;

    /**
     * Forces the library loaded to load the native libraries at the given paths instead of the builtin ones.
     * Paths must be absolute. Must be called before any PhysX function is called.
     */
    public static void setLoadLibraryPaths(List<String> paths) {
        if (isLoaded.get()) {
            throw new IllegalStateException("Library path cannot be set after library is loaded");
        }
        libraryPaths = paths;
    }

    public static void load() {
        if (!isLoaded.getAndSet(true)) {
            Platform platform = Platform.getPlatform();
            try {
                switch (platform) {
                    case LINUX:
                        PlatformChecks.setPlatformBit(PlatformChecks.PLATFORM_LINUX);
                        break;
                    case WINDOWS:
                        PlatformChecks.setPlatformBit(PlatformChecks.PLATFORM_WINDOWS);
                        break;
                    case MACOS:
                    case MACOS_ARM64:
                        PlatformChecks.setPlatformBit(PlatformChecks.PLATFORM_MACOS);
                        break;
                }

                NativeLib lib = platform.getLib();
                if (!version.equals(lib.getVersion())) {
                    throw new IllegalStateException("Native lib version " + lib.getVersion() +
                            " differs from main version " + version);
                }
                lib.load();
            } catch (Throwable t) {
                throw new IllegalStateException("Failed loading native PhysX libraries for platform " + platform, t);
            }
        }
    }
}
