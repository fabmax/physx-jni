package de.fabmax.physxjni;

import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.cooking.PxCooking;
import physx.cooking.PxCookingParams;
import physx.geometry.PxBoxGeometry;
import physx.geometry.PxGeometry;
import physx.geometry.PxPlaneGeometry;
import physx.physics.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PhysXTestEnv {

    public static final int PX_PHYSICS_VERSION = PxTopLevelFunctions.getPHYSICS_VERSION();

    public static final PxFoundation foundation;
    public static final PxPhysics physics;
    public static final PxCookingParams cookingParams;
    public static final PxCooking cooking;

    public static final PxMaterial defaultMaterial;
    public static final PxFilterData defaultFilterData;

    static class CustomErrorCallback extends PxErrorCallbackImpl {
        private final Map<Integer, String> codeNames = new HashMap<>() {{
            put(PxErrorCodeEnum.eDEBUG_INFO, "DEBUG_INFO");
            put(PxErrorCodeEnum.eDEBUG_WARNING, "DEBUG_WARNING");
            put(PxErrorCodeEnum.eINVALID_PARAMETER, "INVALID_PARAMETER");
            put(PxErrorCodeEnum.eINVALID_OPERATION, "INVALID_OPERATION");
            put(PxErrorCodeEnum.eOUT_OF_MEMORY, "OUT_OF_MEMORY");
            put(PxErrorCodeEnum.eINTERNAL_ERROR, "INTERNAL_ERROR");
            put(PxErrorCodeEnum.eABORT, "ABORT");
            put(PxErrorCodeEnum.ePERF_WARNING, "PERF_WARNING");
        }};

        @Override
        public void reportError(int code, String message, String file, int line) {
            String codeName = codeNames.getOrDefault(code, "code: " + code);
            System.out.printf("[%s] %s (%s:%d)\n", codeName, message, file, line);
        }
    }

    static {
        // create PhysX foundation object
        PxDefaultAllocator allocator = new PxDefaultAllocator();
        PxErrorCallback errorCb = new CustomErrorCallback();
        foundation = PxTopLevelFunctions.CreateFoundation(PX_PHYSICS_VERSION, allocator, errorCb);

        // create PhysX main physics object
        PxTolerancesScale tolerances = new PxTolerancesScale();
        physics = PxTopLevelFunctions.CreatePhysics(PX_PHYSICS_VERSION, foundation, tolerances);
        defaultMaterial = physics.createMaterial(0.5f, 0.5f, 0.5f);
        defaultFilterData = new PxFilterData(0, 0, 0, 0);
        defaultFilterData.setWord0(1);          // collision group: 0 (i.e. 1 << 0)
        defaultFilterData.setWord1(0xffffffff); // collision mask: collide with everything
        defaultFilterData.setWord2(0);          // no additional collision flags
        defaultFilterData.setWord3(0);          // word3 is currently not used

        cookingParams = new PxCookingParams(tolerances);
        cooking = PxTopLevelFunctions.CreateCooking(PX_PHYSICS_VERSION, foundation, cookingParams);

        PxTopLevelFunctions.InitExtensions(physics);
    }

    public static PxScene createEmptyScene(int numThreads) {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxSceneDesc sceneDesc = PxSceneDesc.createAt(mem, MemoryStack::nmalloc, physics.getTolerancesScale());
            sceneDesc.setGravity(new PxVec3(0f, -9.81f, 0f));
            sceneDesc.setCpuDispatcher(PxTopLevelFunctions.DefaultCpuDispatcherCreate(numThreads));
            sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
            return physics.createScene(sceneDesc);
        }
    }

    public static PxRigidDynamic createDefaultBox(float posX, float posY, float posZ) {
        return createDefaultBox(posX, posY, posZ, defaultFilterData);
    }

    public static PxRigidDynamic createDefaultBox(float posX, float posY, float posZ, PxFilterData simFilterData) {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxBoxGeometry box = PxBoxGeometry.createAt(mem, MemoryStack::nmalloc, 0.5f, 0.5f, 0.5f);
            PxTransform pose = PxTransform.createAt(mem, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity);
            pose.setP(PxVec3.createAt(mem, MemoryStack::nmalloc, posX, posY, posZ));

            PxShape shape = physics.createShape(box, defaultMaterial, true);
            PxRigidDynamic body = physics.createRigidDynamic(pose);
            shape.setSimulationFilterData(simFilterData);
            body.attachShape(shape);
            return body;
        }
    }

    public static PxRigidStatic createGroundPlane() {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxPlaneGeometry plane = PxPlaneGeometry.createAt(mem, MemoryStack::nmalloc);
            PxShape shape = physics.createShape(plane, defaultMaterial, true);

            float r = 1f / (float) Math.sqrt(2f);
            PxQuat q = PxQuat.createAt(mem, MemoryStack::nmalloc, 0f, 0f, r, r);
            PxVec3 p = PxVec3.createAt(mem, MemoryStack::nmalloc, 0f, 0f, 0f);
            shape.setLocalPose(PxTransform.createAt(mem, MemoryStack::nmalloc, p, q));
            return createStaticBody(shape, 0f, 0f, 0f);
        }
    }

    public static PxRigidStatic createStaticBody(PxGeometry fromGeometry, float posX, float posY, float posZ) {
        PxShape shape = physics.createShape(fromGeometry, defaultMaterial, true);
        shape.setSimulationFilterData(defaultFilterData);
        return createStaticBody(shape, posX, posY, posZ);
    }

    public static PxRigidStatic createStaticBody(PxShape fromShape, float posX, float posY, float posZ) {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxTransform pose = PxTransform.createAt(mem, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity);
            pose.setP(PxVec3.createAt(mem, MemoryStack::nmalloc, posX, posY, posZ));

            PxRigidStatic body = physics.createRigidStatic(pose);
            body.attachShape(fromShape);
            return body;
        }
    }

    public static void simulateScene(PxScene scene, float duration, PxRigidActor printActor) {
        float step = 1/60f;
        float t = 0;
        for (int i = 0; i < duration / step; i++) {
            // print position of printActor 2 times per simulated sec
            if (printActor != null && i % 30 == 0) {
                PxVec3 pos = printActor.getGlobalPose().getP();
                System.out.printf(Locale.ENGLISH, "t = %.2f s, pos(%6.3f, %6.3f, %6.3f)\n", t, pos.getX(), pos.getY(), pos.getZ());
            }
            scene.simulate(step);
            scene.fetchResults(true);
            t += step;
        }
    }
}
