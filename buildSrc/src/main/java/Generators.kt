import de.fabmax.webidl.generator.jni.java.JniJavaGenerator
import de.fabmax.webidl.generator.jni.nat.JniNativeGenerator
import de.fabmax.webidl.parser.WebIdlParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileNotFoundException

private object CommonGeneratorSettings {
    val externallyAllocatableClasses = setOf(
        "PxBoundedData",
        "PxBounds3",
        "PxFilterData",
        "PxHullPolygon",
        "PxMeshScale",
        "PxQuat",
        "PxRaycastHit",
        "PxSweepHit",
        "PxTransform",
        "PxVec3",
        "PxExtendedVec3",
        "PxHeightFieldSample",

        "PxBoxGeometry",
        "PxCapsuleGeometry",
        "PxConvexMeshGeometry",
        "PxHeightFieldGeometry",
        "PxPlaneGeometry",
        "PxSphereGeometry",
        "PxTriangleMeshGeometry",

        "PxSceneDesc",
        "PxSceneLimits",
        "PxBatchQueryDesc",
        "PxBoxControllerDesc",
        "PxCapsuleControllerDesc",
        "PxConvexMeshDesc",
        "PxCudaContextManagerDesc",
        "PxHeightFieldDesc",
        "PxTriangleMeshDesc",
        "PxVehicleAntiRollBarData",
        "PxVehicleDriveSimData4W",
        "PxVehicleSuspensionData",
        "PxVehicleTireData",
        "PxVehicleWheelData",
        "BatchVehicleUpdateDesc",

        "PxActorFlags",
        "PxBaseFlags",
        "PxConvexFlags",
        "PxConvexMeshGeometryFlags",
        "PxHitFlags",
        "PxMeshFlags",
        "PxMeshGeometryFlags",
        "PxQueryFlags",
        "PxRevoluteJointFlags",
        "PxRigidBodyFlags",
        "PxRigidDynamicLockFlags",
        "PxSceneFlags",
        "PxShapeFlags",
        "PxTriangleMeshFlags",
        "PxVehicleWheelsSimFlags"
    )
}

open class CheckWebIdlConsistency : DefaultTask() {
    @Input
    var idlSource1 = ""
    @Input
    var idlSource2 = ""

    @TaskAction
    fun checkConsistency() {
        val idlFile1 = File(idlSource1)
        val idlFile2 = File(idlSource2)

        if (idlFile2.exists()) {
            val lines1 = idlFile1.readLines()
            val lines2 = idlFile2.readLines()

            if (lines1.size != lines2.size) {
                throw IllegalStateException("WebIDL files differ in length: $idlSource1: ${lines1.size}, $idlSource2: ${lines2.size}")
            }

            for (i in lines1.indices) {
                if (lines1[i] != lines2[i]) {
                    throw IllegalStateException("WebIDL files inconsistent (line $i): \"${lines1[i]}\" != \"${lines2[i]}\"")
                }
            }
        }
    }
}

open class GenerateJavaBindings : DefaultTask() {
    @Input
    var idlSource = ""
    @Input
    var generatorOutput = "./generated"

    @TaskAction
    fun generate() {
        val idlFile = File(idlSource)
        if (!idlFile.exists()) {
            throw FileNotFoundException("PhysX WebIDL definition not found!")
        }

        val model = WebIdlParser().parse(idlFile.path)
        JniJavaGenerator().apply {
            outputDirectory = generatorOutput
            packagePrefix = "physx"
            onClassLoad = "de.fabmax.physxjni.Loader.load();"

            externallyAllocatableClasses += CommonGeneratorSettings.externallyAllocatableClasses

            nullableAttributes += "PxBatchQueryDesc.preFilterShader"
            nullableAttributes += "PxBatchQueryDesc.postFilterShader"
            nullableAttributes += "BatchVehicleUpdateDesc.preFilterShader"
            nullableAttributes += "BatchVehicleUpdateDesc.postFilterShader"

            nullableParameters += "PxArticulationBase.createLink" to "parent"
            nullableParameters += "PxTopLevelFunctions.D6JointCreate" to "actor0"
            nullableParameters += "PxTopLevelFunctions.D6JointCreate" to "actor1"
            nullableParameters += "PxTopLevelFunctions.DistanceJointCreate" to "actor0"
            nullableParameters += "PxTopLevelFunctions.DistanceJointCreate" to "actor1"
            nullableParameters += "PxTopLevelFunctions.FixedJointCreate" to "actor0"
            nullableParameters += "PxTopLevelFunctions.FixedJointCreate" to "actor1"
            nullableParameters += "PxTopLevelFunctions.PrismaticJointCreate" to "actor0"
            nullableParameters += "PxTopLevelFunctions.PrismaticJointCreate" to "actor1"
            nullableParameters += "PxTopLevelFunctions.RevoluteJointCreate" to "actor0"
            nullableParameters += "PxTopLevelFunctions.RevoluteJointCreate" to "actor1"
            nullableParameters += "PxTopLevelFunctions.SphericalJointCreate" to "actor0"
            nullableParameters += "PxTopLevelFunctions.SphericalJointCreate" to "actor1"
        }.generate(model)
    }
}

open class GenerateNativeGlueCode : DefaultTask() {
    init {
        group = "native build"
    }

    @Input
    var idlSource = ""
    @Input
    var generatorOutput = "./generated"

    @TaskAction
    fun generate() {
        val idlFile = File(idlSource)
        if (!idlFile.exists()) {
            throw FileNotFoundException("PhysX WebIDL definition not found!")
        }

        val model = WebIdlParser().parse(idlFile.path)
        JniNativeGenerator().apply {
            outputDirectory = generatorOutput
            packagePrefix = "physx"

            externallyAllocatableClasses += CommonGeneratorSettings.externallyAllocatableClasses
        }.generate(model)
    }
}