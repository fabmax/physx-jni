import de.fabmax.webidl.generator.jni.JniJavaGenerator
import de.fabmax.webidl.generator.jni.JniNativeGenerator
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
        "PxQuat",
        "PxTransform",
        "PxVec3",

        "PxBoxGeometry",
        "PxCapsuleGeometry",
        "PxConvexMeshDesc",
        "PxConvexMeshGeometry",
        "PxPlaneGeometry",
        "PxSphereGeometry",
        "PxTriangleMeshDesc",
        "PxTriangleMeshGeometry",

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
    var generatorOutput = "./generated"

    @TaskAction
    fun generate() {
        val idlFile = File("PhysX/physx/source/physxwebbindings/src/PhysXJs.idl")
        if (!idlFile.exists()) {
            throw FileNotFoundException("PhysX WebIDL definition not found! Forgot to run 'git submodule update --init'?")
        }

        val model = WebIdlParser().parse(idlFile.path)
        JniJavaGenerator().apply {
            outputDirectory = generatorOutput
            packagePrefix = "physx"
            onClassLoad = "de.fabmax.physxjni.Loader.load();"

            externallyAllocatableClasses += CommonGeneratorSettings.externallyAllocatableClasses
            nullableAttributes += "PxBatchQueryDesc.preFilterShader"
            nullableAttributes += "PxBatchQueryDesc.postFilterShader"
        }.generate(model)
        println("Generated JNI classes: " + (model.interfaces.size + model.enums.size) + " classes")
    }
}

open class GenerateNativeGlueCode : DefaultTask() {
    init {
        group = "native build"
    }

    @TaskAction
    fun generate() {
        val idlFile = File("PhysX/physx/source/physxwebbindings/src/PhysXJs.idl")
        if (!idlFile.exists()) {
            throw FileNotFoundException("PhysX WebIDL definition not found! Forgot to run 'git submodule update --init'?")
        }

        val model = WebIdlParser().parse(idlFile.path)
        JniNativeGenerator().apply {
            outputDirectory = "PhysX/physx/source/physxjnibindings/src/"
            packagePrefix = "physx"

            externallyAllocatableClasses += CommonGeneratorSettings.externallyAllocatableClasses
        }.generate(model)
    }
}