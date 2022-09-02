package de.fabmax.physxjni;

import java.util.ArrayList;
import java.util.List;

public class NativeMetaWindows implements NativeMeta {

    private static final String version = "1.1.0";

    private static final List<String> libraries = new ArrayList<String>() {{
        add("windows/PhysX_64.dll");
        add("windows/PhysXCommon_64.dll");
        add("windows/PhysXCooking_64.dll");
        add("windows/PhysXFoundation_64.dll");
        add("windows/PhysXJniBindings_64.dll");
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
