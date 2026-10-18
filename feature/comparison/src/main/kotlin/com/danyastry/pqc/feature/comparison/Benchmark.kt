package com.danyastry.pqc.feature.comparison

data class KeyExchangeResult(
    val name: String,
    val isQuantumSafe: Boolean,
    val publicKeyBytes: Int,
    val wireBytes: Int,
    val keyGenMillis: Double,
    val sendMillis: Double,
    val receiveMillis: Double,
)

data class SignatureResult(
    val name: String,
    val isQuantumSafe: Boolean,
    val publicKeyBytes: Int,
    val signatureBytes: Int,
    val keyGenMillis: Double,
    val signMillis: Double,
    val verifyMillis: Double,
)

class Benchmark(
    private val clock: () -> Long = System::nanoTime,
    private val runs: Int = RUNS,
    private val warmups: Int = WARMUPS,
) {

    fun keyExchange(scheme: KeyExchangeScheme): KeyExchangeResult {
        val (keyGenMillis, receiver) = measure { scheme.generateKeyPair() }
        val (sendMillis, delivery) = measure { scheme.send(receiver.public) }
        val (receiveMillis, secret) = measure { scheme.receive(receiver, delivery.wire) }
        check(secret.contentEquals(delivery.secret)) { "${scheme.name} gave the receiver a different secret" }
        return KeyExchangeResult(
            name = scheme.name,
            isQuantumSafe = scheme.isQuantumSafe,
            publicKeyBytes = scheme.publicKeyBytes(receiver.public),
            wireBytes = delivery.wire.size,
            keyGenMillis = keyGenMillis,
            sendMillis = sendMillis,
            receiveMillis = receiveMillis,
        )
    }

    fun signature(scheme: SignatureScheme): SignatureResult {
        val (keyGenMillis, pair) = measure { scheme.generateKeyPair() }
        val (signMillis, signature) = measure { scheme.sign(pair.private, MESSAGE) }
        val (verifyMillis, valid) = measure { scheme.verify(pair.public, MESSAGE, signature) }
        check(valid) { "${scheme.name} could not verify its own signature" }
        return SignatureResult(
            name = scheme.name,
            isQuantumSafe = scheme.isQuantumSafe,
            publicKeyBytes = scheme.publicKeyBytes(pair.public),
            signatureBytes = signature.size,
            keyGenMillis = keyGenMillis,
            signMillis = signMillis,
            verifyMillis = verifyMillis,
        )
    }

    private fun <T> measure(operation: () -> T): Pair<Double, T> {
        repeat(warmups) { operation() }
        val samples = List(runs) {
            val start = clock()
            val output = operation()
            (clock() - start) to output
        }
        val median = samples.map { it.first }.sorted()[runs / 2]
        return median / NANOS_PER_MILLI to samples.last().second
    }

    companion object {
        const val RUNS = 3
        const val WARMUPS = 1
        private const val NANOS_PER_MILLI = 1_000_000.0
        private val MESSAGE = "Pay Alice 100 dollars".encodeToByteArray()
    }
}
