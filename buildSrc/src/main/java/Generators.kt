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
        "PxTransform",
        "PxVec3",

        "PxBoxGeometry",
        "PxCapsuleGeometry",
        "PxConvexMeshGeometry",
        "PxPlaneGeometry",
        "PxSphereGeometry",
        "PxTriangleMeshGeometry",

        "PxSceneDesc",
        "PxSceneLimits",
        "PxBatchQueryDesc",
        "PxConvexMeshDesc",
        "PxCudaContextManagerDesc",
        "PxHeightFieldDesc",
        "PxTriangleMeshDesc",
        "PxVehicleAntiRollBarData",
        "PxVehicleDriveSimData4W",
        "PxVehicleSuspensionData",
        "PxVehicleTireData",
        "PxVehicleWheelData",

        "PxActorFlags",
        "PxBaseFlags",
        "PxConvexFlags",
        "PxConvexMeshGeometryFlags",
        "PxHitFlags",
        "PxMeshFlags",
        "PxRevoluteJointFlags",
        "PxRigidBodyFlags",
        "PxRigidDynamicLockFlags",
        "PxSceneFlags",
        "PxShapeFlags",
        "PxTriangleMeshFlags",
        "PxVehicleWheelsSimFlags"
    )
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
            nullableParameters += "PxArticulationBase.createLink" to "parent"
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