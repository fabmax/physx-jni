package de.fabmax.physxjni;

import org.junit.Test;
import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.common.PxCudaContextManager;
import physx.common.PxCudaContextManagerDesc;
import physx.common.PxCudaInteropModeEnum;
import physx.common.PxVec3;
import physx.geomutils.PxBoxGeometry;
import physx.physics.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static de.fabmax.physxjni.PhysXTestEnv.foundation;
import static de.fabmax.physxjni.PhysXTestEnv.physics;

public class CudaTest {

    @Test
    public void createCudaContextTest() {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxCudaContextManagerDesc desc = PxCudaContextManagerDesc.createAt(mem, MemoryStack::nmalloc);
            desc.setInteropMode(PxCudaInteropModeEnum.NO_INTEROP);
            PxCudaContextManager cudaMgr = PxTopLevelFunctions.CreateCudaContextManager(foundation, desc);
            if (cudaMgr == null || !cudaMgr.contextIsValid()) {
                System.err.println("Failed creating CUDA context, no CUDA capable GPU?");
                // skip the test as this is probably caused by a missing CUDA hardware support and there
                // isn't much we can do about that
                return;
            }

            System.out.println("CUDA device: " + cudaMgr.getDeviceName() + ", mem: " + (cudaMgr.getDeviceTotalMemBytes() / 1024.0 / 1024.0) + " MB");

            System.out.println("Running CPU simulation...");
            double cpuTime = simulateScene(null);
            System.out.println("Running GPU simulation...");
            double gpuTime = simulateScene(cudaMgr);

            System.out.printf(Locale.ENGLISH, "GPU speed up: %.2f x\n", cpuTime / gpuTime);
        }
    }

    private double simulateScene(PxCudaContextManager cudaMgr) {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxSceneDesc sceneDesc = PxSceneDesc.createAt(mem, MemoryStack::nmalloc, physics.getTolerancesScale());
            sceneDesc.setGravity(new PxVec3(0f, -9.81f, 0f));
            sceneDesc.setCpuDispatcher(PxTopLevelFunctions.DefaultCpuDispatcherCreate(16));
            sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());

            if (cudaMgr != null) {
                // cuda related scene settings
                sceneDesc.setCudaContextManager(cudaMgr);
                sceneDesc.getFlags().set(PxSceneFlagEnum.eENABLE_GPU_DYNAMICS);
                sceneDesc.setBroadPhaseType(PxBroadPhaseTypeEnum.eGPU);
                sceneDesc.setGpuMaxNumPartitions(8);
            }

            PxScene scene = physics.createScene(sceneDesc);
            return simulate10kBodies(scene);
        }
    }

    private double simulate10kBodies(PxScene scene) {
        List<PxActor> actors = new ArrayList<>();

        try (MemoryStack mem = MemoryStack.stackPush()) {
            // Ground body
            PxBoxGeometry groundGeom = PxBoxGeometry.createAt(mem, MemoryStack::nmalloc, 100f, 1f, 100f);
            PxRigidStatic ground = PhysXTestEnv.createStaticBody(groundGeom, 0f, 0f, 0f);
            PxBoxGeometry wall1Geom = PxBoxGeometry.createAt(mem, MemoryStack::nmalloc, 100f, 20f, 1f);
            PxRigidStatic wall1 = PhysXTestEnv.createStaticBody(wall1Geom, 0f, 10f, 30f);
            PxBoxGeometry wall2Geom = PxBoxGeometry.createAt(mem, MemoryStack::nmalloc, 100f, 20f, 1f);
            PxRigidStatic wall2 = PhysXTestEnv.createStaticBody(wall2Geom, 0f, 10f, -30f);
            PxBoxGeometry wall3Geom = PxBoxGeometry.createAt(mem, MemoryStack::nmalloc, 1f, 20f, 100f);
            PxRigidStatic wall3 = PhysXTestEnv.createStaticBody(wall3Geom, 30f, 10f, 0f);
            PxBoxGeometry wall4Geom = PxBoxGeometry.createAt(mem, MemoryStack::nmalloc, 1f, 20f, 100f);
            PxRigidStatic wall4 = PhysXTestEnv.createStaticBody(wall4Geom, -30f, 10f, 0f);
            actors.add(ground);
            actors.add(wall1);
            actors.add(wall2);
            actors.add(wall3);
            actors.add(wall4);
        }

        // create 25 x 25 x 32 boxes = 20k bodies
        PxRigidDynamic printBox = null;
        for (int h = 0; h < 32; h++) {
            for (int x = -12; x <= 12; x++) {
                for (int z = -12; z <= 12; z++) {
                    PxRigidDynamic box = PhysXTestEnv.createDefaultBox(x * 2f + x % 2 * 0.5f, 5f + h * 2f, z * 2f + z % 2 * 0.5f);
                    printBox = box;
                    actors.add(box);
                }
            }
        }

        actors.forEach(scene::addActor);

        System.out.println("Simulating " + actors.size() + " actors for 15 secs...");
        long t = System.nanoTime();
        PhysXTestEnv.simulateScene(scene, 15f, printBox);
        t = System.nanoTime() - t;
        System.out.printf(Locale.ENGLISH, "Done, took: %.3f s\n", t / 1e9);

        scene.release();
        actors.forEach(PxActor::release);
        return t / 1e9;
    }
}
