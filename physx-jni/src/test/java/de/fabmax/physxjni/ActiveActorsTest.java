package de.fabmax.physxjni;

import org.junit.Assert;
import org.junit.Test;
import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.common.PxVec3;
import physx.geomutils.PxBoxGeometry;
import physx.physics.*;
import physx.support.SupportFunctions;
import physx.support.Vector_PxActorPtr;

public class ActiveActorsTest {

    @Test
    public void activeActorsTest() {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxPhysics physics = PhysXTestEnv.physics;

            // create scene with enabled active actors flag
            PxSceneDesc sceneDesc = PxSceneDesc.createAt(mem, MemoryStack::nmalloc, physics.getTolerancesScale());
            sceneDesc.setGravity(new PxVec3(0f, -9.81f, 0f));
            sceneDesc.setCpuDispatcher(PxTopLevelFunctions.DefaultCpuDispatcherCreate(1));
            sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
            sceneDesc.getFlags().set(PxSceneFlagEnum.eENABLE_ACTIVE_ACTORS);
            PxScene scene = physics.createScene(sceneDesc);

            // create a falling box (will be the active actor)
            PxRigidDynamic activeBox = PhysXTestEnv.createDefaultBox(0f, 5f, 0f);
            scene.addActor(activeBox);

            // create a static box (is not active)
            PxBoxGeometry staticBoxGeom = PxBoxGeometry.createAt(mem, MemoryStack::nmalloc, 1f, 1f, 1f);
            PxRigidStatic staticBox = PhysXTestEnv.createStaticBody(staticBoxGeom, 10f, 0f, 0f);
            scene.addActor(staticBox);

            PhysXTestEnv.simulateScene(scene, 5f, activeBox);

            Vector_PxActorPtr activeActors = SupportFunctions.PxScene_getActiveActors(scene);

            // there should be exactly one activbe actor (the falling box)
            Assert.assertEquals(activeActors.size(), 1);
            Assert.assertEquals(activeActors.at(0), activeBox);
        }
    }

}
