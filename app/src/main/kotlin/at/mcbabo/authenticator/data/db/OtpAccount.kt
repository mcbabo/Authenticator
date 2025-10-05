package at.mcbabo.authenticator.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.mcbabo.authenticator.internal.crypto.Algorithm
import at.mcbabo.authenticator.internal.crypto.AuthType

@Entity(tableName = "otp_accounts")
data class OtpAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "account_name")
    val accountName: String, // e.g., "john@example.com"

    @ColumnInfo(name = "issuer")
    val issuer: String, // e.g., "Google", "GitHub", "Microsoft"

    @ColumnInfo(name = "encrypted_secret")
    val encryptedSecret: String, // Base64 encoded encrypted secret

    @ColumnInfo(name = "algorithm")
    val algorithm: Algorithm = Algorithm.SHA1, // SHA1, SHA256, SHA512

    @ColumnInfo(name = "digits")
    val digits: Int = 6, // Usually 6 or 8

    @ColumnInfo(name = "period")
    val period: Int = 30, // Time step in seconds

    @ColumnInfo(name = "type")
    val type: AuthType = AuthType.TOTP, // TOTP or HOTP

    @ColumnInfo(name = "counter")
    val counter: Long = 0, // For HOTP only

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "tags")
    val tags: String? = null, // JSON array of tags for organization

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0
)
