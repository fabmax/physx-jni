package de.fabmax.physxjni;

import org.junit.Assert;
import org.junit.Test;
import org.lwjgl.system.MemoryStack;
import physx.common.PxVec3;

public class ExternalAllocationTest {
    @Test
    public void externalAllocTest() {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            // interface based stack allocation
            PxVec3 vec = PxVec3.createAt(mem, MemoryStack::nmalloc, 1f, 2f, 3f);
            Assert.assertEquals(1f, vec.getX(), 0.0);
            Assert.assertEquals(2f, vec.getY(), 0.0);
            Assert.assertEquals(3f, vec.getZ(), 0.0);

            vec.setX(4f);
            vec.setY(5f);
            vec.setZ(6f);
            Assert.assertEquals(4f, vec.getX(), 0.0);
            Assert.assertEquals(5f, vec.getY(), 0.0);
            Assert.assertEquals(6f, vec.getZ(), 0.0);

            // simple address based stack allocation
            long address = mem.nmalloc(PxVec3.ALIGNOF, PxVec3.SIZEOF);
            vec = PxVec3.createAt(address, 2f, 3f, 4f);
            Assert.assertEquals(2f, vec.getX(), 0.0);
            Assert.assertEquals(3f, vec.getY(), 0.0);
            Assert.assertEquals(4f, vec.getZ(), 0.0);

            vec.setX(3f);
            vec.setY(2f);
            vec.setZ(1f);
            Assert.assertEquals(3f, vec.getX(), 0.0);
            Assert.assertEquals(2f, vec.getY(), 0.0);
            Assert.assertEquals(1f, vec.getZ(), 0.0);
        }
    }
}
