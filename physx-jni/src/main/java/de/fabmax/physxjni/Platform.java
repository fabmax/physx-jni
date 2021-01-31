package de.fabmax.physxjni;

public enum Platform {

    WIN64("de.fabmax.physxjni.NativeMetaWin64");

    private final String metaClassName;

    Platform(String metaClassName) {
        this.metaClassName = metaClassName;
    }

    public String getMetaClassName() {
        return metaClassName;
    }

    public static Platform getPlatform() {
        // todo: detect the platform we are running on, however this does not make sense as long as only win64 is supported
        return WIN64;
    }
}
