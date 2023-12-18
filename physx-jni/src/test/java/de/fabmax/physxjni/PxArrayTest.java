package de.fabmax.physxjni;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import physx.common.PxVec3;
import physx.support.PxArray_PxU32;
import physx.support.PxArray_PxVec3;

public class PxArrayTest {
    @Test
    public void testBasicArrayFunctions() {
        // needed if this test is run stand-alone, because PxFoundation needs to be created for PxArray to work
        PhysXTestEnv.init();

        try (MemoryStack mem = MemoryStack.stackPush()) {
            var array = new PxArray_PxVec3(0);
            Assertions.assertEquals(0, array.size());

            // add a vector to the empty array
            var vecToAdd = PxVec3.createAt(mem, MemoryStack::nmalloc, 1f, 2f, 3f);
            array.pushBack(vecToAdd);
            Assertions.assertEquals(1, array.size());

            // items are added by value, changing vecToAdd won't change array content
            vecToAdd.setX(4f);   vecToAdd.setY(5f);   vecToAdd.setZ(6f);

            // get vector from array. vector is returned by value, changing array content will affect the returned
            // object
            var vecFromArray = array.get(0);
            Assertions.assertTrue(vecFromArray.getX() == 1f && vecFromArray.getY() == 2f && vecFromArray.getZ() == 3f);

            // overwrite array item, will also affect vecFromArray
            array.set(0, vecToAdd);
            Assertions.assertEquals(1, array.size());
            Assertions.assertTrue(vecFromArray.getX() == 4f && vecFromArray.getY() == 5f && vecFromArray.getZ() == 6f);

            array.clear();
            Assertions.assertEquals(0, array.size());

            array.destroy();
        }
    }

    @Test
    public void testZeroInit() {
        // needed if this test is run stand-alone, because PxFoundation needs to be created for PxArray to work
        PhysXTestEnv.init();

        try (MemoryStack mem = MemoryStack.stackPush()) {
            // create an array with initial size of 10: will contain 10 PxVec3 initialized to (0, 0, 0)
            var array = new PxArray_PxVec3(10);
            Assertions.assertEquals(10, array.size());

            // append an additional vector as 11th element
            var vecToAdd = PxVec3.createAt(mem, MemoryStack::nmalloc, 1f, 2f, 3f);
            array.pushBack(vecToAdd);
            Assertions.assertEquals(11, array.size());

            for (int i = 0; i < array.size(); i++) {
                var it = array.get(i);

                if (i == 10) {
                    Assertions.assertTrue(it.getX() == 1f && it.getY() == 2f && it.getZ() == 3f);
                } else {
                    Assertions.assertTrue(it.getX() == 0f && it.getY() == 0f && it.getZ() == 0f);
                }
            }

            array.destroy();
        }
    }

    @Test
    public void testPrimitiveArrayFunctions() {
        // needed if this test is run stand-alone, because PxFoundation needs to be created for PxArray to work
        PhysXTestEnv.init();

        var array = new PxArray_PxU32(1);
        Assertions.assertEquals(0, array.get(0));

        array.set(0, 17);
        Assertions.assertEquals(17, array.get(0));
    }
}
