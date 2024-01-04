package de.fabmax.physxjni;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.common.PxBoundedData;
import physx.common.PxVec3;
import physx.cooking.PxTriangleMeshDesc;
import physx.geometry.PxTriangleMesh;
import physx.geometry.PxTriangleMeshGeometry;
import physx.physics.PxRigidDynamic;
import physx.physics.PxRigidStatic;
import physx.physics.PxScene;
import physx.support.PxArray_PxU32;
import physx.support.PxArray_PxVec3;

public class TriangleMeshTest {
    @Test
    public void triangleMeshTest() {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxScene scene = PhysXTestEnv.createEmptyScene();

            // generate some triangle geometry (negative cone with 10 triangles, 0.5 units deep, 5 units radius)
            PxArray_PxVec3 pointVector = new PxArray_PxVec3();
            PxArray_PxU32 indexVector = new PxArray_PxU32();
            pointVector.pushBack(PxVec3.createAt(mem, MemoryStack::nmalloc, 0f, -0.5f, 0f));
            PxVec3 tmpVec = PxVec3.createAt(mem, MemoryStack::nmalloc, 0f, 0f, 0f);
            for (int i = 0; i <= 10; i++) {
                double a = i / 10.0 * 2 * Math.PI;
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

            // create mesh descriptor
            PxBoundedData points = PxBoundedData.createAt(mem, MemoryStack::nmalloc);
            points.setCount(pointVector.size());
            points.setStride(PxVec3.SIZEOF);
            points.setData(pointVector.begin());

            PxBoundedData triangles = PxBoundedData.createAt(mem, MemoryStack::nmalloc);
            triangles.setCount(indexVector.size() / 3);
            triangles.setStride(4 * 3);     // 3 4-byte integer indices per triangle
            triangles.setData(indexVector.begin());

            PxTriangleMeshDesc desc = PxTriangleMeshDesc.createAt(mem, MemoryStack::nmalloc);
            desc.setPoints(points);
            desc.setTriangles(triangles);

            // cook mesh and delete input data afterwards (no need to keep them around anymore)
            PxTriangleMesh mesh = PxTopLevelFunctions.CreateTriangleMesh(PhysXTestEnv.cookingParams, desc);
            pointVector.destroy();
            indexVector.destroy();

            // create bodies and scene
            PxTriangleMeshGeometry geom = PxTriangleMeshGeometry.createAt(mem, MemoryStack::nmalloc, mesh);
            PxRigidStatic triMesh = PhysXTestEnv.createStaticBody(geom, 0f, 0f, 0f);
            scene.addActor(triMesh);

            PxRigidDynamic box = PhysXTestEnv.createDefaultBox(0f, 5f, 0f);
            scene.addActor(box);

            // simulate for a few seconds
            PhysXTestEnv.simulateScene(scene, 5, box);

            // box should rest on our tri mesh
            Assertions.assertTrue(box.getGlobalPose().getP().getY() > 0);

            // clean up
            scene.release();
            box.release();
            triMesh.release();
            mesh.release();
        }
    }
}
