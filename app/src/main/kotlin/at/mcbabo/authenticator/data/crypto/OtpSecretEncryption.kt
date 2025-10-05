package at.mcbabo.authenticator.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class OtpSecretEncryption {
    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEYSTORE_ALIAS = "otp_master_key"
        private const val KEY_SIZE = 256
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH = 128
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    /**
     * Get or create master key in Android Keystore
     */
    fun getOrCreateKey(): SecretKey {
        return keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
            ?: generateAndStoreKey()
    }

    private fun generateAndStoreKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val builder = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setUserAuthenticationRequired(false)

        builder.setInvalidatedByBiometricEnrollment(false)
        builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_DEVICE_CREDENTIAL)

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Encrypt a secret using AES-GCM with Android Keystore
     */
    suspend fun encrypt(plaintext: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(plaintext.isNotEmpty()) { "Plaintext cannot be empty" }

            val key = getOrCreateKey()

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv
            require(iv.size == IV_LENGTH) { "Invalid IV length: ${iv.size}" }

            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = iv + ciphertext

            Base64.encodeToString(combined, Base64.NO_WRAP)
        }
    }

    /**
     * Decrypt a secret using AES-GCM with Android Keystore
     */
    suspend fun decrypt(encryptedData: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(encryptedData.isNotEmpty()) { "Encrypted data cannot be empty" }

            val key = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
                ?: throw SecurityException("Encryption key not found")

            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
            require(combined.size > IV_LENGTH) { "Invalid encrypted data length" }

            val iv = combined.sliceArray(0 until IV_LENGTH)
            val ciphertext = combined.sliceArray(IV_LENGTH until combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val plaintextBytes = cipher.doFinal(ciphertext)
            String(plaintextBytes, Charsets.UTF_8)
        }
    }

    /**
     * Check if encryption key exists
     */
    fun hasKey(): Boolean {
        return keyStore.containsAlias(KEYSTORE_ALIAS)
    }

    /**
     * Delete the encryption key (use with caution - will make all encrypted data unrecoverable)
     */
    fun deleteKey() {
        if (hasKey()) {
            keyStore.deleteEntry(KEYSTORE_ALIAS)
        }
    }

    /**
     * Verify key is accessible (useful for checking after device unlock)
     */
    suspend fun verifyKeyAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            true
        } catch (e: Exception) {
            false
        }
    }
}
