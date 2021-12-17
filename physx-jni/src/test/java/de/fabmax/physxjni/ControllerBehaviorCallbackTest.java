package de.fabmax.physxjni;

import org.junit.Assert;
import org.junit.Test;
import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.character.*;
import physx.common.PxBoundedData;
import physx.common.PxVec3;
import physx.cooking.PxTriangleMeshDesc;
import physx.geomutils.PxTriangleMesh;
import physx.geomutils.PxTriangleMeshGeometry;
import physx.physics.PxActor;
import physx.physics.PxRigidStatic;
import physx.physics.PxScene;
import physx.physics.PxShape;
import physx.support.Vector_PxU32;
import physx.support.Vector_PxVec3;

public class ControllerBehaviorCallbackTest {
    @Test
    public void controllerBehaviorCallbackTest() {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxScene scene = PhysXTestEnv.createEmptyScene(4);

            PxControllerManager controllerManager = PxTopLevelFunctions.CreateControllerManager(scene);
            PxControllerFilters filters = new PxControllerFilters();
            BehaviorCallback behaviorCb = new BehaviorCallback();
            PxController controller;

            // generate some triangle geometry as ground
            Vector_PxVec3 pointVector = new Vector_PxVec3();
            Vector_PxU32 indexVector = new Vector_PxU32();
            pointVector.push_back(PxVec3.createAt(mem, MemoryStack::nmalloc, 0f, 0f, 0f));
            PxVec3 tmpVec = PxVec3.createAt(mem, MemoryStack::nmalloc, 0f, 0f, 0f);
            for (int i = 0; i <= 4; i++) {
                double a = i / 4.0 * 2 * Math.PI;
                tmpVec.setX((float) Math.cos(a) * 5f);
                tmpVec.setZ((float) Math.sin(a) * -5f);
                // Vector_PxVec3 stores PxVec3's as value, we can (and should) recycle the same temporary PxVec3 object to fill it
                pointVector.push_back(tmpVec);
                if (i > 0) {
                    indexVector.push_back(0);
                    indexVector.push_back(i);
                    indexVector.push_back(i+1);
                }
            }

            PxBoundedData points = PxBoundedData.createAt(mem, MemoryStack::nmalloc);
            points.setCount(pointVector.size());
            points.setStride(PxVec3.SIZEOF);
            points.setData(pointVector.data());

            PxBoundedData triangles = PxBoundedData.createAt(mem, MemoryStack::nmalloc);
            triangles.setCount(indexVector.size() / 3);
            triangles.setStride(4 * 3);     // 3 4-byte integer indices per triangle
            triangles.setData(indexVector.data());

            PxTriangleMeshDesc meshDesc = PxTriangleMeshDesc.createAt(mem, MemoryStack::nmalloc);
            meshDesc.setPoints(points);
            meshDesc.setTriangles(triangles);

            // cook mesh and delete input data afterwards (no need to keep them around anymore)
            PxTriangleMesh mesh = PhysXTestEnv.cooking.createTriangleMesh(meshDesc, PhysXTestEnv.physics.getPhysicsInsertionCallback());
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

            Assert.assertEquals(0.0, controller.getFootPosition().getY(), 0.0001);
            Assert.assertEquals(5, behaviorCb.shapeCallbackCalls);

            triMesh.release();
            behaviorCb.destroy();
            controllerManager.release();
            scene.release();
            filters.destroy();
        }
    }

    private static class BehaviorCallback extends JavaControllerBehaviorCallback {
        int shapeCallbackCalls = 0;

        @Override
        public int getShapeBehaviorFlags(PxShape shape, PxActor actor) {
            shapeCallbackCalls++;
            return PxControllerBehaviorFlagEnum.eCCT_SLIDE;
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
