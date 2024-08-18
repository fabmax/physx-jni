import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

object Sha1Helper {
    fun writeHashes(forFilesInDirectory: File) {
        forFilesInDirectory.listFiles { _, name -> !name.endsWith(".sha1", true) }?.forEach { writeHash(it) }
    }

    fun writeHash(forFile: File) {
        val hashStr = FileInputStream(forFile).use { makeHash(it) }
        File(forFile.path + ".sha1").writer().use { it.write(hashStr) }
        println("$forFile -> SHA1: $hashStr")
    }

    private fun makeHash(inStream: InputStream): String {
        val md = MessageDigest.getInstance("SHA-1")
        val buf = ByteArray(4096)
        var len = inStream.read(buf)
        while (len > 0) {
            md.update(buf, 0, len)
            len = inStream.read(buf)
        }
        val hash = md.digest()
        val hashStr = StringBuilder()
        for (b in hash) {
            hashStr.append("%02x".format(b.toInt() and 0xff))
        }
        return hashStr.toString()
    }
}