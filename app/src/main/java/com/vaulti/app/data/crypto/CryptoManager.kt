package com.vaulti.app.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    private var aesKey: SecretKey? = null

    private val prefs = context.getSharedPreferences("vaulti_crypto", Context.MODE_PRIVATE)

    val isInitialized: Boolean get() = aesKey != null

    private val uid: String?
        get() = firebaseAuth.currentUser?.uid

    private fun cryptoDocRef() = uid?.let {
        firestore.collection("users").document(it).collection("_crypto").document("key")
    }

    suspend fun hasKeyBackup(): Boolean {
        val ref = cryptoDocRef() ?: return false
        return try {
            ref.get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun generateAndBackupKey(pin: String) {
        val currentUid = uid ?: throw IllegalStateException("Not authenticated")

        val keyGen = javax.crypto.KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val key = keyGen.generateKey()

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val kek = deriveKeyFromPin(pin, salt)
        val encryptedKey = wrapKey(key, kek)

        val doc = mapOf(
            "encryptedKey" to Base64.encodeToString(encryptedKey, Base64.NO_WRAP),
            "salt" to Base64.encodeToString(salt, Base64.NO_WRAP),
            "iterations" to ITERATIONS
        )

        cryptoDocRef()?.set(doc)?.await()

        this.aesKey = key
        storeDeviceWrappedKey(key, currentUid)
    }

    suspend fun recoverKey(pin: String): Boolean {
        val currentUid = uid ?: return false

        val doc = try {
            cryptoDocRef()?.get()?.await() ?: return false
        } catch (_: Exception) {
            return false
        }

        val encryptedKeyB64 = doc.getString("encryptedKey") ?: return false
        val saltB64 = doc.getString("salt") ?: return false
        val iterations = doc.getLong("iterations")?.toInt() ?: ITERATIONS

        val encryptedKey = Base64.decode(encryptedKeyB64, Base64.NO_WRAP)
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)

        val kek = deriveKeyFromPin(pin, salt, iterations)
        val key = try {
            unwrapKey(encryptedKey, kek)
        } catch (_: Exception) {
            return false
        }

        this.aesKey = key
        storeDeviceWrappedKey(key, currentUid)
        return true
    }

    fun tryDeviceUnlock(): Boolean {
        val currentUid = uid ?: return false
        if (aesKey != null) return true

        val blob = prefs.getString("wrapped_key_$currentUid", null) ?: return false
        val encryptedKey = Base64.decode(blob, Base64.NO_WRAP)

        return try {
            val deviceKey = getOrCreateDeviceKeyPair(currentUid)
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, deviceKey.private)
            val keyBytes = cipher.doFinal(encryptedKey)
            this.aesKey = SecretKeySpec(keyBytes, "AES")
            true
        } catch (_: Exception) {
            false
        }
    }

    fun removeDeviceKey() {
        val currentUid = uid ?: return
        prefs.edit().remove("wrapped_key_$currentUid").apply()
    }

    fun encrypt(plaintext: String, context: String): String {
        val key = aesKey ?: throw IllegalStateException("Crypto not initialized")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(context.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(ciphertext: String, context: String): String {
        val key = aesKey ?: throw IllegalStateException("Crypto not initialized")
        val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ct = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        cipher.updateAAD(context.toByteArray(Charsets.UTF_8))
        val plaintext = cipher.doFinal(ct)
        return String(plaintext, Charsets.UTF_8)
    }

    fun clearKey() {
        aesKey = null
    }

    private fun getOrCreateDeviceKeyPair(uid: String): KeyPair {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val alias = "${KEY_ALIAS_PREFIX}$uid"

        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
            return KeyPair(entry.certificate.publicKey, entry.privateKey)
        }

        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(2048)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .build()
        generator.initialize(spec)
        return generator.generateKeyPair()
    }

    private fun storeDeviceWrappedKey(key: SecretKey, uid: String) {
        try {
            val deviceKey = getOrCreateDeviceKeyPair(uid)
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, deviceKey.public)
            val wrapped = cipher.doFinal(key.encoded)
            prefs.edit().putString("wrapped_key_$uid", Base64.encodeToString(wrapped, Base64.NO_WRAP)).apply()
        } catch (_: Exception) {}
    }

    private fun deriveKeyFromPin(pin: String, salt: ByteArray, iterations: Int = ITERATIONS): SecretKey {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private fun wrapKey(key: SecretKey, kek: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, kek)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(key.encoded)
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return combined
    }

    private fun unwrapKey(wrapped: ByteArray, kek: SecretKey): SecretKey {
        val iv = wrapped.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = wrapped.copyOfRange(GCM_IV_LENGTH, wrapped.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val keyBytes = cipher.doFinal(encrypted)
        return SecretKeySpec(keyBytes, "AES")
    }

    companion object {
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val ITERATIONS = 100_000
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS_PREFIX = "vaulti_device_key_"
    }
}
