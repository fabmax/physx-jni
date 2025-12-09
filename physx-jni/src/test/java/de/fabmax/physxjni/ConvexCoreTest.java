package de.fabmax.physxjni;

import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import physx.geometry.PxConvexCoreBox;
import physx.geometry.PxConvexCoreGeometryFactory;
import physx.geometry.PxConvexCoreTypeEnum;
import physx.geometry.PxGeometryTypeEnum;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConvexCoreTest {
    @Test
    public void convexBoxGeometryTest() {
        try (var stack = MemoryStack.stackPush()) {
            var box = PxConvexCoreBox.createAt(stack, MemoryStack::nmalloc, 1f, 2f, 3f);
            var boxGeometry = PxConvexCoreGeometryFactory.createFromBox(box, 0.1f);

            assertEquals(PxGeometryTypeEnum.eCONVEXCORE, boxGeometry.getType());
            assertEquals(PxConvexCoreTypeEnum.eBOX, boxGeometry.getCoreType());

            var data = MemoryUtil.memFloatBuffer(boxGeometry.getCoreData().getAddress(), 3);
            assertEquals(1f, data.get(0));
            assertEquals(2f, data.get(1));
            assertEquals(3f, data.get(2));

            assertEquals(0.1f, boxGeometry.getMargin());

            boxGeometry.destroy();
        }
    }
}
