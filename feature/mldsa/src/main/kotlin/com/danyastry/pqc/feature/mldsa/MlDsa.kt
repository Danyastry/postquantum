package com.danyastry.pqc.feature.mldsa

import com.danyastry.pqc.core.pqc.BouncyCastle
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Provider
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.SignatureException
import org.bouncycastle.jcajce.interfaces.MLDSAPrivateKey
import org.bouncycastle.jcajce.interfaces.MLDSAPublicKey
import org.bouncycastle.jcajce.spec.ContextParameterSpec
import org.bouncycastle.jcajce.spec.MLDSAParameterSpec

enum class MlDsaLevel(
    val spec: MLDSAParameterSpec,
    val nistCategory: Int,
    val publicKeyBytes: Int,
    val privateKeyBytes: Int,
    val signatureBytes: Int,
) {
    ML_DSA_44(MLDSAParameterSpec.ml_dsa_44, 2, 1_312, 2_560, 2_420),
    ML_DSA_65(MLDSAParameterSpec.ml_dsa_65, 3, 1_952, 4_032, 3_309),
    ML_DSA_87(MLDSAParameterSpec.ml_dsa_87, 5, 2_592, 4_896, 4_627),
}

enum class SigningMode { HEDGED, DETERMINISTIC }

class MlDsa(
    private val provider: Provider = BouncyCastle.provider,
    private val random: SecureRandom = SecureRandom(),
) {

    fun generateKeyPair(level: MlDsaLevel): KeyPair =
        KeyPairGenerator.getInstance(ALGORITHM, provider).apply { initialize(level.spec, random) }.generateKeyPair()

    fun sign(privateKey: PrivateKey, message: ByteArray, context: ByteArray = ByteArray(0), mode: SigningMode = SigningMode.HEDGED): ByteArray {
        requireValidContext(context)
        return Signature.getInstance(ALGORITHM, provider).run {
            when (mode) {
                SigningMode.HEDGED -> initSign(privateKey, random)
                SigningMode.DETERMINISTIC -> initSign(privateKey)
            }
            setParameter(ContextParameterSpec(context))
            update(message)
            sign()
        }
    }

    fun verify(publicKey: PublicKey, message: ByteArray, signature: ByteArray, context: ByteArray = ByteArray(0)): Boolean {
        requireValidContext(context)
        return try {
            Signature.getInstance(ALGORITHM, provider).run {
                initVerify(publicKey)
                setParameter(ContextParameterSpec(context))
                update(message)
                verify(signature)
            }
        } catch (_: SignatureException) {
            false
        }
    }

    private fun requireValidContext(context: ByteArray) {
        require(context.size <= MAX_CONTEXT_BYTES) { "ML-DSA context is at most $MAX_CONTEXT_BYTES bytes, got ${context.size}" }
    }

    companion object {
        const val ALGORITHM = "ML-DSA"
        const val SEED_BYTES = 32
        const val MAX_CONTEXT_BYTES = 255

        fun publicKeyBytes(key: PublicKey): ByteArray = (key as MLDSAPublicKey).publicData

        fun privateKeyBytes(key: PrivateKey): ByteArray = (key as MLDSAPrivateKey).privateData

        fun seedBytes(key: PrivateKey): ByteArray? = (key as MLDSAPrivateKey).seed
    }
}
