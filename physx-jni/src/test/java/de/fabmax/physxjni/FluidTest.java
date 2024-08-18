package de.fabmax.physxjni;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import physx.common.*;
import physx.extensions.PxExtensionTopLevelFunctions;
import physx.extensions.PxRigidBodyExt;
import physx.geometry.PxBoxGeometry;
import physx.particles.*;
import physx.physics.PxRigidBody;
import physx.physics.PxScene;
import physx.physics.PxShape;
import physx.support.NativeArrayHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FluidTest {

    private static final PxCudaContextManager cudaMgr = CudaHelpers.getCudaContextManager();

    @BeforeAll
    public static void checkIsCudaAvailable() {
        Assumptions.assumeTrue(cudaMgr != null, "No CUDA support on this platform");
    }

    @Test
    public void fluidTest() {
        if (cudaMgr == null) {
            return;
        }
        var test = new FluidTestImpl();
        test.simulateFluidScene(1f);
        test.cleanup();
    }

    /**
     * Based on PhysX snippet SnippetPBF.cpp
     */
    private static class FluidTestImpl {

        private static final boolean USE_LARGE_FLUID = true;

        private static final float PARTICLE_SPACING = 0.1f;
        private static final float FLUID_DENSITY = 1000f;

        private final PxScene scene;
        private final List<PxRigidBody> boxes = new ArrayList<>();
        private PxParticleAndDiffuseBuffer particleBuffer;

        FluidTestImpl() {
            scene = CudaHelpers.createCudaEnabledScene(cudaMgr);

            var maxDiffuseParticles = USE_LARGE_FLUID ? 2_000_000 : 100_000;
            var numX = 50;
            var numY = USE_LARGE_FLUID ? 600 : 120;
            var numZ = 30;

            initParticles(numX, numY, numZ, -2.5f, 3f, 0.5f, maxDiffuseParticles);
            setupContainer();
            setupRigidBodies();
        }

        void setupContainer() {
            try (MemoryStack mem = MemoryStack.stackPush()) {
                var physics = PhysXTestEnv.physics;
                var mat = PhysXTestEnv.defaultMaterial;
                var fd = PhysXTestEnv.defaultFilterData;

                scene.addActor(PxExtensionTopLevelFunctions.CreatePlane(physics, PxPlane.createAt(mem, MemoryStack::nmalloc, 0.f, 1.f, 0.f, 0.0f), mat, fd));
                scene.addActor(PxExtensionTopLevelFunctions.CreatePlane(physics, PxPlane.createAt(mem, MemoryStack::nmalloc, -1.f, 0.f, 0.f, 7.5f), mat, fd));
                scene.addActor(PxExtensionTopLevelFunctions.CreatePlane(physics, PxPlane.createAt(mem, MemoryStack::nmalloc, 0.f, 0.f, 1.f, 7.5f), mat, fd));
                scene.addActor(PxExtensionTopLevelFunctions.CreatePlane(physics, PxPlane.createAt(mem, MemoryStack::nmalloc, 0.f, 0.f, -1.f, 7.5f), mat, fd));
                scene.addActor(PxExtensionTopLevelFunctions.CreatePlane(physics, PxPlane.createAt(mem, MemoryStack::nmalloc, 1.f, 0.f, 0.f, 7.5f), mat, fd));
            }
        }

        void setupRigidBodies() {
            try (MemoryStack mem = MemoryStack.stackPush()) {
                float dynamicsDensity = FLUID_DENSITY * 0.5f;
                float boxSize = 1f;
                float boxMass = boxSize * boxSize * boxSize * dynamicsDensity;
                PxShape shape = PhysXTestEnv.physics.createShape(PxBoxGeometry.createAt(mem, MemoryStack::nmalloc, boxSize * 0.5f, boxSize * 0.5f, boxSize * 0.5f), PhysXTestEnv.defaultMaterial);
                shape.setSimulationFilterData(PhysXTestEnv.defaultFilterData);
                for (int i = 0; i < 5; ++i) {
                    var pose = PxTransform.createAt(mem, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity);
                    pose.setP(PxVec3.createAt(mem, MemoryStack::nmalloc, i - 3.0f, 10, 7.5f));
                    var body = PhysXTestEnv.physics.createRigidDynamic(pose);
                    body.attachShape(shape);
                    PxRigidBodyExt.setMassAndUpdateInertia(body, boxMass);
                    scene.addActor(body);
                    boxes.add(body);
                }
                shape.release();
            }
        }

        void initParticles(int numX, int numY, int numZ, float posX, float posY, float posZ, int maxDiffuseParticles) {
            try (MemoryStack mem = MemoryStack.stackPush()) {
                int maxParticles = numX * numY * numZ;
                float restOffset = 0.5f * PARTICLE_SPACING / 0.6f;

                // Material setup
                PxPBDMaterial defaultMat = PhysXTestEnv.physics.createPBDMaterial(0.05f, 0.05f, 0f, 0.001f, 0.5f, 0.005f, 0.01f, 0f, 0f);
                defaultMat.setViscosity(0.001f);
                defaultMat.setSurfaceTension(0.00704f);
                defaultMat.setCohesion(0.0704f);
                defaultMat.setVorticityConfinement(10f);

                PxPBDParticleSystem particleSystem = PhysXTestEnv.physics.createPBDParticleSystem(cudaMgr, 96);

                // General particle system setting
                float solidRestOffset = restOffset;
                float fluidRestOffset = restOffset * 0.6f;
                float particleMass = FLUID_DENSITY * 1.333f * 3.14159f * PARTICLE_SPACING * PARTICLE_SPACING * PARTICLE_SPACING;
                particleSystem.setRestOffset(restOffset);
                particleSystem.setContactOffset(restOffset + 0.01f);
                particleSystem.setParticleContactOffset(fluidRestOffset / 0.6f);
                particleSystem.setSolidRestOffset(solidRestOffset);
                particleSystem.setFluidRestOffset(fluidRestOffset);
                particleSystem.enableCCD(false);
                particleSystem.setMaxVelocity(solidRestOffset * 100f);

                particleSystem.setSimulationFilterData(PhysXTestEnv.defaultFilterData);
                scene.addActor(particleSystem);

                // Diffuse particles setting
                PxDiffuseParticleParams dpParams = PxDiffuseParticleParams.createAt(mem, MemoryStack::nmalloc);
                dpParams.setThreshold(300.0f);
                dpParams.setBubbleDrag(0.9f);
                dpParams.setBuoyancy(0.9f);
                dpParams.setAirDrag(0.0f);
                dpParams.setKineticEnergyWeight(0.01f);
                dpParams.setPressureWeight(1.0f);
                dpParams.setDivergenceWeight(10.f);
                dpParams.setLifetime(1.0f);
                dpParams.setUseAccurateVelocity(false);

                // Create particles and add them to the particle system
                PxParticlePhaseFlags flags = PxParticlePhaseFlags.createAt(mem, MemoryStack::nmalloc, 0);
                flags.raise(PxParticlePhaseFlagEnum.eParticlePhaseFluid);
                flags.raise(PxParticlePhaseFlagEnum.eParticlePhaseSelfCollide);
                int particlePhase = particleSystem.createPhase(defaultMat, flags);

                // create pinned buffers for particle attributes (these are actually arrays, despite the awkward return types)
                var phase = PxCudaTopLevelFunctions.allocPinnedHostBufferPxU32(cudaMgr, maxParticles);
                var positionInvMass = PxCudaTopLevelFunctions.allocPinnedHostBufferPxVec4(cudaMgr, maxParticles);
                var velocity = PxCudaTopLevelFunctions.allocPinnedHostBufferPxVec4(cudaMgr, maxParticles);

                float x = posX;
                float y = posY;
                float z = posZ;

                for (int i = 0; i < numX; ++i) {
                    for (int j = 0; j < numY; ++j) {
                        for (int k = 0; k < numZ; ++k) {
				            int index = i * (numY * numZ) + j * numZ + k;

                            NativeArrayHelpers.setU32At(phase, index, particlePhase);
                            setXyzw(PxVec4.arrayGet(positionInvMass.getAddress(), index), x, y, z, 1f / particleMass);
                            setXyzw(PxVec4.arrayGet(velocity.getAddress(), index),0f, 0f, 0f, 0f);

                            z += PARTICLE_SPACING;
                        }
                        z = posZ;
                        y += PARTICLE_SPACING;
                    }
                    y = posY;
                    x += PARTICLE_SPACING;
                }

                PxParticleAndDiffuseBufferDesc bufferDesc = PxParticleAndDiffuseBufferDesc.createAt(mem, MemoryStack::nmalloc);
                bufferDesc.setMaxParticles(maxParticles);
                bufferDesc.setNumActiveParticles(maxParticles);
                bufferDesc.setMaxDiffuseParticles(maxDiffuseParticles);
                bufferDesc.setMaxActiveDiffuseParticles(maxDiffuseParticles);
                bufferDesc.setDiffuseParams(dpParams);

                bufferDesc.setPositions(positionInvMass);
                bufferDesc.setVelocities(velocity);
                bufferDesc.setPhases(NativeArrayHelpers.voidToU32Ptr(phase));

                particleBuffer = PxCudaTopLevelFunctions.CreateAndPopulateParticleAndDiffuseBuffer(bufferDesc, cudaMgr);
                particleSystem.addParticleBuffer(particleBuffer);

                PxCudaTopLevelFunctions.freePinnedHostBufferPxVec4(cudaMgr, positionInvMass);
                PxCudaTopLevelFunctions.freePinnedHostBufferPxVec4(cudaMgr, velocity);
                PxCudaTopLevelFunctions.freePinnedHostBufferPxU32(cudaMgr, NativeArrayHelpers.voidToU32Ptr(phase));
            }
        }

        void cleanup() {
            scene.release();
            particleBuffer.release();
        }

        void simulateFluidScene(float duration) {
            float step = 1/60f;
            float t = 0;
            for (int i = 0; i < duration / step; i++) {
                // print position of one of the boxes times per simulated sec
                if (i % 30 == 0) {
                    var pos = boxes.get(0).getGlobalPose().getP();
                    System.out.printf(Locale.ENGLISH, "t = %.2f s, pos(%6.3f, %6.3f, %6.3f)\n", t, pos.getX(), pos.getY(), pos.getZ());
                }
                scene.simulate(step);
                scene.fetchResults(true);
                t += step;
            }

            // boxes should float somewhere on the fluid
            boxes.forEach(box -> {
                var pos = box.getGlobalPose().getP();
                Assertions.assertTrue(pos.getY() > 0.55f);
            });
        }
    }

    private static PxVec4 setXyzw(PxVec4 target, float x, float y, float z, float w) {
        target.setX(x);
        target.setY(y);
        target.setZ(z);
        target.setW(w);
        return target;
    }
}
