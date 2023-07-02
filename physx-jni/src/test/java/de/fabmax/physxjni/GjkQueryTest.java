package de.fabmax.physxjni;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import physx.common.PxIDENTITYEnum;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.extensions.CustomSupportImpl;
import physx.extensions.PxGjkQuery;
import physx.extensions.PxGjkQueryProximityInfoResult;
import physx.extensions.SphereSupport;

public class GjkQueryTest {

    @Test
    public void gjkQuery() {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            // two spheres with radius:
            //   1st: built-in sphere, r = 0.75
            //   2nd: custom sphere implementation, r = 1.25
            var sphereSupport = SphereSupport.createAt(mem, MemoryStack::nmalloc, 0.75f);
            var customSphereSupport = new CustomSphereSupport(1.25f, 0f);

            // 1st sphere location: origin
            var poseA = PxTransform.createAt(mem, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity);
            // 2nd sphere location: x = 2 == r1 + r2
            var poseB = PxTransform.createAt(mem, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity);
            poseB.setP(PxVec3.createAt(mem, MemoryStack::nmalloc, 2f, 0f, 0f));

            var proximityResult = PxGjkQueryProximityInfoResult.createAt(mem, MemoryStack::nmalloc);
            PxGjkQuery.proximityInfo(sphereSupport, customSphereSupport, poseA, poseB, 1f, 1f, proximityResult);
            Assertions.assertTrue(proximityResult.getSuccess());
            var ptA = proximityResult.getPointA();
            var ptB = proximityResult.getPointB();

            // -> spheres should touch each other at x = 0.75 (separation = 0.0)
            System.out.println("separation:   " + proximityResult.getSeparation());
            System.out.printf("closest pt a: (%.3f, %.3f, %.3f)\n", ptA.getX(), ptA.getY(), ptA.getZ());
            System.out.printf("closest pt b: (%.3f, %.3f, %.3f)\n", ptB.getX(), ptB.getY(), ptB.getZ());

            Assertions.assertEquals(0f, proximityResult.getSeparation(), 0.0001f);
            Assertions.assertEquals(0.75f, ptA.getX(), 0.0001f);
            Assertions.assertEquals(0.75f, ptB.getX(), 0.0001f);


            // construct another custom sphere with a shifted origin (only for example / testing purpose, usually it
            // would make much more sense to use the pose to set the position...)
            var customSphereSupportShifted = new CustomSphereSupport(1f, -0.5f);

            PxGjkQuery.proximityInfo(customSphereSupportShifted, customSphereSupport, poseA, poseB, 1f, 1f, proximityResult);
            Assertions.assertTrue(proximityResult.getSuccess());
            ptA = proximityResult.getPointA();
            ptB = proximityResult.getPointB();

            // -> sphere separation should be 0.25 with closest point a at x = 0.5 and closest point b at x = 0.75
            System.out.println("[shifted origin] separation:   " + proximityResult.getSeparation());
            System.out.printf("[shifted origin] closest pt a: (%.3f, %.3f, %.3f)\n", ptA.getX(), ptA.getY(), ptA.getZ());
            System.out.printf("[shifted origin] closest pt b: (%.3f, %.3f, %.3f)\n", ptB.getX(), ptB.getY(), ptB.getZ());

            Assertions.assertEquals(0.25f, proximityResult.getSeparation(), 0.0001f);
            Assertions.assertEquals(0.5f, ptA.getX(), 0.0001f);
            Assertions.assertEquals(0.75f, ptB.getX(), 0.0001f);
        }
    }

    static class CustomSphereSupport extends CustomSupportImpl {
        private final float radius;
        private final float xPos;

        CustomSphereSupport(float radius, float xPos) {
            this.radius = radius;
            this.xPos = xPos;
        }

        @Override
        public float getCustomMargin() {
            return radius;
        }

        @Override
        public void getCustomSupportLocal(PxVec3 dir, PxVec3 result) {
            result.setX(xPos);
            result.setY(0f);
            result.setZ(0f);
        }
    }
}
