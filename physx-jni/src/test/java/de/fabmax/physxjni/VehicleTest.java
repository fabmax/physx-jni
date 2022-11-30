package de.fabmax.physxjni;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import physx.common.PxIDENTITYEnum;
import physx.common.PxQuat;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.physics.*;
import physx.vehicle2.*;

import java.util.Arrays;
import java.util.Locale;

public class VehicleTest {

    @Test
    public void vehicleTest() {
        PxScene scene = PhysXTestEnv.createEmptyScene();
        var groundPlane = PhysXTestEnv.createGroundPlane();
        scene.addActor(groundPlane);

        EngineDriveVehicle vehicle = new EngineDriveVehicle();
        PxVehiclePhysXSimulationContext vehicleSimulationContext = new PxVehiclePhysXSimulationContext();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Create and configure engine drive vehicle properties
            setBaseParams(vehicle.getBaseParams());
            setPhysxIntegrationParams(vehicle.getPhysXParams(), vehicle.getBaseParams().getAxleDescription());
            setEngineDriveParams(vehicle.getEngineDriveParams(), vehicle.getBaseParams().getAxleDescription());

            // Initialize vehicle stuff
            Assertions.assertTrue(
                    vehicle.initialize(
                            PhysXTestEnv.physics,
                            PhysXTestEnv.cookingParams,
                            PhysXTestEnv.defaultMaterial,
                            EngineDriveVehicleEnum.eDIFFTYPE_FOURWHEELDRIVE
                    )
            );

            // Apply a start pose to the physx actor and add it to the physx scene.
            var vehiclePose = PxTransform.createAt(stack, MemoryStack::nmalloc,
                    PxVec3.createAt(stack, MemoryStack::nmalloc, 0f, 0.1f, 0f),
                    PxQuat.createAt(stack, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity)
            );
            vehicle.getPhysXState().getPhysxActor().getRigidBody().setGlobalPose(vehiclePose);
            scene.addActor(vehicle.getPhysXState().getPhysxActor().getRigidBody());

            var gravity = PxVec3.createAt(stack, MemoryStack::nmalloc, 0f, -9.81f, 0f);

            // Set the vehicle in 1st gear.
            vehicle.getEngineDriveState().getGearboxState().setCurrentGear(vehicle.getEngineDriveParams().getGearBoxParams().getNeutralGear() + 1);
            vehicle.getEngineDriveState().getGearboxState().setTargetGear(vehicle.getEngineDriveParams().getGearBoxParams().getNeutralGear() + 1);

            // Set the vehicle to use the automatic gearbox.
            vehicle.getTransmissionCommandState().setTargetGear(PxVehicleEngineDriveTransmissionCommandStateEnum.eAUTOMATIC_GEAR);

            // Set up the simulation context.
            // The test is set up with
            // a) z as the longitudinal axis
            // b) x as the lateral axis
            // c) y as the vertical axis.
            // d) metres as the lengthscale.
            vehicleSimulationContext.setToDefault();
            vehicleSimulationContext.getFrame().setLngAxis(PxVehicleAxesEnum.ePosZ);
            vehicleSimulationContext.getFrame().setLatAxis(PxVehicleAxesEnum.ePosX);
            vehicleSimulationContext.getFrame().setVrtAxis(PxVehicleAxesEnum.ePosY);
            vehicleSimulationContext.getScale().setScale(1f);
            vehicleSimulationContext.setGravity(gravity);
            vehicleSimulationContext.setPhysxScene(scene);
            vehicleSimulationContext.setPhysxActorUpdateMode(PxVehiclePhysXActorUpdateModeEnum.eAPPLY_ACCELERATION);
        }

        runSimulation(scene, vehicle, vehicleSimulationContext);
    }

    private void runSimulation(PxScene scene, EngineDriveVehicle vehicle, PxVehiclePhysXSimulationContext vehicleSimulationContext) {
        float duration = 10.1f;
        float step = 1/60f;
        float t = 0;

        var vehicleZ = 0f;
        var prevV = 0f;

        for (int i = 0; i < duration / step; i++) {
            var vehicleActor = vehicle.getPhysXState().getPhysxActor().getRigidBody();
            PxVec3 pos = vehicleActor.getGlobalPose().getP();
            vehicleZ = pos.getZ();

            // print position of printActor each simulated sec
            if (i % 60 == 0) {
                var v = vehicle.getPhysXState().getPhysxActor().getRigidBody().getLinearVelocity().getZ();
                var a = v - prevV;
                prevV = v;
                System.out.printf(Locale.ENGLISH, "t = %.2f s, pos(%6.3f, %6.3f, %6.3f), v = %.1f km/s, a = %.1f m/s^2\n", t, pos.getX(), pos.getY(), pos.getZ(), v * 3.6f, a);
            }

            // Pedal to the metal!
            vehicle.getCommandState().setThrottle(1f);
            vehicle.getCommandState().setNbBrakes(0);
            vehicle.getCommandState().setSteer(0f);

            // Simulate vehicle by a single time step
            vehicle.step(step, vehicleSimulationContext);

            // Simulate physx scene by a single time step
            scene.simulate(step);
            scene.fetchResults(true);
            t += step;
        }
        // vehicle should have driven some distance
        Assertions.assertTrue(vehicleZ > 100f);
    }

    private void setBaseParams(BaseVehicleParams baseParams) {

        //
        // most values taken from Physx/physx/snippets/media/vehicledata/Base.json
        //

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var axleDesc = baseParams.getAxleDescription();
            axleDesc.setNbAxles(2);
            axleDesc.setNbWheels(4);
            axleDesc.setNbWheelsPerAxle(0, 2);
            axleDesc.setNbWheelsPerAxle(1, 2);
            axleDesc.setAxleToWheelIds(0, 0);
            axleDesc.setAxleToWheelIds(1, 2);
            for (int i = 0; i < 4; i++) {
                axleDesc.setWheelIdsInAxleOrder(i, i);
            }
            Assertions.assertTrue(axleDesc.isValid());

            var frame = baseParams.getFrame();
            frame.setLatAxis(PxVehicleAxesEnum.ePosX);
            frame.setLngAxis(PxVehicleAxesEnum.ePosZ);
            frame.setVrtAxis(PxVehicleAxesEnum.ePosY);
            Assertions.assertTrue(frame.isValid());

            baseParams.getScale().setScale(1f);

            var rigidBody = baseParams.getRigidBodyParams();
            rigidBody.setMass(2000f);
            rigidBody.setMoi(PxVec3.createAt(stack, MemoryStack::nmalloc, 3200f, 3400f, 750f));
            Assertions.assertTrue(rigidBody.isValid());

            var brakeResponse = baseParams.getBrakeResponseParams(0);
            brakeResponse.setMaxResponse(2000f);
            for (int i = 0; i < 4; i++) {
                brakeResponse.setWheelResponseMultipliers(i, 1f);
            }
            Assertions.assertTrue(brakeResponse.isValid(axleDesc));

            var handBrakeResponse = baseParams.getBrakeResponseParams(0);
            handBrakeResponse.setMaxResponse(2000f);
            for (int i = 0; i < 4; i++) {
                handBrakeResponse.setWheelResponseMultipliers(i, i < 2 ? 1f : 0f);
            }
            Assertions.assertTrue(handBrakeResponse.isValid(axleDesc));

            var steerResponse = baseParams.getSteerResponseParams();
            steerResponse.setMaxResponse(0.5f);
            for (int i = 0; i < 4; i++) {
                steerResponse.setWheelResponseMultipliers(i, i < 2 ? 1f : 0f);
            }
            Assertions.assertTrue(steerResponse.isValid(axleDesc));

            var ackermann = baseParams.getAckermannParams(0);
            ackermann.setWheelIds(0, 0);
            ackermann.setWheelIds(1, 1);
            ackermann.setWheelBase(2.87f);
            ackermann.setTrackWidth(1.6f);
            ackermann.setStrength(1f);
            Assertions.assertTrue(ackermann.isValid(axleDesc));

            var suspensionAttachmentPoses = Arrays.asList(
                    PxTransform.createAt(stack, MemoryStack::nmalloc,
                            PxVec3.createAt(stack, MemoryStack::nmalloc, -0.8f, -0.1f, 1.27f),
                            PxQuat.createAt(stack, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity)
                    ),
                    PxTransform.createAt(stack, MemoryStack::nmalloc,
                            PxVec3.createAt(stack, MemoryStack::nmalloc, 0.8f, -0.1f, 1.27f),
                            PxQuat.createAt(stack, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity)
                    ),
                    PxTransform.createAt(stack, MemoryStack::nmalloc,
                            PxVec3.createAt(stack, MemoryStack::nmalloc, -0.8f, -0.1f, -1.6f),
                            PxQuat.createAt(stack, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity)
                    ),
                    PxTransform.createAt(stack, MemoryStack::nmalloc,
                            PxVec3.createAt(stack, MemoryStack::nmalloc, 0.8f, -0.1f, -1.6f),
                            PxQuat.createAt(stack, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity)
                    )
            );
            var wheelAttachmentPose = PxTransform.createAt(stack, MemoryStack::nmalloc,
                    PxVec3.createAt(stack, MemoryStack::nmalloc, 0f, 0f, 0f),
                    PxQuat.createAt(stack, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity)
            );
            var suspensionTravelDir = PxVec3.createAt(stack, MemoryStack::nmalloc, 0f, -1f, 0f);
            for (int i = 0; i < 4; i++) {
                var suspension = baseParams.getSuspensionParams(i);
                suspension.setSuspensionAttachment(suspensionAttachmentPoses.get(i));
                suspension.setSuspensionTravelDir(suspensionTravelDir);
                suspension.setWheelAttachment(wheelAttachmentPose);
                suspension.setSuspensionTravelDist(0.25f);
                Assertions.assertTrue(suspension.isValid());
            }

            var suspensionCalc = baseParams.getSuspensionStateCalculationParams();
            suspensionCalc.setSuspensionJounceCalculationType(PxVehicleSuspensionJounceCalculationTypeEnum.eSWEEP);
            suspensionCalc.setLimitSuspensionExpansionVelocity(false);
            Assertions.assertTrue(suspensionCalc.isValid());

            var forceAppPoint = PxVec3.createAt(stack, MemoryStack::nmalloc, 0f, 0f, -0.11f);
            for (int i = 0; i < 4; i++) {
                var suspensionComp = baseParams.getSuspensionComplianceParams(i);
                suspensionComp.getWheelToeAngle().addPair(0f, 0f);
                suspensionComp.getWheelCamberAngle().addPair(0f, 0f);
                suspensionComp.getSuspForceAppPoint().addPair(0f, forceAppPoint);
                suspensionComp.getTireForceAppPoint().addPair(0f, forceAppPoint);
                Assertions.assertTrue(suspensionComp.isValid());
            }

            for (int i = 0; i < 4; i++) {
                var suspensionForce = baseParams.getSuspensionForceParams(i);
                suspensionForce.setDamping(8_000f);
                suspensionForce.setStiffness(32_000f);
                suspensionForce.setSprungMass(500f);
                Assertions.assertTrue(suspensionForce.isValid());
            }

            for (int i = 0; i < 4; i++) {
                var tireForce = baseParams.getTireForceParams(i);
                tireForce.setLongStiff(25_000f);
                tireForce.setLatStiffX(0.01f);
                tireForce.setLatStiffY(120_000f);
                tireForce.setCamberStiff(0f);
                tireForce.setRestLoad(500f);
                PxVehicleTireForceParamsExt.setFrictionVsSlip(tireForce, 0, 0, 0f);
                PxVehicleTireForceParamsExt.setFrictionVsSlip(tireForce, 0, 1, 1f);
                PxVehicleTireForceParamsExt.setFrictionVsSlip(tireForce, 1, 0, 0.1f);
                PxVehicleTireForceParamsExt.setFrictionVsSlip(tireForce, 1, 1, 1f);
                PxVehicleTireForceParamsExt.setFrictionVsSlip(tireForce, 2, 0, 1f);
                PxVehicleTireForceParamsExt.setFrictionVsSlip(tireForce, 2, 1, 1f);

                PxVehicleTireForceParamsExt.setLoadFilter(tireForce, 0, 0, 0f);
                PxVehicleTireForceParamsExt.setLoadFilter(tireForce, 0, 1, 0.23f);
                PxVehicleTireForceParamsExt.setLoadFilter(tireForce, 1, 0, 3f);
                PxVehicleTireForceParamsExt.setLoadFilter(tireForce, 1, 1, 3f);
                Assertions.assertTrue(tireForce.isValid());
            }

            for (int i = 0; i < 4; i++) {
                var wheel = baseParams.getWheelParams(i);
                wheel.setMass(25f);
                wheel.setRadius(0.35f);
                wheel.setHalfWidth(0.15f);
                wheel.setDampingRate(0.25f);
                wheel.setMoi(1.17f);
                Assertions.assertTrue(wheel.isValid());
            }

            Assertions.assertTrue(baseParams.isValid());
        }
    }

    private void setPhysxIntegrationParams(PhysXIntegrationParams physxParams, PxVehicleAxleDescription axleDesc) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var filterData = PxFilterData.createAt(stack, MemoryStack::nmalloc, 0, 0, 0, 0);
            var queryFlags = PxQueryFlags.createAt(stack, MemoryStack::nmalloc, (short) PxQueryFlagEnum.eSTATIC);
            var queryFilterData = PxQueryFilterData.createAt(stack, MemoryStack::nmalloc, filterData, queryFlags);

            var actorCMassLocalPose = PxTransform.createAt(stack, MemoryStack::nmalloc,
                    PxVec3.createAt(stack, MemoryStack::nmalloc, 0f, 0.55f, 1.594f),
                    PxQuat.createAt(stack, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity)
            );
            var actorBoxShapeHalfExtents = PxVec3.createAt(stack, MemoryStack::nmalloc, 0.85f, 0.65f, 2.5f);
            var actorShapeLocalPose = PxTransform.createAt(stack, MemoryStack::nmalloc,
                    PxVec3.createAt(stack, MemoryStack::nmalloc, 0f, 0.83f, 1.37f),
                    PxQuat.createAt(stack, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity)
            );

            PxVehiclePhysXMaterialFriction materialFriction = new PxVehiclePhysXMaterialFriction();
            materialFriction.setFriction(1f);
            materialFriction.setMaterial(PhysXTestEnv.defaultMaterial);

            physxParams.create(axleDesc, queryFilterData, null, materialFriction, 1, 1f, actorCMassLocalPose, actorBoxShapeHalfExtents, actorShapeLocalPose);
            Assertions.assertTrue(physxParams.isValid(axleDesc));
        }
    }

    private void setEngineDriveParams(EngineDrivetrainParams engineDriveParams, PxVehicleAxleDescription axleDesc) {

        //
        // most values taken from Physx/physx/snippets/media/vehicledata/EngineDrive.json
        //

        var autobox = engineDriveParams.getAutoboxParams();
        autobox.setUpRatios(0, 0.65f);
        autobox.setUpRatios(1, 0.15f);
        autobox.setUpRatios(2, 0.65f);
        autobox.setUpRatios(3, 0.65f);
        autobox.setUpRatios(4, 0.65f);
        autobox.setUpRatios(5, 0.65f);
        autobox.setUpRatios(6, 0.65f);
        autobox.setDownRatios(0, 0.5f);
        autobox.setDownRatios(1, 0.5f);
        autobox.setDownRatios(2, 0.5f);
        autobox.setDownRatios(3, 0.5f);
        autobox.setDownRatios(4, 0.5f);
        autobox.setDownRatios(5, 0.5f);
        autobox.setDownRatios(6, 0.5f);
        autobox.setLatency(2f);

        engineDriveParams.getClutchCommandResponseParams().setMaxResponse(10f);

        var engine = engineDriveParams.getEngineParams();
        engine.getTorqueCurve().addPair(0f, 1f);
        engine.getTorqueCurve().addPair(0.33f, 1f);
        engine.getTorqueCurve().addPair(1f, 1f);
        engine.setMoi(1f);
        engine.setPeakTorque(500f);
        engine.setIdleOmega(0f);
        engine.setMaxOmega(600f);
        engine.setDampingRateFullThrottle(0.15f);
        engine.setDampingRateZeroThrottleClutchEngaged(2f);
        engine.setDampingRateZeroThrottleClutchDisengaged(0.35f);

        var gearbox = engineDriveParams.getGearBoxParams();
        gearbox.setNeutralGear(1);
        gearbox.setRatios(0, -4f);
        gearbox.setRatios(1, 0f);
        gearbox.setRatios(2, 4f);
        gearbox.setRatios(3, 2f);
        gearbox.setRatios(4, 1.5f);
        gearbox.setRatios(5, 1.1f);
        gearbox.setRatios(6, 1f);
        gearbox.setNbRatios(7);
        gearbox.setFinalRatio(4f);
        gearbox.setSwitchTime(0.5f);

        var fourWheelDiff = engineDriveParams.getFourWheelDifferentialParams();
        for (int i = 0; i < 4; i++) {
            fourWheelDiff.setTorqueRatios(i, 0.25f);
            fourWheelDiff.setAveWheelSpeedRatios(i, 0.25f);
        }
        fourWheelDiff.setFrontWheelIds(0, 0);
        fourWheelDiff.setFrontWheelIds(1, 1);
        fourWheelDiff.setRearWheelIds(0, 2);
        fourWheelDiff.setRearWheelIds(1, 3);
        fourWheelDiff.setCenterBias(1.3f);
        fourWheelDiff.setCenterTarget(1.29f);
        fourWheelDiff.setFrontBias(1.3f);
        fourWheelDiff.setFrontTarget(1.29f);
        fourWheelDiff.setRearBias(1.3f);
        fourWheelDiff.setRearTarget(1.29f);
        fourWheelDiff.setRate(10f);

        var clutch = engineDriveParams.getClutchParams();
        clutch.setAccuracyMode(PxVehicleClutchAccuracyModeEnum.eESTIMATE);
        clutch.setEstimateIterations(5);

        Assertions.assertTrue(engineDriveParams.isValid(axleDesc));
    }
}
