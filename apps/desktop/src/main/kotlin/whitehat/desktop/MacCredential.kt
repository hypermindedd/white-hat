package whitehat.desktop

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference

internal interface MacSecurity : Library {
    fun SecKeychainFindGenericPassword(
        keychain: Pointer?, serviceLength: Int, service: ByteArray, accountLength: Int, account: ByteArray,
        length: IntByReference?, data: PointerByReference?, item: PointerByReference,
    ): Int

    fun SecKeychainAddGenericPassword(
        keychain: Pointer?, serviceLength: Int, service: ByteArray, accountLength: Int, account: ByteArray,
        length: Int, data: ByteArray, item: PointerByReference?,
    ): Int

    fun SecKeychainItemModifyAttributesAndData(item: Pointer, attributes: Pointer?, length: Int, data: ByteArray): Int
    fun SecKeychainItemDelete(item: Pointer): Int
    fun SecKeychainItemFreeContent(attributes: Pointer?, data: Pointer): Int
}

internal interface MacCoreFoundation : Library {
    fun CFRelease(item: Pointer)
}

internal class MacCredential(id: String) : DesktopCredential {
    private val security by lazy {
        Native.load("/System/Library/Frameworks/Security.framework/Security", MacSecurity::class.java)
    }
    private val core by lazy {
        Native.load("/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation", MacCoreFoundation::class.java)
    }
    private val service = "whitehat.desktop.session.v1".encodeToByteArray()
    private val account = id.encodeToByteArray()

    private fun find(item: PointerByReference, length: IntByReference? = null, data: PointerByReference? = null): Int =
        security.SecKeychainFindGenericPassword(null, service.size, service, account.size, account, length, data, item)

    override fun read(): ByteArray? {
        val item = PointerByReference()
        val length = IntByReference()
        val data = PointerByReference()
        try {
            val status = find(item, length, data)
            if (status == NOT_FOUND) return null
            requireSuccess(status)
            return requireNotNull(data.value).getByteArray(0, length.value)
        } finally {
            data.value?.let { security.SecKeychainItemFreeContent(null, it) }
            item.value?.let(core::CFRelease)
        }
    }

    override fun write(value: ByteArray?) {
        if (value != null) {
            val status = security.SecKeychainAddGenericPassword(
                null, service.size, service, account.size, account, value.size, value, null,
            )
            if (status != DUPLICATE) {
                requireSuccess(status)
                return
            }
        }
        val item = PointerByReference()
        try {
            val status = find(item)
            if (status == NOT_FOUND && value == null) return
            requireSuccess(status)
            val reference = requireNotNull(item.value)
            if (value == null) {
                val deleted = security.SecKeychainItemDelete(reference)
                if (deleted != NOT_FOUND) requireSuccess(deleted)
            } else {
                requireSuccess(security.SecKeychainItemModifyAttributesAndData(reference, null, value.size, value))
            }
        } finally {
            item.value?.let(core::CFRelease)
        }
    }

    private fun requireSuccess(status: Int) {
        check(status == 0) { "macOS Keychain operation failed (OSStatus $status)" }
    }

    private companion object {
        const val NOT_FOUND = -25300
        const val DUPLICATE = -25299
    }
}
