package de.fabmax.physxjni;

import org.junit.Assert;
import org.junit.Test;
import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.common.PxVec3;
import physx.geomutils.PxBoxGeometry;
import physx.physics.*;
import physx.support.TypeHelpers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SimCallbackTest {

    @Test
    public void simCallbackTest() {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            PxPhysics physics = PhysXTestEnv.physics;

            TestSimulationCallback simCallback = new TestSimulationCallback();

            // create scene with custom simulation callback
            PxSceneDesc sceneDesc = PxSceneDesc.createAt(mem, MemoryStack::nmalloc, physics.getTolerancesScale());
            sceneDesc.setSimulationEventCallback(simCallback);
            sceneDesc.setGravity(new PxVec3(0f, -9.81f, 0f));
            sceneDesc.setCpuDispatcher(PxTopLevelFunctions.DefaultCpuDispatcherCreate(1));
            sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
            PxScene scene = physics.createScene(sceneDesc);

            // create a box with contact reporting enabled
            // the corresponding flags are encoded in word2 of simulation filter data
            int reportContactFlags = PxPairFlagEnum.eNOTIFY_TOUCH_FOUND | PxPairFlagEnum.eNOTIFY_TOUCH_LOST;
            PxFilterData boxFilterData = PxFilterData.createAt(mem, MemoryStack::nmalloc, 1, -1, reportContactFlags, 0);
            PxRigidDynamic box = PhysXTestEnv.createDefaultBox(0f, 5f, 0f, boxFilterData);
            scene.addActor(box);

            // create a trigger body
            // triggers don't collide with other bodies but trigger events are reported as soon as they overlap
            // with another body
            PxFilterData triggerFilterData = PxFilterData.createAt(mem, MemoryStack::nmalloc, 1, -1, 0, 0);
            PxBoxGeometry triggerGeom = PxBoxGeometry.createAt(mem, MemoryStack::nmalloc, 1f, 0.5f, 1f);
            PxShapeFlags triggerFlags = PxShapeFlags.createAt(mem, MemoryStack::nmalloc, (byte) PxShapeFlagEnum.eTRIGGER_SHAPE);
            PxShape triggerShape = physics.createShape(triggerGeom, PhysXTestEnv.defaultMaterial, true, triggerFlags);
            triggerShape.setSimulationFilterData(triggerFilterData);
            PxRigidStatic trigger = PhysXTestEnv.createStaticBody(triggerShape, 0f, 3f, 0f);
            scene.addActor(trigger);

            // create default ground box, nothing special here
            PxBoxGeometry groundGeom = PxBoxGeometry.createAt(mem, MemoryStack::nmalloc, 10f, 0.5f, 10f);
            PxRigidStatic ground = PhysXTestEnv.createStaticBody(groundGeom, 0f, 0f, 0f);
            scene.addActor(ground);

            // associate body pointers with human-readable names
            simCallback.actorNames.put(box, "Box");
            simCallback.actorNames.put(trigger, "Trigger");
            simCallback.actorNames.put(ground, "Ground");

            PhysXTestEnv.simulateScene(scene, 5f, box);

            Assert.assertEquals(2, simCallback.contactBodies.size());
            Assert.assertTrue(simCallback.contactBodies.contains(box));
            Assert.assertTrue(simCallback.contactBodies.contains(ground));

            Assert.assertEquals(2, simCallback.triggerBodies.size());
            Assert.assertTrue(simCallback.triggerBodies.contains(box));
            Assert.assertTrue(simCallback.triggerBodies.contains(trigger));

            // clean up
            scene.release();
            ground.release();
            trigger.release();
            box.release();
            simCallback.destroy();
        }
    }

    private static class TestSimulationCallback extends JavaSimulationEventCallback {
        Map<PxActor, String> actorNames = new HashMap<>();

        Set<PxActor> contactBodies = new HashSet<>();
        Set<PxActor> triggerBodies = new HashSet<>();

        @Override
        public void onContact(PxContactPairHeader pairHeader, PxContactPair pairs, int nbPairs) {
            PxActor actor0 = pairHeader.getActors(0);
            PxActor actor1 = pairHeader.getActors(1);
            contactBodies.add(actor0);
            contactBodies.add(actor1);
            Assert.assertTrue(actorNames.containsKey(actor0));
            Assert.assertTrue(actorNames.containsKey(actor1));
            String name0 = actorNames.get(actor0);
            String name1 = actorNames.get(actor1);

            for (int i = 0; i < nbPairs; i++) {
                PxContactPair pair = TypeHelpers.getContactPairAt(pairs, i);
                PxPairFlags events = pair.getEvents();
                String event = "OTHER";
                if (events.isSet(PxPairFlagEnum.eNOTIFY_TOUCH_FOUND)) {
                    event = "TOUCH_FOUND";
                } else if (events.isSet(PxPairFlagEnum.eNOTIFY_TOUCH_LOST)) {
                    event = "TOUCH_LOST";
                }
                System.out.println("onContact: " + name0 + " and " + name1 + ": " + event);
            }
        }

        @Override
        public void onTrigger(PxTriggerPair pairs, int count) {
            for (int i = 0; i < count; i++) {
                PxTriggerPair pair = TypeHelpers.getTriggerPairAt(pairs, i);
                PxActor actor0 = pair.getTriggerActor();
                PxActor actor1 = pair.getOtherActor();
                triggerBodies.add(actor0);
                triggerBodies.add(actor1);
                Assert.assertTrue(actorNames.containsKey(actor0));
                Assert.assertTrue(actorNames.containsKey(actor1));
                String name0 = actorNames.get(actor0);
                String name1 = actorNames.get(actor1);

                int status = pair.getStatus();
                String event = "OTHER";
                if (status == PxPairFlagEnum.eNOTIFY_TOUCH_FOUND) {
                    event = "TRIGGER_ENTER";
                } else if (status == PxPairFlagEnum.eNOTIFY_TOUCH_LOST) {
                    event = "TRIGGER_EXIT";
                }
                System.out.println("onTrigger: " + name0 + " and " + name1 + ": " + event);
            }
        }
    }
}
