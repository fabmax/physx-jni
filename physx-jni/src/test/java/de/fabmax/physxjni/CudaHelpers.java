package de.fabmax.physxjni;

/*import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.common.PxCudaContextManager;
import physx.common.PxCudaContextManagerDesc;
import physx.common.PxCudaTopLevelFunctions;
import physx.common.PxVec3;
import physx.physics.*;

import static de.fabmax.physxjni.PhysXTestEnv.foundation;
import static de.fabmax.physxjni.PhysXTestEnv.physics;

public class CudaHelpers {

    private static PxCudaContextManager cudaMgr;

    public static boolean isAvailable() {
        if (Platform.getPlatform() == Platform.MACOS || Platform.getPlatform() == Platform.MACOS_ARM64) {
            // no CUDA support on macOS
            return false;
        }
        return PxCudaTopLevelFunctions.GetSuggestedCudaDeviceOrdinal(PhysXTestEnv.foundation) >= 0;
    }

    public static PxCudaContextManager getCudaContextManager() {
        if (cudaMgr != null && cudaMgr.contextIsValid()) {
            return cudaMgr;
        }

        if (!isAvailable()) {
            System.err.println("CUDA is not available or disabled on this platform");
            return null;
        }

        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxCudaContextManagerDesc desc = PxCudaContextManagerDesc.createAt(mem, MemoryStack::nmalloc);
            cudaMgr = PxCudaTopLevelFunctions.CreateCudaContextManager(foundation, desc);
            if (cudaMgr == null || !cudaMgr.contextIsValid()) {
                System.err.println("Failed creating CUDA context, no CUDA capable GPU?");
                cudaMgr = null;
            }
            return cudaMgr;
        }
    }

    public static PxScene createCudaEnabledScene(PxCudaContextManager cudaMgr) {
        if (cudaMgr == null) {
            throw new IllegalArgumentException("PxCudaContextManager must be non-null");
        }

        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxSceneDesc sceneDesc = PxSceneDesc.createAt(mem, MemoryStack::nmalloc, physics.getTolerancesScale());
            sceneDesc.setGravity(new PxVec3(0f, -9.81f, 0f));
            sceneDesc.setCpuDispatcher(PhysXTestEnv.defaultDispatcher);
            sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
            sceneDesc.setCudaContextManager(cudaMgr);
            sceneDesc.setStaticStructure(PxPruningStructureTypeEnum.eDYNAMIC_AABB_TREE);
            sceneDesc.getFlags().raise(PxSceneFlagEnum.eENABLE_PCM);
            sceneDesc.getFlags().raise(PxSceneFlagEnum.eENABLE_GPU_DYNAMICS);
            sceneDesc.setBroadPhaseType(PxBroadPhaseTypeEnum.eGPU);
            sceneDesc.setSolverType(PxSolverTypeEnum.eTGS);
            return physics.createScene(sceneDesc);
        }
    }
}*/
