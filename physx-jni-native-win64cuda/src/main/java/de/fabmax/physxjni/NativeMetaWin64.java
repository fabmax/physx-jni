package de.fabmax.physxjni;

import java.util.ArrayList;
import java.util.List;

public class NativeMetaWin64 implements NativeMeta {

    private static final String version = "0.4.5-SNAPSHOT";

    private static final List<String> libraries = new ArrayList<String>() {{
        add("win64/PhysX_64.dll");
        add("win64/PhysXCommon_64.dll");
        add("win64/PhysXCooking_64.dll");
        add("win64/PhysXFoundation_64.dll");
        add("win64/PhysXGpu_64.dll");
        add("win64/PhysXDevice64.dll");
        add("win64/PhysXJniBindings_64.dll");
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
