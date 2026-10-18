package com.danyastry.pqc.feature.comparison

import com.danyastry.pqc.core.pqc.BouncyCastle
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.SignatureException
import java.security.interfaces.RSAPublicKey
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.ECGenParameterSpec
import org.bouncycastle.jcajce.interfaces.EdDSAPublicKey
import org.bouncycastle.jcajce.interfaces.MLDSAPublicKey
import org.bouncycastle.jcajce.interfaces.SLHDSAPublicKey
import org.bouncycastle.jcajce.spec.MLDSAParameterSpec
import org.bouncycastle.jcajce.spec.SLHDSAParameterSpec
import org.bouncycastle.jce.interfaces.ECPublicKey

class SignatureScheme(
    val name: String,
    val isQuantumSafe: Boolean,
    private val keyAlgorithm: String,
    private val signatureAlgorithm: String,
    private val keyParameters: AlgorithmParameterSpec?,
    private val keyBits: Int = 0,
    private val publicKeySize: (PublicKey) -> Int,
) {
    private val provider = BouncyCastle.provider
    private val random = SecureRandom()

    fun generateKeyPair(): KeyPair = KeyPairGenerator.getInstance(keyAlgorithm, provider).apply {
        if (keyParameters != null) initialize(keyParameters, random) else initialize(keyBits, random)
    }.generateKeyPair()

    fun publicKeyBytes(key: PublicKey): Int = publicKeySize(key)

    fun sign(key: PrivateKey, message: ByteArray): ByteArray = Signature.getInstance(signatureAlgorithm, provider).run {
        initSign(key, random)
        update(message)
        sign()
    }

    fun verify(key: PublicKey, message: ByteArray, signature: ByteArray): Boolean = try {
        Signature.getInstance(signatureAlgorithm, provider).run {
            initVerify(key)
            update(message)
            verify(signature)
        }
    } catch (_: SignatureException) {
        false
    }
}

object SignatureSchemes {

    private val ecPoint: (PublicKey) -> Int = { (it as ECPublicKey).q.getEncoded(false).size }
    private val mlDsa: (PublicKey) -> Int = { (it as MLDSAPublicKey).publicData.size }
    private val slhDsa: (PublicKey) -> Int = { (it as SLHDSAPublicKey).publicData.size }

    val all: List<SignatureScheme> = listOf(
        SignatureScheme("Ed25519", false, "Ed25519", "Ed25519", null, keyBits = 255) { (it as EdDSAPublicKey).pointEncoding.size },
        SignatureScheme("ECDSA P-256", false, "EC", "SHA256withECDSA", ECGenParameterSpec("secp256r1"), publicKeySize = ecPoint),
        SignatureScheme("RSA-3072", false, "RSA", "SHA256withRSA/PSS", null, keyBits = 3_072) { ((it as RSAPublicKey).modulus.bitLength() + 7) / 8 },
        SignatureScheme("ML-DSA-44", true, "ML-DSA", "ML-DSA", MLDSAParameterSpec.ml_dsa_44, publicKeySize = mlDsa),
        SignatureScheme("ML-DSA-65", true, "ML-DSA", "ML-DSA", MLDSAParameterSpec.ml_dsa_65, publicKeySize = mlDsa),
        SignatureScheme("ML-DSA-87", true, "ML-DSA", "ML-DSA", MLDSAParameterSpec.ml_dsa_87, publicKeySize = mlDsa),
        SignatureScheme("SLH-DSA-SHA2-128f", true, "SLH-DSA", "SLH-DSA", SLHDSAParameterSpec.slh_dsa_sha2_128f, publicKeySize = slhDsa),
        SignatureScheme("SLH-DSA-SHA2-128s", true, "SLH-DSA", "SLH-DSA", SLHDSAParameterSpec.slh_dsa_sha2_128s, publicKeySize = slhDsa),
    )
}
