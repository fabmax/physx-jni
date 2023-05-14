package de.fabmax.physxjni;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import physx.common.PxCudaContextManager;
import physx.common.PxCudaTopLevelFunctions;
import physx.common.PxVec4;
import physx.particles.*;
import physx.physics.PxRigidBody;
import physx.physics.PxScene;
import physx.support.NativeArrayHelpers;
import physx.support.Vector_PxU32;
import physx.support.Vector_PxVec4;

public class ClothTest {

    private static PxCudaContextManager cudaMgr = CudaHelpers.createCudaContextManager();
    private PxParticleClothBuffer clothBuffer;

    @BeforeAll
    public static void init() {
        cudaMgr = CudaHelpers.createCudaContextManager();
    }

    @Test
    public void simpleClothTest() {
        if (cudaMgr == null) {
            return;
        }
        var test = new SimpleClothTest();
        test.simulateClothScene(5f);
        test.cleanup();
    }

    @AfterAll
    public static void cleanup() {
        if (cudaMgr != null) {
            cudaMgr.release();
        }
    }

    /**
     * Based on PhysX snippet SnippetPBDCloth.cpp
     */
    private static class SimpleClothTest {
        private static final int NUM_X = 250;
        private static final int NUM_Z = 250;

        private static final float PARTICLE_SPACING = 0.05f;
        private static final float TOTAL_CLOTH_MASS = 10f;

        final PxScene scene;
        private PxParticleClothBuffer clothBuffer;

        SimpleClothTest() {
            scene = CudaHelpers.createCudaEnabledScene(cudaMgr);
            initCloth();
            addSceneBodies();
        }

        void cleanup() {
            scene.release();
            clothBuffer.release();
        }

        void simulateClothScene(float duration) {
            var positions = new Vector_PxVec4(clothBuffer.getNbActiveParticles());

            float step = 1/60f;
            float t = 0;
            for (int i = 0; i < duration / step; i++) {
                // print position of printActor 2 times per simulated sec
                if (i % 30 == 0) {
                    var devPositions = clothBuffer.getPositionInvMasses();
                    var numParticles = clothBuffer.getNbActiveParticles();

                    cudaMgr.acquireContext();
                    var cudaContext = cudaMgr.getCudaContext();
                    cudaContext.memcpyDtoH(positions.data(), PxCudaTopLevelFunctions.pxVec4deviceptr(devPositions), PxVec4.SIZEOF * numParticles);

                    var posCenter = positions.at(id(NUM_X/2, NUM_Z/2));
                    System.out.println("cloth center pos: " + posCenter.getX() + ", " + posCenter.getY() + ", " + posCenter.getZ());

                    cudaMgr.releaseContext();
                }
                scene.simulate(step);
                scene.fetchResults(true);
                t += step;
            }

            // after simulation cloth corners should be on the ground (+ rest offset), center on top of the cube
            var center = positions.at(id(NUM_X/2, NUM_Z/2));
            var corner1 = positions.at(id(0, 0));
            var corner2 = positions.at(id(0, NUM_Z-1));
            var corner3 = positions.at(id(NUM_X-1, NUM_Z-1));
            var corner4 = positions.at(id(NUM_X-1, 0));

            Assertions.assertEquals(1.05f, center.getY(), 0.01f);
            Assertions.assertEquals(0.05f, corner1.getY(), 0.01f);
            Assertions.assertEquals(0.05f, corner2.getY(), 0.01f);
            Assertions.assertEquals(0.05f, corner3.getY(), 0.01f);
            Assertions.assertEquals(0.05f, corner4.getY(), 0.01f);

            positions.destroy();
        }

        private int id(int x, int z) {
            return x * NUM_Z + z;
        }

        private void initCloth() {
            try (MemoryStack mem = MemoryStack.stackPush()) {
                var positionX = -0.5f * NUM_X * PARTICLE_SPACING;
                var positionY = 8f;
                var positionZ = -0.5f * NUM_Z * PARTICLE_SPACING;

                var numParticles = NUM_X * NUM_Z;
                var numSprings = (NUM_X - 1) * (NUM_Z - 1) * 4 + (NUM_X - 1) + (NUM_Z - 1);
                var numTriangles = (NUM_X - 1) * (NUM_Z - 1) * 2;

                var restOffset = PARTICLE_SPACING;
                var stretchStiffness = 10000f;
                var shearStiffness = 100f;
                var springDamping = 0.001f;

                var defaultMat = PhysXTestEnv.physics.createPBDMaterial(0.8f, 0.05f, 1e+6f, 0.001f, 0.5f, 0.005f, 0.05f, 0.f, 0.f);
                PxPBDParticleSystem particleSystem = PhysXTestEnv.physics.createPBDParticleSystem(cudaMgr);

                // General particle system setting
                var particleMass = TOTAL_CLOTH_MASS / numParticles;
                particleSystem.setRestOffset(restOffset);
                particleSystem.setContactOffset(restOffset + 0.02f);
                particleSystem.setParticleContactOffset(restOffset + 0.02f);
                particleSystem.setSolidRestOffset(restOffset);
                particleSystem.setFluidRestOffset(0f);

                particleSystem.setSimulationFilterData(PhysXTestEnv.defaultFilterData);
                scene.addActor(particleSystem);

                // Create particles and add them to the particle system
                PxParticlePhaseFlags flags = PxParticlePhaseFlags.createAt(mem, MemoryStack::nmalloc, 0);
                flags.raise(PxParticlePhaseFlagEnum.eParticlePhaseSelfCollideFilter);
                flags.raise(PxParticlePhaseFlagEnum.eParticlePhaseSelfCollide);
                var particlePhase = particleSystem.createPhase(defaultMat, flags);

                var clothBuffers = PxCudaTopLevelFunctions.CreateParticleClothBufferHelper(1, numTriangles, numSprings, numParticles, cudaMgr);

                // create pinned buffers for particle attributes (these are actually arrays, despite the awkward return types)
                var phase = PxCudaTopLevelFunctions.allocPinnedHostBufferPxU32(cudaMgr, numParticles);
                var positionInvMass = PxCudaTopLevelFunctions.allocPinnedHostBufferPxVec4(cudaMgr, numParticles);
                var velocity = PxCudaTopLevelFunctions.allocPinnedHostBufferPxVec4(cudaMgr, numParticles);
                var springs = new Vector_PxParticleSpring(numSprings);
                var triangles = new Vector_PxU32();

                var x = positionX;
                var y = positionY;
                var z = positionZ;
                var sqrt2 = (float) Math.sqrt(2.0);

                var vec4Buffer = PxVec4.createAt(mem, MemoryStack::nmalloc);
                var iSpring = 0;
                for (int i = 0; i < NUM_X; i++) {
                    for (int j = 0; j < NUM_Z; j++) {
                        var index = i * NUM_Z + j;
                        NativeArrayHelpers.setU32At(phase, index, particlePhase);
                        setXyzw(PxVec4.arrayGet(positionInvMass.getAddress(), index), x, y, z, 1f / particleMass);
                        setXyzw(PxVec4.arrayGet(velocity.getAddress(), index),0f, 0f, 0f, 0f);

                        if (i > 0) {
                            setSpring(springs.at(iSpring++), id(i - 1, j), id(i, j), PARTICLE_SPACING, stretchStiffness, springDamping);
                        }
                        if (j > 0) {
                            setSpring(springs.at(iSpring++), id(i, j - 1), id(i, j), PARTICLE_SPACING, stretchStiffness, springDamping);
                        }

                        if (i > 0 && j > 0) {
                            setSpring(springs.at(iSpring++), id(i - 1, j - 1), id(i, j), sqrt2 * PARTICLE_SPACING, shearStiffness, springDamping);
                            setSpring(springs.at(iSpring++), id(i - 1, j), id(i, j - 1), sqrt2 * PARTICLE_SPACING, shearStiffness, springDamping);

                            //Triangles are used to compute approximated aerodynamic forces for cloth falling down
                            triangles.push_back(id(i - 1, j - 1));
                            triangles.push_back(id(i - 1, j));
                            triangles.push_back(id(i, j - 1));

                            triangles.push_back(id(i - 1, j));
                            triangles.push_back(id(i, j - 1));
                            triangles.push_back(id(i, j));
                        }
                        z += PARTICLE_SPACING;
                    }
                    z = positionZ;
                    x += PARTICLE_SPACING;
                }

                Assertions.assertEquals(numSprings, springs.size());
                Assertions.assertEquals(numTriangles, triangles.size() / 3);

                clothBuffers.addCloth(0f, 0f, 0f, NativeArrayHelpers.voidToU32Ptr(triangles.data()), numTriangles, springs.data(), numSprings, positionInvMass, numParticles);

                var bufferDesc = PxParticleBufferDesc.createAt(mem, MemoryStack::nmalloc);
                bufferDesc.setMaxParticles(numParticles);
                bufferDesc.setNumActiveParticles(numParticles);
                bufferDesc.setPositions(positionInvMass);
                bufferDesc.setVelocities(velocity);
                bufferDesc.setPhases(NativeArrayHelpers.voidToU32Ptr(phase));

                var clothDesc = clothBuffers.getParticleClothDesc();
                var clothPreProcessor = PxCudaTopLevelFunctions.CreateParticleClothPreProcessor(cudaMgr);

                var output = PxPartitionedParticleCloth.createAt(mem, MemoryStack::nmalloc);
                clothPreProcessor.partitionSprings(clothDesc, output);
                clothPreProcessor.release();

                clothBuffer = PxCudaTopLevelFunctions.CreateAndPopulateParticleClothBuffer(bufferDesc, clothDesc, output, cudaMgr);
                particleSystem.addParticleBuffer(clothBuffer);

                clothBuffers.release();

                PxCudaTopLevelFunctions.freePinnedHostBufferPxVec4(cudaMgr, positionInvMass);
                PxCudaTopLevelFunctions.freePinnedHostBufferPxVec4(cudaMgr, velocity);
                PxCudaTopLevelFunctions.freePinnedHostBufferPxU32(cudaMgr, NativeArrayHelpers.voidToU32Ptr(phase));
                springs.destroy();
                triangles.destroy();
            }
        }

        private PxRigidBody addSceneBodies() {
            try (MemoryStack mem = MemoryStack.stackPush()) {
                scene.addActor(PhysXTestEnv.createGroundPlane());

                var box = PhysXTestEnv.createDefaultBox(0f, 2f, 0f);
                scene.addActor(box);
                return box;
            }
        }
    }

    private static PxVec4 setXyzw(PxVec4 target, float x, float y, float z, float w) {
        target.setX(x);
        target.setY(y);
        target.setZ(z);
        target.setW(w);
        return target;
    }

    private static PxParticleSpring setSpring(PxParticleSpring target, int ind0, int ind1, float length, float stiffness, float damping) {
        target.setInd0(ind0);
        target.setInd1(ind1);
        target.setLength(length);
        target.setStiffness(stiffness);
        target.setDamping(damping);
        target.setPad(0f);
        return target;
    }
}
