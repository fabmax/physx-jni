package de.fabmax.physxjni;

import org.junit.jupiter.api.Assertions;
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

/**
 * Based on PhysX snippet SnippetPBDCloth.cpp
 */
public class ParticlesTest {

    private PxParticleClothBuffer clothBuffer;

    @Test
    public void particlesTest() {
        PxCudaContextManager cudaMgr = CudaHelpers.createCudaContextManager();
        if (cudaMgr == null) {
            return;
        }

        System.out.println("create scene");
        var scene = CudaHelpers.createCudaEnabledScene(cudaMgr);
        System.out.println("init cloth");
        initCloth(scene, cudaMgr);
        addSceneBodies(scene);

        System.out.println("sim scene");
        simulateScene(scene, 5f, cudaMgr);
        System.out.println("done");
    }

    private void simulateScene(PxScene scene, float duration, PxCudaContextManager cudaMgr) {
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

                var posCenter = positions.at(id(125, 125, 250));
                System.out.println("center: " + posCenter.getX() + ", " + posCenter.getY() + ", " + posCenter.getZ());

                cudaMgr.releaseContext();
            }
            scene.simulate(step);
            scene.fetchResults(true);
            t += step;
        }

        positions.destroy();
    }

    private void initCloth(PxScene scene, PxCudaContextManager cudaMgr) {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            var numX = 250;
            var numZ = 250;
            var particleSpacing = 0.05f;
            var positionX = -0.5f * numX * particleSpacing;
            var positionY = 8f;
            var positionZ = -0.5f * numZ * particleSpacing;
            var totalClothMass = 10f;

            var numParticles = numX * numZ;
            var numSprings = (numX - 1) * (numZ - 1) * 4 + (numX - 1) + (numZ - 1);
            var numTriangles = (numX - 1) * (numZ - 1) * 2;

            var restOffset = particleSpacing;
            var stretchStiffness = 10000f;
            var shearStiffness = 100f;
            var springDamping = 0.001f;

            var defaultMat = PhysXTestEnv.physics.createPBDMaterial(0.8f, 0.05f, 1e+6f, 0.001f, 0.5f, 0.005f, 0.05f, 0.f, 0.f);
            PxPBDParticleSystem particleSystem = PhysXTestEnv.physics.createPBDParticleSystem(cudaMgr);

            // General particle system setting
            var particleMass = totalClothMass / numParticles;
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
            for (int i = 0; i < numX; i++) {
                for (int j = 0; j < numZ; j++) {
                    var index = i * numZ + j;
                    NativeArrayHelpers.setU32At(phase, index, particlePhase);
                    setXyzw(PxVec4.arrayGet(positionInvMass.getAddress(), index), x, y, z, 1f / particleMass);
                    setXyzw(PxVec4.arrayGet(velocity.getAddress(), index),0f, 0f, 0f, 0f);

                    if (i > 0) {
                        setSpring(springs.at(iSpring++), id(i - 1, j, numZ), id(i, j, numZ), particleSpacing, stretchStiffness, springDamping);
                    }
                    if (j > 0) {
                        setSpring(springs.at(iSpring++), id(i, j - 1, numZ), id(i, j, numZ), particleSpacing, stretchStiffness, springDamping);
                    }

                    if (i > 0 && j > 0) {
                        setSpring(springs.at(iSpring++), id(i - 1, j - 1, numZ), id(i, j, numZ), sqrt2 * particleSpacing, stretchStiffness, springDamping);
                        setSpring(springs.at(iSpring++), id(i - 1, j, numZ), id(i, j - 1, numZ), sqrt2 * particleSpacing, stretchStiffness, springDamping);

                        //Triangles are used to compute approximated aerodynamic forces for cloth falling down
                        triangles.push_back(id(i - 1, j - 1, numZ));
                        triangles.push_back(id(i - 1, j, numZ));
                        triangles.push_back(id(i, j - 1, numZ));

                        triangles.push_back(id(i - 1, j, numZ));
                        triangles.push_back(id(i, j - 1, numZ));
                        triangles.push_back(id(i, j, numZ));
                    }
                    z += particleSpacing;
                }
                z = positionZ;
                x += particleSpacing;
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

    private int id(int x, int y, int numY) {
        return x * numY + y;
    }

    private PxVec4 setXyzw(PxVec4 target, float x, float y, float z, float w) {
        target.setX(x);
        target.setY(y);
        target.setZ(z);
        target.setW(w);
        return target;
    }

    private PxParticleSpring setSpring(PxParticleSpring target, int ind0, int ind1, float length, float stiffness, float damping) {
        target.setInd0(ind0);
        target.setInd1(ind1);
        target.setLength(length);
        target.setStiffness(stiffness);
        target.setDamping(damping);
        target.setPad(0f);
        return target;
    }

    private PxRigidBody addSceneBodies(PxScene scene) {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            scene.addActor(PhysXTestEnv.createGroundPlane());

            var box = PhysXTestEnv.createDefaultBox(0f, 5f, 0f);
            scene.addActor(box);
            return box;
        }
    }
}
