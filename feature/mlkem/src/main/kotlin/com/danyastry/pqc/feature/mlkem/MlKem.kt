package com.danyastry.pqc.feature.mlkem

import com.danyastry.pqc.core.pqc.BouncyCastle
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Provider
import java.security.PublicKey
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation
import org.bouncycastle.jcajce.interfaces.MLKEMPrivateKey
import org.bouncycastle.jcajce.interfaces.MLKEMPublicKey
import org.bouncycastle.jcajce.spec.KEMExtractSpec
import org.bouncycastle.jcajce.spec.KEMGenerateSpec
import org.bouncycastle.jcajce.spec.MLKEMParameterSpec

enum class MlKemLevel(
    val spec: MLKEMParameterSpec,
    val nistCategory: Int,
    val comparableAesBits: Int,
    val encapsulationKeyBytes: Int,
    val decapsulationKeyBytes: Int,
    val ciphertextBytes: Int,
) {
    ML_KEM_512(MLKEMParameterSpec.ml_kem_512, 1, 128, 800, 1_632, 768),
    ML_KEM_768(MLKEMParameterSpec.ml_kem_768, 3, 192, 1_184, 2_400, 1_088),
    ML_KEM_1024(MLKEMParameterSpec.ml_kem_1024, 5, 256, 1_568, 3_168, 1_568),
}

class Encapsulation(val sharedSecret: ByteArray, val ciphertext: ByteArray)

class MlKem(
    private val provider: Provider = BouncyCastle.provider,
    private val random: SecureRandom = SecureRandom(),
) {

    fun generateKeyPair(level: MlKemLevel): KeyPair =
        KeyPairGenerator.getInstance(ALGORITHM, provider).apply { initialize(level.spec, random) }.generateKeyPair()

    fun encapsulate(encapsulationKey: PublicKey): Encapsulation {
        val generator = KeyGenerator.getInstance(ALGORITHM, provider).apply {
            init(KEMGenerateSpec(encapsulationKey, SECRET_ALGORITHM, SHARED_SECRET_BITS), random)
        }
        val secret = generator.generateKey() as SecretKeyWithEncapsulation
        return Encapsulation(sharedSecret = secret.encoded, ciphertext = secret.encapsulation)
    }

    fun decapsulate(decapsulationKey: PrivateKey, ciphertext: ByteArray): ByteArray =
        KeyGenerator.getInstance(ALGORITHM, provider).run {
            init(KEMExtractSpec(decapsulationKey, ciphertext, SECRET_ALGORITHM, SHARED_SECRET_BITS))
            generateKey().encoded
        }

    companion object {
        const val ALGORITHM = "ML-KEM"
        const val SHARED_SECRET_BYTES = 32
        const val SEED_BYTES = 64
        private const val SHARED_SECRET_BITS = SHARED_SECRET_BYTES * Byte.SIZE_BITS
        private const val SECRET_ALGORITHM = "AES"

        fun encapsulationKeyBytes(key: PublicKey): ByteArray = (key as MLKEMPublicKey).publicData

        fun decapsulationKeyBytes(key: PrivateKey): ByteArray = (key as MLKEMPrivateKey).privateData

        fun seedBytes(key: PrivateKey): ByteArray? = (key as MLKEMPrivateKey).seed
    }
}
