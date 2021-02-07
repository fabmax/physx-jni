package de.fabmax.physxjni;

import org.junit.Assert;
import org.junit.Test;
import physx.common.PxVec3;
import sun.misc.Unsafe;

import java.io.Closeable;
import java.lang.reflect.Field;

public class StackAllocationTest {

    @Test
    public void stackAllocTest() {
        try (UnsafeMemory mem = new UnsafeMemory(1024)) {
            // interface based stack allocation
            PxVec3 vec1 = PxVec3.malloc(mem, UnsafeMemory::allocate, 0f, 0f, 0f);
            Assert.assertEquals(0f, vec1.getX(), 0.0);
            Assert.assertEquals(0f, vec1.getY(), 0.0);
            Assert.assertEquals(0f, vec1.getZ(), 0.0);

            vec1.setX(1f);
            vec1.setY(2f);
            vec1.setZ(3f);
            Assert.assertEquals(1f, vec1.getX(), 0.0);
            Assert.assertEquals(2f, vec1.getY(), 0.0);
            Assert.assertEquals(3f, vec1.getZ(), 0.0);

            // simple address based stack allocation
            PxVec3 vec2 = PxVec3.malloc(mem.allocate(PxVec3.ALIGNOF, PxVec3.SIZEOF), 0f, 0f, 0f);
            Assert.assertEquals(0f, vec2.getX(), 0.0);
            Assert.assertEquals(0f, vec2.getY(), 0.0);
            Assert.assertEquals(0f, vec2.getZ(), 0.0);

            vec2.setX(1f);
            vec2.setY(2f);
            vec2.setZ(3f);
            Assert.assertEquals(1f, vec2.getX(), 0.0);
            Assert.assertEquals(2f, vec2.getY(), 0.0);
            Assert.assertEquals(3f, vec2.getZ(), 0.0);
        }
    }

    private static class UnsafeMemory implements Closeable {
        private final Unsafe unsafe;
        private final long address;
        private long pointer;

        UnsafeMemory(long size) {
            try {
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                unsafe = (Unsafe) f.get(null);
            } catch (Throwable t) {
                throw new RuntimeException("Failed obtaining Unsafe", t);
            }

            address = unsafe.allocateMemory(size);
            pointer = address;
        }

        long allocate(int alignment, int size) {
            // alignment is ignored for this test...
            long addr = pointer;
            pointer += size;
            return addr;
        }

        @Override
        public void close() {
            unsafe.freeMemory(address);
        }
    }
}
