package whitehat.android

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import whitehat.client.NativeTokenStore
import whitehat.client.NativeTokens

internal class AndroidTokenStore(context: Context) : NativeTokenStore {
    private val preferences = context.getSharedPreferences("whitehat.session", Context.MODE_PRIVATE)
    private val alias = "whitehat-session-v1"

    private fun keys() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun createKey(): SecretKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
        init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        generateKey()
    }

    override suspend fun read(): NativeTokens? = withContext(Dispatchers.IO) {
        val stored = preferences.getString("encrypted", null) ?: return@withContext null
        val key = keys().getKey(alias, null) as? SecretKey
        if (key == null) {
            check(preferences.edit().remove("encrypted").commit())
            return@withContext null
        }
        val frame = Base64.decode(stored, Base64.NO_WRAP)
        require(frame.size > 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, frame.copyOfRange(0, 12)))
        val body = JSONObject(cipher.doFinal(frame, 12, frame.size - 12).decodeToString())
        NativeTokens(body.getString("subject"), body.getString("access"),
            body.optString("refresh").takeIf(String::isNotEmpty))
    }

    override suspend fun write(tokens: NativeTokens?) = withContext(Dispatchers.IO) {
        if (tokens == null) {
            check(preferences.edit().remove("encrypted").commit())
            return@withContext
        }
        val key = keys().getKey(alias, null) as? SecretKey ?: createKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        require(cipher.iv.size == 12)
        val body = JSONObject().put("subject", tokens.subject).put("access", tokens.accessToken)
            .put("refresh", tokens.refreshToken.orEmpty()).toString()
        val frame = cipher.iv + cipher.doFinal(body.encodeToByteArray())
        check(preferences.edit().putString("encrypted", Base64.encodeToString(frame, Base64.NO_WRAP)).commit())
    }
}
