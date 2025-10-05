package at.mcbabo.authenticator.internal.crypto

import kotlinx.serialization.Serializable

@Serializable
enum class AuthType {
    TOTP,
    HOTP
}

@Serializable
enum class Algorithm(val value: String) {
    SHA1("HmacSHA1"),
    SHA256("HmacSHA256"),
    SHA512("HmacSHA512")
}
