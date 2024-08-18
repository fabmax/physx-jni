import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.FileReader
import java.io.FileWriter

open class VersionNameUpdate : DefaultTask() {
    @Input
    var versionName = "0.0.0"

    @Input
    var filesToUpdate = listOf<String>()

    @TaskAction
    fun updateVersions() {
        filesToUpdate.forEach { file ->
            val text = mutableListOf<String>()
            var updated = false

            FileReader(file).use {
                text += it.readLines()
                for (i in text.indices) {
                    val startI = text[i].indexOf("private static final String version")
                    if (startI >= 0) {
                        text[i] = text[i].substring(0 until startI) + "private static final String version = \"$versionName\";"
                        updated = true
                        break
                    }
                }
            }

            if (updated) {
                FileWriter(file).use {
                    text.forEach { line ->
                        it.append(line).append("\n")
                    }
                }
            } else {
                System.err.println("Failed to update version name in file $file")
            }
        }
    }
}