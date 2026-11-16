package whitehat.desktop

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import whitehat.client.NativeTokens
import whitehat.client.NativeTokenStore

internal interface DesktopCredential {
    fun read(): ByteArray?
    fun write(value: ByteArray?)
}

internal class DesktopTokenStore(private val credential: DesktopCredential) : NativeTokenStore {
    override suspend fun read(): NativeTokens? = withContext(Dispatchers.IO) {
        val bytes = credential.read() ?: return@withContext null
        try {
            DataInputStream(ByteArrayInputStream(bytes)).use { input ->
                check(input.readUnsignedByte() == 1) { "Unsupported credential format" }
                val subject = input.readUTF()
                val accessToken = input.readUTF()
                val refreshToken = if (input.readBoolean()) input.readUTF() else null
                check(input.read() == -1) { "Invalid credential format" }
                NativeTokens(subject, accessToken, refreshToken)
            }
        } finally {
            bytes.fill(0)
        }
    }

    override suspend fun write(tokens: NativeTokens?) = withContext(Dispatchers.IO) {
        val bytes = tokens?.let {
            val buffer = ByteArrayOutputStream()
            DataOutputStream(buffer).use { output ->
                output.writeByte(1)
                output.writeUTF(it.subject)
                output.writeUTF(it.accessToken)
                output.writeBoolean(it.refreshToken != null)
                it.refreshToken?.let(output::writeUTF)
            }
            buffer.toByteArray()
        }
        try {
            credential.write(bytes)
        } finally {
            bytes?.fill(0)
        }
    }
}

internal fun desktopCredentialId(issuer: String, clientId: String, api: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(listOf(issuer, clientId, api).joinToString("\u0000").encodeToByteArray())
        .joinToString("") { "%02x".format(it) }

internal fun desktopTokenStore(issuer: String, clientId: String, api: String): NativeTokenStore {
    val id = desktopCredentialId(issuer, clientId, api)
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.startsWith("mac") -> DesktopTokenStore(MacCredential(id))
        os.startsWith("windows") -> DesktopTokenStore(windowsCredential(id))
        else -> ProcessTokenStore()
    }
}

private class ProcessTokenStore : NativeTokenStore {
    private var tokens: NativeTokens? = null
    override suspend fun read() = tokens
    override suspend fun write(tokens: NativeTokens?) {
        this.tokens = tokens
    }
}
