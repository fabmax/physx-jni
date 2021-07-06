package de.fabmax.physxjni;

import java.util.ArrayList;
import java.util.List;

public class NativeMetaLinux64 implements NativeMeta {

    private static final String version = "0.4.12";

    private static final List<String> libraries = new ArrayList<String>() {{
        add("linux64/libPhysXJniBindings_64.so");
        add("linux64/libPhysXGpu_64.so");
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
