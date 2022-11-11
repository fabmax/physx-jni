package de.fabmax.physxjni;

public class CudaTest {
/*
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

            int[] numBodies1000 = new int[] { 1, 5, 10 };
            double[] cpuTimes = new double[numBodies1000.length];
            double[] gpuTimes = new double[numBodies1000.length];

            for (int i = 0; i < numBodies1000.length; i++) {
                System.out.println("Running CPU simulation...");
                cpuTimes[i] = simulateScene(null, numBodies1000[i]);
                System.out.println("Running GPU simulation...");
                gpuTimes[i] = simulateScene(cudaMgr, numBodies1000[i]);
            }

            System.out.println();
            System.out.println("# Bodies | CPU Time | GPU Time | GPU speed up");
            System.out.println("---------+----------+----------+-------------");
            for (int i = 0; i < numBodies1000.length; i++) {
                int n = numBodies1000[i] * 1000;
                double cpuTime = cpuTimes[i];
                double gpuTime = gpuTimes[i];
                System.out.printf(Locale.ENGLISH, " %7d |%7.3f s |%7.3f s | %.2f x\n",
                        n, cpuTime, gpuTime, cpuTime / gpuTime);
            }
        }
    }

    private double simulateScene(PxCudaContextManager cudaMgr, int numBodies1000) {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxSceneDesc sceneDesc = PxSceneDesc.createAt(mem, MemoryStack::nmalloc, physics.getTolerancesScale());
            sceneDesc.setGravity(new PxVec3(0f, -9.81f, 0f));
            sceneDesc.setCpuDispatcher(PxTopLevelFunctions.DefaultCpuDispatcherCreate(8));
            sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
            sceneDesc.getFlags().clear(PxSceneFlagEnum.eENABLE_PCM);

            if (cudaMgr != null) {
                // cuda related scene settings
                sceneDesc.setCudaContextManager(cudaMgr);
                sceneDesc.getFlags().set(PxSceneFlagEnum.eENABLE_GPU_DYNAMICS);
                sceneDesc.setBroadPhaseType(PxBroadPhaseTypeEnum.eGPU);
                sceneDesc.setGpuMaxNumPartitions(8);
            }

            PxScene scene = physics.createScene(sceneDesc);
            return simulateBodies(scene, numBodies1000);
        }
    }

    private double simulateBodies(PxScene scene, int numBodies1000) {
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

        PxRigidDynamic printBox = null;
        for (int h = 0; h < 2 * numBodies1000; h++) {
            for (int x = -10; x < 10; x++) {
                for (int z = -12; z <= 12; z++) {
                    PxRigidDynamic box = PhysXTestEnv.createDefaultBox(x * 2f + x % 2 * 0.5f, 5f + h * 2f, z * 2f + z % 2 * 0.5f);
                    printBox = box;
                    actors.add(box);
                }
            }
        }

        actors.forEach(scene::addActor);

        // do initial sim step before starting the timer
        scene.simulate(1f / 60f);
        scene.fetchResults(true);

        float tSim = 10f;
        System.out.printf(Locale.ENGLISH, "Simulating %d actors for %.0f secs...\n", actors.size(), tSim);
        long t = System.nanoTime();
        PhysXTestEnv.simulateScene(scene, tSim, printBox);
        t = System.nanoTime() - t;
        System.out.printf(Locale.ENGLISH, "Done, took: %.3f s\n", t / 1e9);

        scene.release();
        actors.forEach(PxActor::release);
        return t / 1e9;
    }*/
}
