package com.danyastry.pqc.feature.comparison

import com.danyastry.pqc.core.pqc.BouncyCastle
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation
import org.bouncycastle.jcajce.interfaces.MLKEMPublicKey
import org.bouncycastle.jcajce.interfaces.XDHPublicKey
import org.bouncycastle.jcajce.spec.KEMExtractSpec
import org.bouncycastle.jcajce.spec.KEMGenerateSpec
import org.bouncycastle.jcajce.spec.MLKEMParameterSpec
import org.bouncycastle.jcajce.spec.RawEncodedKeySpec
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.spec.ECPublicKeySpec

class Delivery(val secret: ByteArray, val wire: ByteArray)

interface KeyExchangeScheme {
    val name: String
    val isQuantumSafe: Boolean
    fun generateKeyPair(): KeyPair
    fun publicKeyBytes(key: PublicKey): Int
    fun send(recipient: PublicKey): Delivery
    fun receive(own: KeyPair, wire: ByteArray): ByteArray
}

private val provider = BouncyCastle.provider
private val random = SecureRandom()
private const val SECRET_BYTES = 32

class X25519Exchange : KeyExchangeScheme {
    override val name = "X25519"
    override val isQuantumSafe = false

    override fun generateKeyPair(): KeyPair = KeyPairGenerator.getInstance("X25519", provider).apply { initialize(255, random) }.generateKeyPair()

    override fun publicKeyBytes(key: PublicKey): Int = (key as XDHPublicKey).uEncoding.size

    override fun send(recipient: PublicKey): Delivery {
        val ephemeral = generateKeyPair()
        return Delivery(agree(ephemeral, recipient), (ephemeral.public as XDHPublicKey).uEncoding)
    }

    override fun receive(own: KeyPair, wire: ByteArray): ByteArray =
        agree(own, KeyFactory.getInstance("X25519", provider).generatePublic(RawEncodedKeySpec(wire)))

    private fun agree(own: KeyPair, peer: PublicKey): ByteArray = KeyAgreement.getInstance("X25519", provider).run {
        init(own.private)
        doPhase(peer, true)
        generateSecret()
    }
}

class EcdhP256Exchange : KeyExchangeScheme {
    override val name = "ECDH P-256"
    override val isQuantumSafe = false

    private val curve = ECNamedCurveTable.getParameterSpec(CURVE)

    override fun generateKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("EC", provider).apply { initialize(ECGenParameterSpec(CURVE), random) }.generateKeyPair()

    override fun publicKeyBytes(key: PublicKey): Int = point(key).size

    override fun send(recipient: PublicKey): Delivery {
        val ephemeral = generateKeyPair()
        return Delivery(agree(ephemeral, recipient), point(ephemeral.public))
    }

    override fun receive(own: KeyPair, wire: ByteArray): ByteArray {
        val peer = KeyFactory.getInstance("EC", provider).generatePublic(ECPublicKeySpec(curve.curve.decodePoint(wire), curve))
        return agree(own, peer)
    }

    private fun point(key: PublicKey): ByteArray = (key as ECPublicKey).q.getEncoded(false)

    private fun agree(own: KeyPair, peer: PublicKey): ByteArray = KeyAgreement.getInstance("ECDH", provider).run {
        init(own.private)
        doPhase(peer, true)
        generateSecret()
    }

    private companion object {
        const val CURVE = "secp256r1"
    }
}

class RsaOaepExchange(private val bits: Int = 3_072) : KeyExchangeScheme {
    override val name = "RSA-$bits"
    override val isQuantumSafe = false

    private val oaep = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT)

    override fun generateKeyPair(): KeyPair = KeyPairGenerator.getInstance("RSA", provider).apply { initialize(bits, random) }.generateKeyPair()

    override fun publicKeyBytes(key: PublicKey): Int = ((key as RSAPublicKey).modulus.bitLength() + 7) / Byte.SIZE_BITS

    override fun send(recipient: PublicKey): Delivery {
        val secret = ByteArray(SECRET_BYTES).also(random::nextBytes)
        val wire = Cipher.getInstance(TRANSFORMATION, provider).run {
            init(Cipher.ENCRYPT_MODE, recipient, oaep, random)
            doFinal(secret)
        }
        return Delivery(secret, wire)
    }

    override fun receive(own: KeyPair, wire: ByteArray): ByteArray = Cipher.getInstance(TRANSFORMATION, provider).run {
        init(Cipher.DECRYPT_MODE, own.private, oaep)
        doFinal(wire)
    }

    private companion object {
        const val TRANSFORMATION = "RSA/NONE/OAEPPadding"
    }
}

class MlKemExchange(private val spec: MLKEMParameterSpec) : KeyExchangeScheme {
    override val name = spec.name
    override val isQuantumSafe = true

    override fun generateKeyPair(): KeyPair = KeyPairGenerator.getInstance("ML-KEM", provider).apply { initialize(spec, random) }.generateKeyPair()

    override fun publicKeyBytes(key: PublicKey): Int = (key as MLKEMPublicKey).publicData.size

    override fun send(recipient: PublicKey): Delivery {
        val key = KeyGenerator.getInstance("ML-KEM", provider).run {
            init(KEMGenerateSpec(recipient, "AES", SECRET_BYTES * Byte.SIZE_BITS), random)
            generateKey() as SecretKeyWithEncapsulation
        }
        return Delivery(key.encoded, key.encapsulation)
    }

    override fun receive(own: KeyPair, wire: ByteArray): ByteArray = KeyGenerator.getInstance("ML-KEM", provider).run {
        init(KEMExtractSpec(own.private, wire, "AES", SECRET_BYTES * Byte.SIZE_BITS))
        generateKey().encoded
    }
}

object KeyExchangeSchemes {
    val all: List<KeyExchangeScheme> = listOf(
        X25519Exchange(),
        EcdhP256Exchange(),
        RsaOaepExchange(),
        MlKemExchange(MLKEMParameterSpec.ml_kem_512),
        MlKemExchange(MLKEMParameterSpec.ml_kem_768),
        MlKemExchange(MLKEMParameterSpec.ml_kem_1024),
    )
}
