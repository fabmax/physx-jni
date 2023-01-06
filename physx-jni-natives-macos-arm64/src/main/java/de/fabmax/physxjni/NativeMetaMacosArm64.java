package de.fabmax.physxjni;

import java.util.ArrayList;
import java.util.List;

public class NativeMetaMacosArm64 implements NativeMeta {

    private static final String version = "2.0.4";

    private static final List<String> libraries = new ArrayList<>() {{
        add("macos-arm64/libPhysXJniBindings_64.dylib");
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
