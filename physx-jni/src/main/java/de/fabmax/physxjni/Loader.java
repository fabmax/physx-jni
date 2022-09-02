package de.fabmax.physxjni;

import physx.JniThreadManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Loader {

    private static final String version = "1.1.0";

    private static final AtomicBoolean isLoaded = new AtomicBoolean(false);

    public static void load() {
        if (!isLoaded.getAndSet(true)) {
            Platform platform = Platform.getPlatform();
            try {
                Class<?> metaClass =  Loader.class.getClassLoader().loadClass(platform.getMetaClassName());
                NativeMeta meta = (NativeMeta) metaClass.getConstructor().newInstance();

                if (!version.equals(meta.getVersion())) {
                    throw new IllegalStateException("Native lib version " + meta.getVersion() + " differs from main" +
                            " version " + version);
                }

                boolean forceCopy = meta.getVersion().endsWith("-SNAPSHOT")
                        || "true".equalsIgnoreCase(System.getProperty("physx.forceCopyLibs", "false"));
                loadLibsFromResources(meta.getLibResources(), forceCopy);

                JniThreadManager.init();

            } catch (Throwable t) {
                throw new IllegalStateException("Failed loading native PhysX libraries for platform " + platform, t);
            }
        }
    }

    private static void loadLibsFromResources(List<String> libResourceNames, boolean forceCopy) throws IOException {
        File tempLibDir = new File(System.getProperty("java.io.tmpdir"), "de.fabmax.physx-jni" + File.separator + version);
        if ((tempLibDir.exists() && !tempLibDir.isDirectory()) || (!tempLibDir.exists() && !tempLibDir.mkdirs())) {
            throw new IllegalStateException("Failed creating native lib dir " + tempLibDir);
        }

        // 1st: make sure all libs are available in system temp dir
        List<String> libFiles = new ArrayList<>();
        for (String libResource : libResourceNames) {
            InputStream libIn = Loader.class.getClassLoader().getResourceAsStream(libResource);
            if (libIn == null) {
                throw new IllegalStateException("Failed loading " + libResource + " from resources");
            }

            File libTmpFile = new File(tempLibDir, new File(libResource).getName());
            if (forceCopy && libTmpFile.exists()) {
                if (!libTmpFile.delete()) {
                    throw new IllegalStateException("Failed deleting existing native lib file " + libTmpFile);
                }
            }
            if (!libTmpFile.exists()) {
                Files.copy(libIn, libTmpFile.toPath());
            }
            libFiles.add(libTmpFile.getAbsolutePath());
        }

        // 2nd: load libs
        for (String libFile : libFiles) {
            System.load(libFile);
        }
    }
}
