package de.fabmax.physxjni;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import physx.geometry.PxTetrahedronMeshDesc;
import physx.geometry.PxTetrahedronMeshFormatEnum;
import physx.support.PxArray_PxU32;
import physx.support.PxArray_PxVec3;

public class TetMeshTest {

    static {
        // needed if this test is run stand-alone, because PxFoundation needs to be created for PxArray to work
        PhysXTestEnv.init();
    }

    // todo: actual tests needed...

    @Test
    public void testTetMeshDesc() {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            var meshVertices = PxArray_PxVec3.createAt(mem, MemoryStack::nmalloc, 4);
            var meshIndices = PxArray_PxU32.createAt(mem, MemoryStack::nmalloc, 4);

            for (int i = 0; i < 4; i++) {
                meshIndices.set(i, i);
            }

            var testMeshDesc = PxTetrahedronMeshDesc.createAt(mem, MemoryStack::nmalloc, meshVertices, meshIndices, PxTetrahedronMeshFormatEnum.eTET_MESH);
            Assertions.assertTrue(testMeshDesc.isValid());
        }
    }
}
