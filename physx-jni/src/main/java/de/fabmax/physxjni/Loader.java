package de.fabmax.physxjni;

import physx.JniThreadManager;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Loader {

    private static final String version = "2.0.3-SNAPSHOT";

    private static final AtomicBoolean isLoaded = new AtomicBoolean(false);

    public static void load() {
        if (!isLoaded.getAndSet(true)) {
            Platform platform = Platform.getPlatform();
            try {
                NativeMeta meta = platform.getMeta();
                if (!version.equals(meta.getVersion())) {
                    throw new IllegalStateException("Native lib version " + meta.getVersion() +
                            " differs from main version " + version);
                }
                loadLibsFromResources(meta.getLibResources());
                JniThreadManager.init();

            } catch (Throwable t) {
                throw new IllegalStateException("Failed loading native PhysX libraries for platform " + platform, t);
            }
        }
    }

    private static void loadLibsFromResources(List<String> libResourceNames) throws IOException {
        boolean skipHashCheck = Boolean.parseBoolean(System.getProperty("physx.skipHashCheck", "false"));
        String defaultLibPath = new File(System.getProperty("java.io.tmpdir"), "de.fabmax.physx-jni" + File.separator + version).getAbsolutePath();
        String libTmpPath = System.getProperty("physx.nativeLibLocation", defaultLibPath);

        File tempLibDir = new File(libTmpPath);
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
            if (libTmpFile.exists() && !skipHashCheck && !checkHash(libResource, libTmpFile)) {
                if (!libTmpFile.delete()) {
                    throw new IllegalStateException("Failed deleting existing native lib file " + libTmpFile);
                }
            }
            if (!libTmpFile.exists()) {
                Files.copy(libIn, libTmpFile.toPath());
                //System.out.println("copy " + libResource + " to " + libTmpFile.getAbsolutePath());
            }
            libFiles.add(libTmpFile.getAbsolutePath());
        }

        // 2nd: load libs
        for (String libFile : libFiles) {
            System.load(libFile);
        }
    }

    private static boolean checkHash(String resourceName, File file) {
        boolean isSameHash = false;
        try {
            InputStream hashIn = Loader.class.getClassLoader().getResourceAsStream(resourceName + ".sha1");
            if (hashIn != null) {
                String fileHash = makeFileHash(file);
                String resourceHash;
                try (BufferedReader r = new BufferedReader(new InputStreamReader(hashIn))) {
                    resourceHash = r.readLine();
                }
                isSameHash = fileHash.equals(resourceHash);
            } else {
                System.err.println("Failed to get signature for " + resourceName + " from resources");
            }
        } catch (Exception e) {
            System.err.println("Error on signature check for " + resourceName + " / " + file);
            e.printStackTrace();
        }
        return isSameHash;
    }

    private static String makeFileHash(File file) throws Exception {
        try (FileInputStream inStream = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buf = new byte[4096];
            int len = inStream.read(buf);
            while (len > 0) {
                md.update(buf, 0, len);
                len = inStream.read(buf);
            }
            byte[] hash = md.digest();
            StringBuilder hashStr = new StringBuilder();
            for (byte b : hash) {
                hashStr.append(String.format("%02x", ((int) b) & 0xff));
            }
            return hashStr.toString();
        }
    }
}
