package de.fabmax.physxjni;

import physx.PlatformChecks;

import java.util.concurrent.atomic.AtomicBoolean;

public class Loader {

    private static final String version = "2.4.2-SNAPSHOT";

    private static final AtomicBoolean isLoaded = new AtomicBoolean(false);

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
