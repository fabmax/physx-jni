package de.fabmax.physxjni;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.common.PxIDENTITYEnum;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.PxOverlapResult;
import physx.physics.PxRigidStatic;
import physx.physics.PxScene;
import physx.physics.PxSceneDesc;

public class SceneQueryTest {
    @Test
    public void activeActorsTest() {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxSceneDesc sceneDesc = PxSceneDesc.createAt(mem, MemoryStack::nmalloc, PhysXTestEnv.physics.getTolerancesScale());
            sceneDesc.setGravity(new PxVec3(0f, -9.81f, 0f));
            sceneDesc.setCpuDispatcher(PhysXTestEnv.defaultDispatcher);
            sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
            PxScene scene = PhysXTestEnv.physics.createScene(sceneDesc);

            // create a few static boxes
            PxBoxGeometry staticBoxGeom = PxBoxGeometry.createAt(mem, MemoryStack::nmalloc, 1f, 1f, 1f);
            for (int y = -2; y <= 2; y++) {
                for (int x = -2; x <= 2; x++) {
                    PxRigidStatic staticBox = PhysXTestEnv.createStaticBody(staticBoxGeom, x * 4, y * 4, 0f);
                    scene.addActor(staticBox);
                }
            }

            PxOverlapResult queryResult = new PxOverlapResult();
            PxBoxGeometry querySphere = PxBoxGeometry.createAt(mem, MemoryStack::nmalloc, 8f, 8f, 1f);
            PxTransform queryPose = PxTransform.createAt(mem, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity);

            // shift a box overlap query through the boxes created before
            for (int i = 0; i < 5; i++) {
                PxVec3 pos = PxVec3.createAt(mem, MemoryStack::nmalloc, -16f + i * 4, 0f, 0f);
                queryPose.setP(pos);
                scene.overlap(querySphere, queryPose, queryResult);
                Assertions.assertEquals((i+1) * 5, queryResult.getNbAnyHits());
            }

            queryResult.clear();

            PxVec3 pos = PxVec3.createAt(mem, MemoryStack::nmalloc, 100f, 0f, 0f);
            queryPose.setP(pos);
            scene.overlap(querySphere, queryPose, queryResult);
            Assertions.assertEquals(0, queryResult.getNbAnyHits());

            scene.release();
            queryResult.destroy();
        }
    }
}
