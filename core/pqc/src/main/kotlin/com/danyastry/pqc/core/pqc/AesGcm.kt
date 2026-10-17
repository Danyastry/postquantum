package com.danyastry.pqc.core.pqc

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesGcm {

    const val KEY_BYTES = 32
    const val IV_BYTES = 12
    const val TAG_BYTES = 16
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun seal(key: ByteArray, plaintext: ByteArray, random: SecureRandom = SecureRandom()): ByteArray {
        require(key.size == KEY_BYTES) { "AES-256 needs a $KEY_BYTES byte key, got ${key.size}" }
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BYTES * Byte.SIZE_BITS, iv)) }
        return iv + cipher.doFinal(plaintext)
    }

    fun open(key: ByteArray, sealed: ByteArray): ByteArray {
        require(key.size == KEY_BYTES) { "AES-256 needs a $KEY_BYTES byte key, got ${key.size}" }
        require(sealed.size >= IV_BYTES + TAG_BYTES) { "Sealed data must hold the IV and the tag" }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BYTES * Byte.SIZE_BITS, sealed, 0, IV_BYTES))
        }
        return cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES)
    }
}
