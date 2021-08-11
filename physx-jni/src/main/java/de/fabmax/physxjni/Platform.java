package de.fabmax.physxjni;

public enum Platform {

    LINUX64("de.fabmax.physxjni.NativeMetaLinux64"),
    WIN64("de.fabmax.physxjni.NativeMetaWin64"),
    MAC64("de.fabmax.physxjni.NativeMetaMac64");

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
            return WIN64;
        } else if (osName.contains("linux")) {
            return LINUX64;
        } else if (osName.contains("mac")) {
            return MAC64;
        } else {
            throw new IllegalStateException("Unsupported OS: " + osName);
        }
    }
}
