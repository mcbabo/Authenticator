package at.mcbabo.authenticator.internal.crypto

import android.util.Base64
import androidx.core.net.toUri
import at.mcbabo.authenticator.otp.Payload
import com.osmerion.kotlin.io.encoding.Base32
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

class OTPGenerator {
    companion object {
        private const val DEFAULT_TIME_STEP = 30L
        private const val DEFAULT_DIGITS = 6
        private const val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    }

    fun generateTOTP(
        base32Secret: String,
        timeStepSeconds: Long = DEFAULT_TIME_STEP,
        digits: Int = DEFAULT_DIGITS,
        algorithm: Algorithm = Algorithm.SHA1,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): String {
        require(digits in 6..8) { "Digits must be between 6 and 8" }
        require(timeStepSeconds > 0) { "Time step must be positive" }

        val secretKey = decodeBase32(base32Secret)
        val timeCounter = currentTimeMillis / 1000L / timeStepSeconds

        return generateTOTPFromBytes(secretKey, timeCounter, digits, algorithm)
    }

    fun generateTOTPFromBytes(
        secretKey: ByteArray,
        timeCounter: Long,
        digits: Int = DEFAULT_DIGITS,
        algorithm: Algorithm = Algorithm.SHA1
    ): String {
        val timeBytes = ByteBuffer.allocate(8).putLong(timeCounter).array()
        val hmac = generateHMAC(secretKey, timeBytes, algorithm)

        val offset = (hmac[hmac.size - 1].toInt() and 0x0f)
        val truncatedHash = (
                ((hmac[offset].toInt() and 0x7f) shl 24) or
                        ((hmac[offset + 1].toInt() and 0xff) shl 16) or
                        ((hmac[offset + 2].toInt() and 0xff) shl 8) or
                        (hmac[offset + 3].toInt() and 0xff)
                )

        val otp = truncatedHash % 10.0.pow(digits.toDouble()).toInt()
        return otp.toString().padStart(digits, '0')
    }

    private fun generateHMAC(key: ByteArray, data: ByteArray, algorithm: Algorithm): ByteArray {
        return try {
            val mac = Mac.getInstance(algorithm.value)
            mac.init(SecretKeySpec(key, algorithm.value))
            mac.doFinal(data)
        } catch (e: Exception) {
            throw RuntimeException("Failed to generate HMAC: ${e.message}", e)
        }
    }

    private fun decodeBase32(base32: String): ByteArray {
        val cleanInput = base32.uppercase().replace(Regex("[=\\s]"), "")
        require(cleanInput.isNotEmpty()) { "Base32 secret cannot be empty" }

        val result = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0

        for (char in cleanInput) {
            val value = BASE32_CHARS.indexOf(char)
            require(value != -1) { "Invalid Base32 character: $char" }

            buffer = (buffer shl 5) or value
            bitsLeft += 5

            if (bitsLeft >= 8) {
                result.add((buffer shr (bitsLeft - 8)).toByte())
                bitsLeft -= 8
            }
        }

        return result.toByteArray()
    }

    fun verifyTOTP(
        base32Secret: String,
        userCode: String,
        toleranceSteps: Int = 1,
        timeStepSeconds: Long = DEFAULT_TIME_STEP,
        digits: Int = DEFAULT_DIGITS,
        algorithm: Algorithm = Algorithm.SHA1,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        require(userCode.length == digits) { "Code length mismatch" }

        val secretKey = decodeBase32(base32Secret)
        val currentTimeCounter = currentTimeMillis / 1000L / timeStepSeconds

        for (i in -toleranceSteps..toleranceSteps) {
            val timeCounter = currentTimeCounter + i
            val generatedCode = generateTOTPFromBytes(secretKey, timeCounter, digits, algorithm)

            if (constantTimeEquals(generatedCode, userCode)) {
                return true
            }
        }

        return false
    }

    fun generateHOTP(
        base32Secret: String,
        counter: Long,
        digits: Int = 6,
        algorithm: Algorithm = Algorithm.SHA1
    ): String {
        val secretKey = decodeBase32(base32Secret)
        return generateTOTPFromBytes(secretKey, counter, digits, algorithm)
    }

    fun verifyHOTP(
        base32Secret: String,
        userCode: String,
        counter: Long,
        toleranceSteps: Int = 5,
        digits: Int = 6,
        algorithm: Algorithm = Algorithm.SHA1
    ): Boolean {
        val secretKey = decodeBase32(base32Secret)

        for (i in 0..toleranceSteps) {
            val generatedCode = generateTOTPFromBytes(secretKey, counter + i, digits, algorithm)
            if (constantTimeEquals(generatedCode, userCode)) return true
        }

        return false
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false

        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}

enum class QRCodeResult {
    ADD_ACCOUNT,
    IMPORT_ACCOUNTS,
    INVALID
}

fun parseQRCodeType(content: String): QRCodeResult {
    val uri = content.toUri()
    return when (uri.scheme) {
        "otpauth" -> {
            QRCodeResult.ADD_ACCOUNT
        }

        "otpauth-migration" -> {
            QRCodeResult.IMPORT_ACCOUNTS
        }

        else -> {
            QRCodeResult.INVALID
        }
    }
}

fun parseOTPAccountData(uriString: String): OtpAuthData? {
    val uri = uriString.toUri()
    if (uri.scheme != "otpauth") return null

    val type = when (uri.host?.uppercase()) {
        "TOTP" -> AuthType.TOTP
        "HOTP" -> AuthType.HOTP
        else -> return null
    }

    val label = uri.path?.removePrefix("/") ?: return null
    val (issuerFromLabel, accountName) = if (":" in label) {
        label.split(":", limit = 2)[0] to label.split(":", limit = 2)[1]
    } else {
        null to label
    }

    val secret = uri.getQueryParameter("secret") ?: return null
    val issuer = uri.getQueryParameter("issuer") ?: issuerFromLabel ?: "Unknown"

    val algorithm = when (uri.getQueryParameter("algorithm")) {
        "SHA1" -> Algorithm.SHA1
        "SHA256" -> Algorithm.SHA256
        "SHA512" -> Algorithm.SHA512
        else -> Algorithm.SHA1
    }

    val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
    val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30

    return OtpAuthData(accountName, issuer, secret, algorithm, digits, period, type)
}

fun parseOTPMigrationData(uri: String): List<OtpAuthData>? {
    val uri = uri.toUri()
    val data = uri.getQueryParameter("data") ?: return null
    val bytes = Base64.decode(data, Base64.DEFAULT)

    val payload = Payload.ADAPTER.decode(bytes)

    val accounts = mutableListOf<OtpAuthData>()

    for (otp in payload.otp_parameters) {
        val secretBytes: ByteArray = otp.secret.toByteArray()
        val secretBase32 = Base32.Default.encode(secretBytes)
        accounts.add(
            OtpAuthData(
                accountName = otp.name,
                issuer = otp.issuer,
                secret = secretBase32,
                digits = when (otp.digits) {
                    Payload.OtpParameters.DigitCount.DIGIT_COUNT_SIX -> 6
                    Payload.OtpParameters.DigitCount.DIGIT_COUNT_EIGHT -> 8
                    else -> 6
                },
                algorithm = when (otp.algorithm) {
                    Payload.OtpParameters.Algorithm.ALGORITHM_SHA1 -> Algorithm.SHA1
                    Payload.OtpParameters.Algorithm.ALGORITHM_SHA256 -> Algorithm.SHA256
                    Payload.OtpParameters.Algorithm.ALGORITHM_SHA512 -> Algorithm.SHA512
                    else -> Algorithm.SHA1
                },
                period = if (otp.type == Payload.OtpParameters.OtpType.OTP_TYPE_TOTP) 30 else 0,
                type = when (otp.type) {
                    Payload.OtpParameters.OtpType.OTP_TYPE_TOTP -> AuthType.TOTP
                    Payload.OtpParameters.OtpType.OTP_TYPE_HOTP -> AuthType.HOTP
                    else -> AuthType.TOTP
                },
                counter = otp.counter
            )
        )
    }

    return accounts
}

@Serializable
data class OtpAuthData(
    val accountName: String,
    val issuer: String,
    val secret: String,
    val algorithm: Algorithm,
    val digits: Int,
    val period: Int,
    val type: AuthType,
    val counter: Long = 0L
)
