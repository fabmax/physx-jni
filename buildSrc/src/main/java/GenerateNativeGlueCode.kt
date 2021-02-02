import de.fabmax.webidl.parser.WebIdlParser
import de.fabmax.webidl.generator.jni.JniNativeGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileNotFoundException

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
        }.generate(model)
    }
}