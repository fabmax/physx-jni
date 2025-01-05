package de.fabmax.physxjni.windows;

import de.fabmax.physxjni.NativeLib;

import java.util.ArrayList;
import java.util.List;

public class NativeLibWindows extends NativeLib {

    private static final String version = "2.5.0";

    private static final List<String> libraries = new ArrayList<>() {{
        add("PhysX_64.dll");
        add("PhysXCommon_64.dll");
        add("PhysXCooking_64.dll");
        add("PhysXFoundation_64.dll");
        add("PhysXJniBindings_64.dll");
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
