package de.fabmax.physxjni;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.character.*;
import physx.common.PxBoundedData;
import physx.common.PxVec3;
import physx.cooking.PxTriangleMeshDesc;
import physx.geometry.PxTriangleMesh;
import physx.geometry.PxTriangleMeshGeometry;
import physx.physics.PxActor;
import physx.physics.PxRigidStatic;
import physx.physics.PxScene;
import physx.physics.PxShape;
import physx.support.PxArray_PxU32;
import physx.support.PxArray_PxVec3;

public class ControllerBehaviorCallbackTest {

    @Test
    public void controllerBehaviorCallbackTest() {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxScene scene = PhysXTestEnv.createEmptyScene();

            PxControllerManager controllerManager = PxTopLevelFunctions.CreateControllerManager(scene);
            PxControllerFilters filters = new PxControllerFilters();
            BehaviorCallback behaviorCb = new BehaviorCallback();
            PxController controller;

            // generate some triangle geometry as ground
            PxArray_PxVec3 pointVector = new PxArray_PxVec3();
            PxArray_PxU32 indexVector = new PxArray_PxU32();
            pointVector.pushBack(PxVec3.createAt(mem, MemoryStack::nmalloc, 0f, 0f, 0f));
            PxVec3 tmpVec = PxVec3.createAt(mem, MemoryStack::nmalloc, 0f, 0f, 0f);
            for (int i = 0; i <= 4; i++) {
                double a = i / 4.0 * 2 * Math.PI;
                tmpVec.setX((float) Math.cos(a) * 5f);
                tmpVec.setZ((float) Math.sin(a) * -5f);
                // Vector_PxVec3 stores PxVec3's as value, we can (and should) recycle the same temporary PxVec3 object to fill it
                pointVector.pushBack(tmpVec);
                if (i > 0) {
                    indexVector.pushBack(0);
                    indexVector.pushBack(i);
                    indexVector.pushBack(i+1);
                }
            }

            PxBoundedData points = PxBoundedData.createAt(mem, MemoryStack::nmalloc);
            points.setCount(pointVector.size());
            points.setStride(PxVec3.SIZEOF);
            points.setData(pointVector.begin());

            PxBoundedData triangles = PxBoundedData.createAt(mem, MemoryStack::nmalloc);
            triangles.setCount(indexVector.size() / 3);
            triangles.setStride(4 * 3);     // 3 4-byte integer indices per triangle
            triangles.setData(indexVector.begin());

            PxTriangleMeshDesc meshDesc = PxTriangleMeshDesc.createAt(mem, MemoryStack::nmalloc);
            meshDesc.setPoints(points);
            meshDesc.setTriangles(triangles);

            // cook mesh and delete input data afterwards (no need to keep them around anymore)
            PxTriangleMesh mesh = PxTopLevelFunctions.CreateTriangleMesh(PhysXTestEnv.cookingParams, meshDesc);
            pointVector.destroy();
            indexVector.destroy();

            PxTriangleMeshGeometry geom = PxTriangleMeshGeometry.createAt(mem, MemoryStack::nmalloc, mesh);
            PxRigidStatic triMesh = PhysXTestEnv.createStaticBody(geom, 0f, 0f, 0f);
            scene.addActor(triMesh);

            PxCapsuleControllerDesc desc = PxCapsuleControllerDesc.createAt(mem, MemoryStack::nmalloc);
            desc.setMaterial(PhysXTestEnv.defaultMaterial);
            desc.setHeight(1);
            desc.setRadius(0.5f);
            desc.setBehaviorCallback(behaviorCb);
            controller = controllerManager.createController(desc);
            controller.setFootPosition(PxExtendedVec3.createAt(mem, MemoryStack::nmalloc, 0.0, 0.51, 0.0));

            for (int i = 0; i < 10; i++) {
                scene.simulate(1f / 60f);
                scene.fetchResults(true);
                controller.move(PxVec3.createAt(mem, MemoryStack::nmalloc, 0, -0.1f, 0), 0.001f, 1f / 60f, filters);
            }
            System.out.println("Controller foot pos: " + controller.getFootPosition().getY());
            System.out.println("Behavior callback calls: " + behaviorCb.shapeCallbackCalls);

            Assertions.assertEquals(0.0, controller.getFootPosition().getY(), 0.0001);
            Assertions.assertTrue(behaviorCb.shapeCallbackCalls > 0);

            triMesh.release();
            behaviorCb.destroy();
            controllerManager.release();
            scene.release();
            filters.destroy();
        }
    }

    private static class BehaviorCallback extends PxControllerBehaviorCallbackImpl {
        int shapeCallbackCalls = 0;

        @Override
        public int getShapeBehaviorFlags(PxShape shape, PxActor actor) {
            shapeCallbackCalls++;
            return PxControllerBehaviorFlagEnum.eCCT_SLIDE.value;
        }

        @Override
        public int getControllerBehaviorFlags(PxController controller) {
            return 0;
        }

        @Override
        public int getObstacleBehaviorFlags(PxObstacle obstacle) {
            return 0;
        }
    }
}
