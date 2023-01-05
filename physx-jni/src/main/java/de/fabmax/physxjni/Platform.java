package de.fabmax.physxjni;

public enum Platform {

    LINUX("de.fabmax.physxjni.NativeMetaLinux"),
    WINDOWS("de.fabmax.physxjni.NativeMetaWindows"),
    MACOS("de.fabmax.physxjni.NativeMetaMacos"),
    MACOS_ARM64("de.fabmax.physxjni.NativeMetaMacosArm64");

    private final String metaClassName;

    Platform(String metaClassName) {
        this.metaClassName = metaClassName;
    }

    public NativeMeta getMeta() throws ReflectiveOperationException {
        Class<?> metaClass =  Loader.class.getClassLoader().loadClass(metaClassName);
        return (NativeMeta) metaClass.getConstructor().newInstance();
    }

    public static Platform getPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch");
        
        if (osName.contains("windows")) {
            return WINDOWS;
        } else if (osName.contains("linux")) {
            return LINUX;
        } else if (osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx")) {
            if (arch != null && arch.equals("aarch64")) {
                return MACOS_ARM64;
            } else {
                return MACOS;
            }
        } else {
            throw new IllegalStateException("Unsupported OS: " + osName);
        }
    }
}
