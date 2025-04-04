package de.fabmax.physxjni.linux;

import de.fabmax.physxjni.NativeLib;

import java.util.ArrayList;
import java.util.List;

public class NativeLibLinux extends NativeLib {

    private static final String version = "2.6.1-SNAPSHOT";

    private static final List<String> libraries = new ArrayList<>() {{
        add("libPhysXJniBindings_64.so");
    }};

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    protected List<String> getLibResourceNames() {
        return libraries;
    }
}
