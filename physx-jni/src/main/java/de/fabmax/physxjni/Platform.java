package de.fabmax.physxjni;

public enum Platform {

    LINUX("de.fabmax.physxjni.linux.NativeLibLinux"),
    WINDOWS("de.fabmax.physxjni.windows.NativeLibWindows"),
    MACOS("de.fabmax.physxjni.macos.NativeLibMacos"),
    MACOS_ARM64("de.fabmax.physxjni.macosarm.NativeLibMacosArm64"),;

    private final String metaClassName;

    Platform(String metaClassName) {
        this.metaClassName = metaClassName;
    }

    public NativeLib getLib() throws ReflectiveOperationException {
        Class<?> libImpl =  Loader.class.getClassLoader().loadClass(metaClassName);
        return (NativeLib) libImpl.getConstructor().newInstance();
    }

    public static Platform getPlatform() {
        String vendor = System.getProperty("java.vendor", "unknown").toLowerCase();
        String osName = System.getProperty("os.name", "unknown").toLowerCase();
        String arch = System.getProperty("os.arch", "unknown");

        if (vendor.contains("android")) {   // on Android java.vendor should be "The Android Project"
            throw new IllegalStateException("Android environment detected. Use 'physx-jni-android' library instead of regular 'physx-jni'");
        } else if (osName.contains("windows")) {
            return WINDOWS;
        } else if (osName.contains("linux")) {
            return LINUX;
        } else if (osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx")) {
            if ("aarch64".equals(arch)) {
                return MACOS_ARM64;
            } else {
                return MACOS;
            }
        } else {
            throw new IllegalStateException("Unsupported OS: " + osName);
        }
    }
}
