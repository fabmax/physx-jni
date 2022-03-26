package de.fabmax.physxjni;

public enum Platform {

    LINUX("de.fabmax.physxjni.NativeMetaLinux"),
    WINDOWS("de.fabmax.physxjni.NativeMetaWindows"),
    MACOS("de.fabmax.physxjni.NativeMetaMacos");

    private final String metaClassName;

    Platform(String metaClassName) {
        this.metaClassName = metaClassName;
    }

    public String getMetaClassName() {
        return metaClassName;
    }

    public static Platform getPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            return WINDOWS;
        } else if (osName.contains("linux")) {
            return LINUX;
        } else if (osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx")) {
            return MACOS;
        } else {
            throw new IllegalStateException("Unsupported OS: " + osName);
        }
    }
}
