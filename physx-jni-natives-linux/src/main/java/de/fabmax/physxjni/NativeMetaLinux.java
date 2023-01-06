package de.fabmax.physxjni;

import java.util.ArrayList;
import java.util.List;

public class NativeMetaLinux implements NativeMeta {

    private static final String version = "2.0.4";

    private static final List<String> libraries = new ArrayList<>() {{
        add("linux/libPhysXJniBindings_64.so");
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
