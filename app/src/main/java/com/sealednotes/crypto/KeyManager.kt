package com.sealednotes.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the SQLCipher database encryption key.
 *
 * The actual database passphrase is a random 32-byte key generated on first launch.
 * This key is encrypted using an AES key stored in Android Keystore (hardware-backed
 * where available), and the encrypted form is saved in SharedPreferences.
 *
 * For migration, the raw passphrase can be exported as a Base64 string or QR code,
 * and imported on a new device.
 */
class KeyManager(private val context: Context) {

    companion object {
        private const val KEYSTORE_ALIAS = "sealednotes_master_key"
        private const val PREFS_NAME = "sealednotes_crypto"
        private const val PREF_ENCRYPTED_PASSPHRASE = "encrypted_passphrase"
        private const val PREF_PASSPHRASE_IV = "passphrase_iv"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val GCM_TAG_LENGTH = 128
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns the database passphrase, creating one if this is first launch.
     * The passphrase is returned as a ByteArray suitable for SQLCipher's SupportFactory.
     */
    fun getOrCreateDatabaseKey(): ByteArray {
        val existingEncrypted = prefs.getString(PREF_ENCRYPTED_PASSPHRASE, null)
        val existingIv = prefs.getString(PREF_PASSPHRASE_IV, null)

        return if (existingEncrypted != null && existingIv != null) {
            decryptPassphrase(
                Base64.decode(existingEncrypted, Base64.NO_WRAP),
                Base64.decode(existingIv, Base64.NO_WRAP)
            )
        } else {
            val passphrase = generateRandomPassphrase()
            storePassphrase(passphrase)
            passphrase
        }
    }

    /**
     * Exports the raw database passphrase as a Base64 string.
     * The user can write this down, save it, or encode it as a QR code.
     * This is the ONLY way to access an exported database on a new device.
     */
    fun exportPassphraseAsString(): String {
        val passphrase = getOrCreateDatabaseKey()
        return Base64.encodeToString(passphrase, Base64.NO_WRAP)
    }

    /**
     * Imports a passphrase from a Base64 string (from another device).
     * Stores it encrypted in the local Keystore.
     * Returns true if successful.
     */
    fun importPassphraseFromString(base64Passphrase: String): Boolean {
        return try {
            val passphrase = Base64.decode(base64Passphrase, Base64.NO_WRAP)
            if (passphrase.size != 32) return false
            storePassphrase(passphrase)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks whether a passphrase has been set (i.e., the app has been initialized).
     */
    fun hasPassphrase(): Boolean {
        return prefs.getString(PREF_ENCRYPTED_PASSPHRASE, null) != null
    }

    // ── Private helpers ──

    private fun generateRandomPassphrase(): ByteArray {
        val passphrase = ByteArray(32)
        java.security.SecureRandom().nextBytes(passphrase)
        return passphrase
    }

    private fun storePassphrase(passphrase: ByteArray) {
        val keystoreKey = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey)

        val encrypted = cipher.doFinal(passphrase)
        val iv = cipher.iv

        prefs.edit()
            .putString(PREF_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(PREF_PASSPHRASE_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
    }

    private fun decryptPassphrase(encrypted: ByteArray, iv: ByteArray): ByteArray {
        val keystoreKey = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        keyStore.getEntry(KEYSTORE_ALIAS, null)?.let { entry ->
            return (entry as KeyStore.SecretKeyEntry).secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }
}
