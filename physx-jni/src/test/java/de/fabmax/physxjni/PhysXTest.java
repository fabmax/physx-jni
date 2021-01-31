package de.fabmax.physxjni;

import org.junit.Test;
import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.extensions.PxDefaultAllocator;
import physx.geomutils.PxBoxGeometry;
import physx.physics.*;

public class PhysXTest {

    @Test
    public void testBasic() {
        int version = PxTopLevelFunctions.getPHYSICS_VERSION();

        PxDefaultAllocator allocator = new PxDefaultAllocator();
        PxDefaultErrorCallback errorCb = new PxDefaultErrorCallback();
        PxFoundation foundation = PxTopLevelFunctions.CreateFoundation(version, allocator, errorCb);
        System.out.println("foundation created");


        PxTolerancesScale tolerances = new PxTolerancesScale();
        PxPhysics physics = PxTopLevelFunctions.CreatePhysics(version, foundation, tolerances);

        PxSceneDesc sceneDesc = new PxSceneDesc(tolerances);
        sceneDesc.setGravity(new PxVec3(0f, -9.81f, 0f));
        sceneDesc.setCpuDispatcher(PxTopLevelFunctions.DefaultCpuDispatcherCreate(0));
        sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
        sceneDesc.getFlags().set(PxSceneFlagEnum.eENABLE_CCD);
        PxScene scene = physics.createScene(sceneDesc);
        System.out.println("scene created");


        PxTransform pose = new PxTransform(PxIDENTITYEnum.PxIdentity);
        PxRigidDynamic body = physics.createRigidDynamic(pose);
        pose.destroy();
        System.out.println("destroyed pose");

        PxBoxGeometry box = new PxBoxGeometry(1f, 1f, 1f);
        PxMaterial material = physics.createMaterial(0.5f, 0.5f, 0.5f);
        PxShapeFlags flags = new PxShapeFlags((byte) (PxShapeFlagEnum.eSCENE_QUERY_SHAPE | PxShapeFlagEnum.eSIMULATION_SHAPE));
        PxShape shape = physics.createShape(box, material, true, flags);
        body.attachShape(shape);
        scene.addActor(body);
        System.out.println("body added");

        long time = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            //PxVec3 pos = body.getGlobalPose().getP();
            //System.out.println("simulate " + i + ": (" + pos.getX() + ",  " + pos.getY() + ", " + pos.getZ() + ")");
            scene.simulate(1f/60f);
            scene.fetchResults(true);
        }
        time = System.nanoTime() - time;
        System.out.println("took " + time / 1e6 + " ms");
        PxVec3 pos = body.getGlobalPose().getP();
        System.out.println("final pos: (" + pos.getX() + ",  " + pos.getY() + ", " + pos.getZ() + ")");
    }

}
