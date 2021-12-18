package de.fabmax.physxjni;

import java.util.ArrayList;
import java.util.List;

public class NativeMetaMac64 implements NativeMeta {

    private static final String version = "0.4.16";

    private static final List<String> libraries = new ArrayList<String>() {{
        add("mac64/libPhysXJniBindings_64.dylib");
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
