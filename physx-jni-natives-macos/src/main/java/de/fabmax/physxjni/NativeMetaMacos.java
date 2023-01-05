package de.fabmax.physxjni;

import java.util.ArrayList;
import java.util.List;

public class NativeMetaMacos implements NativeMeta {

    private static final String version = "2.0.4-SNAPSHOT";

    private static final List<String> libraries = new ArrayList<>() {{
        add("macos/libPhysXJniBindings_64.dylib");
    }};

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public List<String> getLibResources() {
        return libraries;
    }
}
