package de.fabmax.physxjni;

public enum Platform {

    LINUX("de.fabmax.physxjni.linux.NativeLibLinux"),
    WINDOWS("de.fabmax.physxjni.windows.NativeLibWindows"),
    MACOS("de.fabmax.physxjni.macos.NativeLibMacos"),
    MACOS_ARM64("de.fabmax.physxjni.macosarm.NativeLibMacosArm64");

    private final String metaClassName;

    Platform(String metaClassName) {
        this.metaClassName = metaClassName;
    }

    public NativeLib getLib() throws ReflectiveOperationException {
        Class<?> libImpl =  Loader.class.getClassLoader().loadClass(metaClassName);
        return (NativeLib) libImpl.getConstructor().newInstance();
    }

    public static Platform getPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch");
        
        if (osName.contains("windows")) {
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
