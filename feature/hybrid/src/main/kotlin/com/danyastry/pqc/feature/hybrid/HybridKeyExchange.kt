package com.danyastry.pqc.feature.hybrid

import com.danyastry.pqc.core.pqc.BouncyCastle
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation
import org.bouncycastle.jcajce.interfaces.MLKEMPublicKey
import org.bouncycastle.jcajce.interfaces.XDHPublicKey
import org.bouncycastle.jcajce.spec.KEMExtractSpec
import org.bouncycastle.jcajce.spec.KEMGenerateSpec
import org.bouncycastle.jcajce.spec.MLKEMParameterSpec
import org.bouncycastle.jcajce.spec.MLKEMPublicKeySpec
import org.bouncycastle.jcajce.spec.RawEncodedKeySpec

class ClientState(val mlKem: KeyPair, val x25519: KeyPair, val share: ByteArray)

class ServerResponse(val share: ByteArray, val mlKemSecret: ByteArray, val x25519Secret: ByteArray) {
    val sharedSecret: ByteArray
        get() = mlKemSecret + x25519Secret
}

class ClientResult(val mlKemSecret: ByteArray, val x25519Secret: ByteArray) {
    val sharedSecret: ByteArray
        get() = mlKemSecret + x25519Secret
}

class HybridKeyExchange(private val random: SecureRandom = SecureRandom()) {

    private val provider = BouncyCastle.provider

    fun clientHello(): ClientState {
        val mlKem = KeyPairGenerator.getInstance("ML-KEM", provider).apply { initialize(MLKEMParameterSpec.ml_kem_768, random) }.generateKeyPair()
        val x25519 = KeyPairGenerator.getInstance("X25519", provider).apply { initialize(X25519_KEY_BITS, random) }.generateKeyPair()
        val share = (mlKem.public as MLKEMPublicKey).publicData + rawX25519(x25519.public)
        return ClientState(mlKem, x25519, share)
    }

    fun serverRespond(clientShare: ByteArray): ServerResponse {
        require(clientShare.size == CLIENT_SHARE_BYTES) { "Client key share must be $CLIENT_SHARE_BYTES bytes, got ${clientShare.size}" }
        val clientMlKem = KeyFactory.getInstance("ML-KEM", provider)
            .generatePublic(MLKEMPublicKeySpec(MLKEMParameterSpec.ml_kem_768, clientShare.copyOfRange(0, ML_KEM_KEY_BYTES)))
        val clientX25519 = x25519PublicKey(clientShare.copyOfRange(ML_KEM_KEY_BYTES, CLIENT_SHARE_BYTES))

        val encapsulated = KeyGenerator.getInstance("ML-KEM", provider).run {
            init(KEMGenerateSpec(clientMlKem, "AES", SECRET_BITS), random)
            generateKey() as SecretKeyWithEncapsulation
        }
        val serverX25519 = KeyPairGenerator.getInstance("X25519", provider).apply { initialize(X25519_KEY_BITS, random) }.generateKeyPair()
        val x25519Secret = agree(serverX25519, clientX25519)
        return ServerResponse(encapsulated.encapsulation + rawX25519(serverX25519.public), encapsulated.encoded, x25519Secret)
    }

    fun clientFinish(client: ClientState, serverShare: ByteArray): ClientResult {
        require(serverShare.size == SERVER_SHARE_BYTES) { "Server key share must be $SERVER_SHARE_BYTES bytes, got ${serverShare.size}" }
        val ciphertext = serverShare.copyOfRange(0, ML_KEM_CIPHERTEXT_BYTES)
        val serverX25519 = x25519PublicKey(serverShare.copyOfRange(ML_KEM_CIPHERTEXT_BYTES, SERVER_SHARE_BYTES))
        val mlKemSecret = KeyGenerator.getInstance("ML-KEM", provider).run {
            init(KEMExtractSpec(client.mlKem.private, ciphertext, "AES", SECRET_BITS))
            generateKey().encoded
        }
        return ClientResult(mlKemSecret, agree(client.x25519, serverX25519))
    }

    private fun agree(own: KeyPair, peer: PublicKey): ByteArray =
        KeyAgreement.getInstance("X25519", provider).run {
            init(own.private)
            doPhase(peer, true)
            generateSecret()
        }

    private fun rawX25519(key: PublicKey): ByteArray = (key as XDHPublicKey).uEncoding

    private fun x25519PublicKey(raw: ByteArray): PublicKey =
        KeyFactory.getInstance("X25519", provider).generatePublic(RawEncodedKeySpec(raw))

    companion object {
        const val ML_KEM_KEY_BYTES = 1_184
        const val ML_KEM_CIPHERTEXT_BYTES = 1_088
        const val X25519_BYTES = 32
        const val SECRET_BYTES = 32
        const val CLIENT_SHARE_BYTES = ML_KEM_KEY_BYTES + X25519_BYTES
        const val SERVER_SHARE_BYTES = ML_KEM_CIPHERTEXT_BYTES + X25519_BYTES
        const val SESSION_KEY_BYTES = 32
        private const val X25519_KEY_BITS = 255
        private const val SECRET_BITS = SECRET_BYTES * Byte.SIZE_BITS
        private val SESSION_INFO = "post-quantum hybrid demo session key".encodeToByteArray()

        fun sessionKey(sharedSecret: ByteArray): ByteArray {
            val hkdf = HKDFBytesGenerator(SHA256Digest()).apply { init(HKDFParameters(sharedSecret, null, SESSION_INFO)) }
            return ByteArray(SESSION_KEY_BYTES).also { hkdf.generateBytes(it, 0, it.size) }
        }
    }
}
