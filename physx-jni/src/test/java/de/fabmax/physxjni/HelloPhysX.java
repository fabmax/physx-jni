package de.fabmax.physxjni;

import org.junit.jupiter.api.Assertions;
import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.geometry.PxBoxGeometry;
import physx.physics.*;

public class HelloPhysX {

    /**
     * Self-contained test / example running the whole chain from setting up PhysX to running the simulation
     * to final clean up.
     */
    public static void main(String[] args) {
        // get PhysX library version
        int version = PxTopLevelFunctions.getPHYSICS_VERSION();

        int versionMajor = version >> 24;
        int versionMinor = (version >> 16) & 0xff;
        int versionMicro = (version >> 8) & 0xff;
        System.out.printf("PhysX loaded, version: %d.%d.%d\n", versionMajor, versionMinor, versionMicro);

        // create PhysX foundation object
        PxDefaultAllocator allocator = new PxDefaultAllocator();
        PxDefaultErrorCallback errorCb = new PxDefaultErrorCallback();
        PxFoundation foundation = PxTopLevelFunctions.CreateFoundation(version, allocator, errorCb);

        // create PhysX main physics object
        PxTolerancesScale tolerances = new PxTolerancesScale();
        PxPhysics physics = PxTopLevelFunctions.CreatePhysics(version, foundation, tolerances);

        // create the CPU dispatcher, can be shared among multiple scenes
        int numThreads = 4;
        PxDefaultCpuDispatcher cpuDispatcher = PxTopLevelFunctions.DefaultCpuDispatcherCreate(numThreads);

        // create a physics scene
        PxVec3 tmpVec = new PxVec3(0f, -9.81f, 0f);
        PxSceneDesc sceneDesc = new PxSceneDesc(tolerances);
        sceneDesc.setGravity(tmpVec);
        sceneDesc.setCpuDispatcher(cpuDispatcher);
        sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
        PxScene scene = physics.createScene(sceneDesc);

        // create a default material
        PxMaterial material = physics.createMaterial(0.5f, 0.5f, 0.5f);
        // create default simulation shape flags
        PxShapeFlags shapeFlags = new PxShapeFlags((byte) (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value));

        // create a few temporary objects used during setup
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
        Assertions.assertEquals(boxHeight, 5f, 0.0001f);

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
        Assertions.assertEquals(1f, boxHeight, 0.0001f);

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
