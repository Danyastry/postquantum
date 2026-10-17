package com.danyastry.pqc.core.pqc

import java.security.MessageDigest

object Fingerprint {

    private const val SHOWN_BYTES = 8

    fun of(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).take(SHOWN_BYTES)
            .joinToString(":") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}
