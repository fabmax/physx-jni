package de.fabmax.physxandroid;

import android.util.Log;
import physxandroid.PlatformChecks;

import java.util.concurrent.atomic.AtomicBoolean;

public class Loader {

    private static final AtomicBoolean isLoaded = new AtomicBoolean(false);

    public static void load() {
        if (!isLoaded.getAndSet(true)) {
            System.loadLibrary("PhysXJniBindings_64");
            PlatformChecks.setPlatformBit(PlatformChecks.PLATFORM_ANDROID);
            Log.i("physx-jni", "Loaded native libraries");
        }
    }
}
