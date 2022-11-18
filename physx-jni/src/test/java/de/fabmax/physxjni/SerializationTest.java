package de.fabmax.physxjni;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import physx.common.PxCollection;
import physx.extensions.*;
import physx.physics.PxScene;
import physx.support.PxU8ConstPtr;
import physx.support.TypeHelpers;
import physx.support.Vector_PxU8;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SerializationTest {

    private InputStream getTestXml() {
        InputStream testXml = getClass().getClassLoader().getResourceAsStream("SerializedCollection.xml");
        if (testXml == null) {
            Assertions.fail("SerializedCollection.xml not found in resources");
        }
        return testXml;
    }

    @Test
    public void serializeCollection() {
        List<String> expected = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(getTestXml()))) {
            in.lines().forEach(expected::add);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        PxScene scene = PhysXTestEnv.createEmptyScene(1);
        scene.addActor(PhysXTestEnv.createDefaultBox(0f, 0f, 0f));
        PxCollection sceneCollection = PxCollectionExt.createCollection(scene);

        PxSerializationRegistry sr = PxSerialization.createSerializationRegistry(PhysXTestEnv.physics);
        PxDefaultMemoryOutputStream memOut = new PxDefaultMemoryOutputStream();
        PxSerialization.complete(sceneCollection, sr);
        PxSerialization.serializeCollectionToXml(memOut, sceneCollection, sr);

        PxU8ConstPtr serData = TypeHelpers.voidToU8Ptr(memOut.getData());
        byte[] bin = new byte[memOut.getSize()];
        for (int i = 0; i < bin.length; i++) {
            bin[i] = TypeHelpers.getU8At(serData, i);
        }
        String[] actual = new String(bin).trim().split("\n");
        //Arrays.stream(actual).forEach(System.out::println);

        sr.release();
        memOut.destroy();
        sceneCollection.release();
        scene.release();

        Assertions.assertEquals(expected.size(), actual.length);
        for (int i = 0; i < actual.length; i++) {
            String expLine = expected.get(i).trim();

            boolean ignoreLine = expLine.startsWith("<Id >")    // IDs change by every execution, don't compare
                    || expLine.startsWith("<PxMaterialRef >")   // them with the store XML
                    || expLine.startsWith("<Speed >");          // For some reason, Speed scale differs between windows and linux

            if (!ignoreLine) {
                Assertions.assertEquals(expLine, actual[i].trim());
            }
        }
    }

    @Test
    public void deserializeCollection() {
        Vector_PxU8 data = new Vector_PxU8();
        try (BufferedInputStream in = new BufferedInputStream(getTestXml())) {
            int b = in.read();
            while (b != -1) {
                data.push_back((byte) b);
                b = in.read();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        PxDefaultMemoryInputData memIn = new PxDefaultMemoryInputData(TypeHelpers.voidToU8Ptr(data.data()), data.size());
        PxSerializationRegistry sr = PxSerialization.createSerializationRegistry(PhysXTestEnv.physics);
        PxCollection loadedCollection = PxSerialization.createCollectionFromXml(memIn, PhysXTestEnv.cooking, sr);
        Assertions.assertEquals(3, loadedCollection.getNbObjects());

        loadedCollection.release();
        sr.release();
        memIn.destroy();
        data.destroy();
    }
}
