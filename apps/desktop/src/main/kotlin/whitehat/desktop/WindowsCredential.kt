package whitehat.desktop

import com.sun.jna.platform.win32.Crypt32Util
import com.sun.jna.platform.win32.WinCrypt
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

internal fun windowsCredential(id: String): DesktopCredential {
    val root = requireNotNull(System.getenv("LOCALAPPDATA")) { "LOCALAPPDATA is unavailable" }
    val path = Path.of(root, "WhiteHat", "credentials", "$id.dpapi")
    val entropy = "whitehat.desktop.session.v1:$id".encodeToByteArray()
    return ProtectedFileCredential(
        path,
        protect = { Crypt32Util.cryptProtectData(it, entropy, WinCrypt.CRYPTPROTECT_UI_FORBIDDEN, null, null) },
        unprotect = { Crypt32Util.cryptUnprotectData(it, entropy, WinCrypt.CRYPTPROTECT_UI_FORBIDDEN, null) },
    )
}

internal class ProtectedFileCredential(
    private val path: Path,
    private val protect: (ByteArray) -> ByteArray,
    private val unprotect: (ByteArray) -> ByteArray,
) : DesktopCredential {
    override fun read(): ByteArray? {
        val encrypted = try {
            Files.newInputStream(path).use { it.readNBytes(MAX_CREDENTIAL_SIZE + 1) }
        } catch (_: NoSuchFileException) {
            return null
        }
        check(encrypted.size <= MAX_CREDENTIAL_SIZE) { "Invalid credential size" }
        return unprotect(encrypted)
    }

    override fun write(value: ByteArray?) {
        if (value == null) {
            Files.deleteIfExists(path)
            return
        }
        val encrypted = protect(value)
        check(encrypted.size <= MAX_CREDENTIAL_SIZE) { "Credential exceeds size limit" }
        Files.createDirectories(path.parent)
        val temporary = Files.createTempFile(path.parent, "session-", ".tmp")
        try {
            Files.write(temporary, encrypted)
            Files.move(temporary, path, ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private companion object {
        const val MAX_CREDENTIAL_SIZE = 256 * 1024
    }
}
