package de.fabmax.physxjni;

import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.cooking.*;
import physx.extensions.PxDefaultAllocator;
import physx.geomutils.PxBoxGeometry;
import physx.geomutils.PxGeometry;
import physx.physics.*;

import java.util.Locale;

public class PhysXTestEnv {

    public static final int PX_PHYSICS_VERSION = PxTopLevelFunctions.getPHYSICS_VERSION();

    public static final PxFoundation foundation;
    public static final PxPhysics physics;
    public static final PxCooking cooking;

    public static final PxMaterial defaultMaterial;
    public static final PxFilterData defaultFilterData;

    static {
        // create PhysX foundation object
        PxDefaultAllocator allocator = new PxDefaultAllocator();
        PxDefaultErrorCallback errorCb = new PxDefaultErrorCallback();
        foundation = PxTopLevelFunctions.CreateFoundation(PX_PHYSICS_VERSION, allocator, errorCb);

        // create PhysX main physics object
        PxTolerancesScale tolerances = new PxTolerancesScale();
        physics = PxTopLevelFunctions.CreatePhysics(PX_PHYSICS_VERSION, foundation, tolerances);
        defaultMaterial = physics.createMaterial(0.5f, 0.5f, 0.5f);
        defaultFilterData = new PxFilterData(1, 1, 0, 0);

        PxCookingParams cookingParams = new PxCookingParams(tolerances);
        cooking = PxTopLevelFunctions.CreateCooking(PX_PHYSICS_VERSION, foundation, cookingParams);
    }

    public static PxScene createEmptyScene(int numThreads) {
        PxSceneDesc sceneDesc = new PxSceneDesc(physics.getTolerancesScale());
        sceneDesc.setGravity(new PxVec3(0f, -9.81f, 0f));
        sceneDesc.setCpuDispatcher(PxTopLevelFunctions.DefaultCpuDispatcherCreate(numThreads));
        sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
        sceneDesc.getFlags().set(PxSceneFlagEnum.eENABLE_CCD);
        return physics.createScene(sceneDesc);
    }

    public static PxRigidDynamic createDefaultBox(float posX, float posY, float posZ) {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxBoxGeometry box = PxBoxGeometry.createAt(mem, MemoryStack::nmalloc, 0.5f, 0.5f, 0.5f);
            PxTransform pose = PxTransform.createAt(mem, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity);
            pose.setP(PxVec3.createAt(mem, MemoryStack::nmalloc, posX, posY, posZ));

            PxShape shape = physics.createShape(box, defaultMaterial, true);
            PxRigidDynamic body = physics.createRigidDynamic(pose);
            shape.setSimulationFilterData(defaultFilterData);
            body.attachShape(shape);
            return body;
        }
    }

    public static PxRigidStatic createStaticBody(PxGeometry fromGeometry, float posX, float posY, float posZ) {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxTransform pose = PxTransform.createAt(mem, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity);
            pose.setP(PxVec3.createAt(mem, MemoryStack::nmalloc, posX, posY, posZ));

            PxShape shape = physics.createShape(fromGeometry, defaultMaterial, true);
            PxRigidStatic body = physics.createRigidStatic(pose);
            shape.setSimulationFilterData(defaultFilterData);
            body.attachShape(shape);
            return body;
        }
    }

    public static void simulateScene(PxScene scene, float duration, PxRigidActor printActor) {
        float step = 1/60f;
        float t = 0;
        for (int i = 0; i < duration / step; i++) {
            scene.simulate(step);
            scene.fetchResults(true);
            t += step;
            if (printActor != null && i % 30 == 0) {
                // print position 2 times per simulated sec
                PxVec3 pos = printActor.getGlobalPose().getP();
                System.out.printf(Locale.ENGLISH, "t = %.2f s, pos(%6.3f, %6.3f, %6.3f)\n", t, pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }
}
