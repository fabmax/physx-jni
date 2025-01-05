package de.fabmax.physxjni.macosarm;

import de.fabmax.physxjni.NativeLib;

import java.util.ArrayList;
import java.util.List;

public class NativeLibMacosArm64 extends NativeLib {

    private static final String version = "2.5.0";

    private static final List<String> libraries = new ArrayList<>() {{
        add("libPhysXJniBindings_64.dylib");
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
