package com.danyastry.pqc.feature.slhdsa

import com.danyastry.pqc.core.pqc.BouncyCastle
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Provider
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.SignatureException
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.jcajce.interfaces.SLHDSAPublicKey
import org.bouncycastle.jcajce.spec.SLHDSAParameterSpec

enum class SlhDsaStrength(val bits: Int, val nistCategory: Int, val publicKeyBytes: Int, val privateKeyBytes: Int) {
    BITS_128(128, 1, 32, 64),
    BITS_192(192, 3, 48, 96),
    BITS_256(256, 5, 64, 128),
}

enum class SlhDsaVariant { SMALL, FAST }

enum class HashFamily { SHA2, SHAKE }

data class SlhDsaParameters(
    val strength: SlhDsaStrength = SlhDsaStrength.BITS_128,
    val variant: SlhDsaVariant = SlhDsaVariant.FAST,
    val hash: HashFamily = HashFamily.SHA2,
) {
    val name: String
        get() = "SLH-DSA-${hash.name}-${strength.bits}${if (variant == SlhDsaVariant.SMALL) "s" else "f"}"

    val signatureBytes: Int
        get() = SIGNATURE_BYTES.getValue(strength to variant)

    val spec: SLHDSAParameterSpec
        get() = SPECS.getValue(Triple(strength, variant, hash))

    private companion object {
        val SIGNATURE_BYTES = mapOf(
            (SlhDsaStrength.BITS_128 to SlhDsaVariant.SMALL) to 7_856,
            (SlhDsaStrength.BITS_128 to SlhDsaVariant.FAST) to 17_088,
            (SlhDsaStrength.BITS_192 to SlhDsaVariant.SMALL) to 16_224,
            (SlhDsaStrength.BITS_192 to SlhDsaVariant.FAST) to 35_664,
            (SlhDsaStrength.BITS_256 to SlhDsaVariant.SMALL) to 29_792,
            (SlhDsaStrength.BITS_256 to SlhDsaVariant.FAST) to 49_856,
        )

        val SPECS = mapOf(
            Triple(SlhDsaStrength.BITS_128, SlhDsaVariant.SMALL, HashFamily.SHA2) to SLHDSAParameterSpec.slh_dsa_sha2_128s,
            Triple(SlhDsaStrength.BITS_128, SlhDsaVariant.FAST, HashFamily.SHA2) to SLHDSAParameterSpec.slh_dsa_sha2_128f,
            Triple(SlhDsaStrength.BITS_192, SlhDsaVariant.SMALL, HashFamily.SHA2) to SLHDSAParameterSpec.slh_dsa_sha2_192s,
            Triple(SlhDsaStrength.BITS_192, SlhDsaVariant.FAST, HashFamily.SHA2) to SLHDSAParameterSpec.slh_dsa_sha2_192f,
            Triple(SlhDsaStrength.BITS_256, SlhDsaVariant.SMALL, HashFamily.SHA2) to SLHDSAParameterSpec.slh_dsa_sha2_256s,
            Triple(SlhDsaStrength.BITS_256, SlhDsaVariant.FAST, HashFamily.SHA2) to SLHDSAParameterSpec.slh_dsa_sha2_256f,
            Triple(SlhDsaStrength.BITS_128, SlhDsaVariant.SMALL, HashFamily.SHAKE) to SLHDSAParameterSpec.slh_dsa_shake_128s,
            Triple(SlhDsaStrength.BITS_128, SlhDsaVariant.FAST, HashFamily.SHAKE) to SLHDSAParameterSpec.slh_dsa_shake_128f,
            Triple(SlhDsaStrength.BITS_192, SlhDsaVariant.SMALL, HashFamily.SHAKE) to SLHDSAParameterSpec.slh_dsa_shake_192s,
            Triple(SlhDsaStrength.BITS_192, SlhDsaVariant.FAST, HashFamily.SHAKE) to SLHDSAParameterSpec.slh_dsa_shake_192f,
            Triple(SlhDsaStrength.BITS_256, SlhDsaVariant.SMALL, HashFamily.SHAKE) to SLHDSAParameterSpec.slh_dsa_shake_256s,
            Triple(SlhDsaStrength.BITS_256, SlhDsaVariant.FAST, HashFamily.SHAKE) to SLHDSAParameterSpec.slh_dsa_shake_256f,
        )
    }
}

class SlhDsa(
    private val provider: Provider = BouncyCastle.provider,
    private val random: SecureRandom = SecureRandom(),
) {

    fun generateKeyPair(parameters: SlhDsaParameters): KeyPair =
        KeyPairGenerator.getInstance(ALGORITHM, provider).apply { initialize(parameters.spec, random) }.generateKeyPair()

    fun sign(privateKey: PrivateKey, message: ByteArray): ByteArray =
        Signature.getInstance(ALGORITHM, provider).run {
            initSign(privateKey, random)
            update(message)
            sign()
        }

    fun verify(publicKey: PublicKey, message: ByteArray, signature: ByteArray): Boolean =
        try {
            Signature.getInstance(ALGORITHM, provider).run {
                initVerify(publicKey)
                update(message)
                verify(signature)
            }
        } catch (_: SignatureException) {
            false
        }

    companion object {
        const val ALGORITHM = "SLH-DSA"

        fun publicKeyBytes(key: PublicKey): ByteArray = (key as SLHDSAPublicKey).publicData

        fun privateKeyBytes(key: PrivateKey): ByteArray = PrivateKeyInfo.getInstance(key.encoded).privateKey.octets
    }
}
