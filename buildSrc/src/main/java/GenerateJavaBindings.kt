import de.fabmax.webidl.parser.WebIdlParser
import de.fabmax.webidl.generator.jni.JniJavaGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileNotFoundException

open class GenerateJavaBindings : DefaultTask() {
    @TaskAction
    fun generate() {
        val idlFile = File("PhysX/physx/source/physxwebbindings/src/PhysXJs.idl")
        if (!idlFile.exists()) {
            throw FileNotFoundException("PhysX WebIDL definition not found! Forgot to run 'git submodule update --init'?")
        }

        // make sure generator output directory exists
        val generatorTarget = File("physx-jni/src/main/generated")
        if (!generatorTarget.exists()) {
            generatorTarget.mkdirs()
        }

        val model = WebIdlParser().parse(idlFile.path)
        JniJavaGenerator().apply {
            outputDirectory = "physx-jni/src/main/generated/physx"
            packagePrefix = "physx"
            onClassLoad = "de.fabmax.physxjni.Loader.load();"

            nullableAttributes += "PxBatchQueryDesc.preFilterShader"
            nullableAttributes += "PxBatchQueryDesc.postFilterShader"
        }.generate(model)
        println("Generated JNI classes: " + (model.interfaces.size + model.enums.size) + " classes")
    }
}
