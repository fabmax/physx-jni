package de.fabmax.physxjni;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import physx.common.PxVec3;

public class ExternalAllocationTest {
    @Test
    public void externalAllocTest() {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            // interface based stack allocation
            PxVec3 vec = PxVec3.createAt(mem, MemoryStack::nmalloc, 1f, 2f, 3f);
            Assertions.assertEquals(1f, vec.getX(), 0.0);
            Assertions.assertEquals(2f, vec.getY(), 0.0);
            Assertions.assertEquals(3f, vec.getZ(), 0.0);

            vec.setX(4f);
            vec.setY(5f);
            vec.setZ(6f);
            Assertions.assertEquals(4f, vec.getX(), 0.0);
            Assertions.assertEquals(5f, vec.getY(), 0.0);
            Assertions.assertEquals(6f, vec.getZ(), 0.0);

            // simple address based stack allocation
            long address = mem.nmalloc(PxVec3.ALIGNOF, PxVec3.SIZEOF);
            vec = PxVec3.createAt(address, 2f, 3f, 4f);
            Assertions.assertEquals(2f, vec.getX(), 0.0);
            Assertions.assertEquals(3f, vec.getY(), 0.0);
            Assertions.assertEquals(4f, vec.getZ(), 0.0);

            vec.setX(3f);
            vec.setY(2f);
            vec.setZ(1f);
            Assertions.assertEquals(3f, vec.getX(), 0.0);
            Assertions.assertEquals(2f, vec.getY(), 0.0);
            Assertions.assertEquals(1f, vec.getZ(), 0.0);
        }
    }
}
