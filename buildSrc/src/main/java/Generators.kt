import de.fabmax.webidl.generator.jni.java.JniJavaGenerator
import de.fabmax.webidl.generator.jni.nat.JniNativeGenerator
import de.fabmax.webidl.parser.WebIdlParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileNotFoundException

//private object CommonGeneratorSettings {
//    val externallyAllocatableClasses = setOf(
//        "PxCudaContextManagerDesc",
//
//        "PxVehicleAntiRollBarData",
//        "PxVehicleDriveSimData4W",
//        "PxVehicleSuspensionData",
//        "PxVehicleTireData",
//        "PxVehicleWheelData",
//        "BatchVehicleUpdateDesc",
//        "PxVehicleWheelsSimFlags"
//    )
//}

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
    @Optional
    var idlModelName: String? = null
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

        val model = if (idlFile.isDirectory) {
            WebIdlParser.parseDirectory(idlFile.path, idlModelName)
        } else {
            WebIdlParser.parseSingleFile(idlFile.path, idlModelName)
        }

        JniJavaGenerator().apply {
            outputDirectory = generatorOutput
            packagePrefix = "physx"
            onClassLoad = "de.fabmax.physxjni.Loader.load();"
            parseCommentsFromDirectories += "../Physx/physx/include"
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
    @Optional
    var idlModelName: String? = null
    @Input
    var generatorOutput = "./generated"

    @TaskAction
    fun generate() {
        val idlFile = File(idlSource)
        if (!idlFile.exists()) {
            throw FileNotFoundException("PhysX WebIDL definition not found!")
        }

        val model = if (idlFile.isDirectory) {
            WebIdlParser.parseDirectory(idlFile.path, idlModelName)
        } else {
            WebIdlParser.parseSingleFile(idlFile.path, idlModelName)
        }

        JniNativeGenerator().apply {
            outputDirectory = generatorOutput
            glueFileName = "PhysXJniGlue.h"
            packagePrefix = "physx"
        }.generate(model)
    }
}