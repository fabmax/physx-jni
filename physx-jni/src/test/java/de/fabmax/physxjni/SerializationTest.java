package de.fabmax.physxjni;

import org.junit.jupiter.api.*;
import physx.common.PxCollection;
import physx.extensions.*;
import physx.physics.PxScene;
import physx.support.NativeArrayHelpers;
import physx.support.PxU8ConstPtr;
import physx.support.Vector_PxU8;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SerializationTest {

    @Test
    @Order(1)
    public void serializeCollection() {
        String serialization = serializeScene();

        // perform a few simple checks on the serialization string - it should contain a material and a dynamic actor
        // System.out.println(serialization);
        Assertions.assertTrue(serialization.contains("PxMaterial") && serialization.contains("PxRigidDynamic"));
    }

    private String serializeScene() {
        PxScene scene = PhysXTestEnv.createEmptyScene();
        scene.addActor(PhysXTestEnv.createDefaultBox(0f, 0f, 0f));
        PxCollection sceneCollection = PxCollectionExt.createCollection(scene);

        PxSerializationRegistry sr = PxSerialization.createSerializationRegistry(PhysXTestEnv.physics);
        PxDefaultMemoryOutputStream memOut = new PxDefaultMemoryOutputStream();
        PxSerialization.complete(sceneCollection, sr);
        PxSerialization.serializeCollectionToXml(memOut, sceneCollection, sr);

        PxU8ConstPtr serData = NativeArrayHelpers.voidToU8Ptr(memOut.getData());
        byte[] bin = new byte[memOut.getSize()];
        for (int i = 0; i < bin.length; i++) {
            bin[i] = NativeArrayHelpers.getU8At(serData, i);
        }

        sr.release();
        memOut.destroy();
        sceneCollection.release();
        scene.release();

        return new String(bin);
    }

    @Test
    @Order(2)
    public void deserializeCollection() {
        Vector_PxU8 data = new Vector_PxU8();
        String serialization = serializeScene();
        for (int i = 0; i < serialization.length(); i++) {
            data.push_back((byte) serialization.charAt(i));
        }

        PxDefaultMemoryInputData memIn = new PxDefaultMemoryInputData(NativeArrayHelpers.voidToU8Ptr(data.data()), data.size());
        PxSerializationRegistry sr = PxSerialization.createSerializationRegistry(PhysXTestEnv.physics);
        PxCollection loadedCollection = PxSerialization.createCollectionFromXml(memIn, PhysXTestEnv.cookingParams, sr);
        Assertions.assertEquals(3, loadedCollection.getNbObjects());

        loadedCollection.release();
        sr.release();
        memIn.destroy();
        data.destroy();
    }
}
