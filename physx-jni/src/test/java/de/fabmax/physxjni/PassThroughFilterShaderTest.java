package de.fabmax.physxjni;

import org.junit.Assert;
import org.junit.Test;
import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.common.PxVec3;
import physx.extensions.PxDefaultCpuDispatcher;
import physx.geomutils.PxBoxGeometry;
import physx.physics.*;
import physx.support.JavaPassThroughFilterShader;

import java.util.concurrent.atomic.AtomicInteger;

public class PassThroughFilterShaderTest {

    @Test
    public void passThroughShaderTest() {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxSceneDesc sceneDesc = PxSceneDesc.createAt(mem, MemoryStack::nmalloc, PhysXTestEnv.physics.getTolerancesScale());
            sceneDesc.setGravity(new PxVec3(0f, -9.81f, 0f));

            PxDefaultCpuDispatcher cpuDispatcher = PxTopLevelFunctions.DefaultCpuDispatcherCreate(4);
            sceneDesc.setCpuDispatcher(cpuDispatcher);

            TestFilterShader shader = new TestFilterShader();
            PxTopLevelFunctions.setupPassThroughFilterShader(sceneDesc, shader);

            PxScene scene = PhysXTestEnv.physics.createScene(sceneDesc);

            PxRigidDynamic box = PhysXTestEnv.createDefaultBox(0f, 5f, 0f);
            scene.addActor(box);

            // create default ground box, nothing special here
            PxBoxGeometry groundGeom = PxBoxGeometry.createAt(mem, MemoryStack::nmalloc, 10f, 0.5f, 10f);
            PxRigidStatic ground = PhysXTestEnv.createStaticBody(groundGeom, 0f, 0f, 0f);
            scene.addActor(ground);

            PhysXTestEnv.simulateScene(scene, 5f, box);

            System.out.println("Filter shader was called " + shader.shaderCalls.get() + " times");
            Assert.assertTrue(shader.shaderCalls.get() > 0);

            scene.release();
            ground.release();
            box.release();
            shader.destroy();
        }
    }

    static class TestFilterShader extends JavaPassThroughFilterShader {
        final AtomicInteger shaderCalls = new AtomicInteger();

        /**
         * This is the Java equivalent to the native DefaultFilterShader
         */
        @Override
        public int filterShader(int attributes0, int filterData0w0, int filterData0w1, int filterData0w2, int filterData0w3,
                                int attributes1, int filterData1w0, int filterData1w1, int filterData1w2, int filterData1w3) {
            shaderCalls.incrementAndGet();

            if ((0 == (filterData0w0 & filterData1w1)) && (0 == (filterData1w0 & filterData0w1))) {
                return PxFilterFlagEnum.eSUPPRESS;
            }

            int outPairFlags;
            if ((attributes0 & PxFilterObjectFlagEnum.eTRIGGER) != 0 || (attributes1 & PxFilterObjectFlagEnum.eTRIGGER) != 0) {
                outPairFlags = PxPairFlagEnum.eTRIGGER_DEFAULT;
            } else {
                outPairFlags = PxPairFlagEnum.eCONTACT_DEFAULT;
            }
            setOutputPairFlags(outPairFlags | filterData0w2 | filterData1w2);

            return PxFilterFlagEnum.eDEFAULT;
        }
    }
}
