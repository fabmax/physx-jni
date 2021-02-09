package de.fabmax.physxjni;

import org.junit.Assert;
import org.junit.Test;
import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.extensions.PxDefaultAllocator;
import physx.geomutils.PxBoxGeometry;
import physx.physics.*;

public class PhysXTest {

    /**
     * Self contained test / example running the whole chain from setting up PhysX to running the simulation
     * to final clean up.
     */
    @Test
    public void testBasic() {
        // get PhysX library version
        int version = PxTopLevelFunctions.getPHYSICS_VERSION();

        // create PhysX foundation object
        PxDefaultAllocator allocator = new PxDefaultAllocator();
        PxDefaultErrorCallback errorCb = new PxDefaultErrorCallback();
        PxFoundation foundation = PxTopLevelFunctions.CreateFoundation(version, allocator, errorCb);

        // create PhysX main physics object
        PxTolerancesScale tolerances = new PxTolerancesScale();
        PxPhysics physics = PxTopLevelFunctions.CreatePhysics(version, foundation, tolerances);

        // create a physics scene
        int numThreads = 4;
        PxSceneDesc sceneDesc = new PxSceneDesc(tolerances);
        sceneDesc.setGravity(new PxVec3(0f, -9.81f, 0f));
        sceneDesc.setCpuDispatcher(PxTopLevelFunctions.DefaultCpuDispatcherCreate(numThreads));
        sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
        sceneDesc.getFlags().set(PxSceneFlagEnum.eENABLE_CCD);
        PxScene scene = physics.createScene(sceneDesc);

        // create a default material
        PxMaterial material = physics.createMaterial(0.5f, 0.5f, 0.5f);
        // create default simulation shape flags
        PxShapeFlags shapeFlags = new PxShapeFlags((byte) (PxShapeFlagEnum.eSCENE_QUERY_SHAPE | PxShapeFlagEnum.eSIMULATION_SHAPE));

        // create a few temporary objects used during setup
        PxVec3 tmpVec = new PxVec3(0f, 0f, 0f);
        PxTransform tmpPose = new PxTransform(PxIDENTITYEnum.PxIdentity);
        PxFilterData tmpFilterData = new PxFilterData(1, 1, 0, 0);

        // create a large static box with size 20x1x20 as ground
        PxBoxGeometry groundGeometry = new PxBoxGeometry(10f, 0.5f, 10f);   // PxBoxGeometry uses half-sizes
        PxShape groundShape = physics.createShape(groundGeometry, material, true, shapeFlags);
        PxRigidStatic ground = physics.createRigidStatic(tmpPose);
        groundShape.setSimulationFilterData(tmpFilterData);
        ground.attachShape(groundShape);
        scene.addActor(ground);

        // create a small dynamic box with size 1x1x1, which will fall on the ground
        tmpVec.setX(0f); tmpVec.setY(5f); tmpVec.setZ(0f);
        tmpPose.setP(tmpVec);
        PxBoxGeometry boxGeometry = new PxBoxGeometry(0.5f, 0.5f, 0.5f);   // PxBoxGeometry uses half-sizes
        PxShape boxShape = physics.createShape(boxGeometry, material, true, shapeFlags);
        PxRigidDynamic box = physics.createRigidDynamic(tmpPose);
        boxShape.setSimulationFilterData(tmpFilterData);
        box.attachShape(boxShape);
        scene.addActor(box);

        // clean up temp objects
        groundGeometry.destroy();
        boxGeometry.destroy();
        tmpFilterData.destroy();
        tmpPose.destroy();
        tmpVec.destroy();
        shapeFlags.destroy();
        sceneDesc.destroy();
        tolerances.destroy();

        // box starts at a height of 5
        float boxHeight = box.getGlobalPose().getP().getY();
        Assert.assertEquals(boxHeight, 5f, 0.0001f);

        // run physics simulation
        for (int i = 0; i <= 500; i++) {
            scene.simulate(1f/60f);
            scene.fetchResults(true);

            boxHeight = box.getGlobalPose().getP().getY();
            if (i % 10 == 0) {
                System.out.println("Step " + i + ": h = " + boxHeight);
            }
        }

        // box should rest on the ground
        Assert.assertEquals(boxHeight, 1f, 0.0001f);

        // cleanup stuff
        scene.removeActor(ground);
        ground.release();
        groundShape.release();

        scene.removeActor(box);
        box.release();
        boxShape.release();

        scene.release();
        material.release();
        physics.release();
        foundation.release();
        errorCb.destroy();
        allocator.destroy();
    }
}
