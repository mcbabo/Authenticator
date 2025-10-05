package at.mcbabo.authenticator.data.repository

import at.mcbabo.authenticator.data.db.OtpAccount
import at.mcbabo.authenticator.data.db.OtpAccountDao
import at.mcbabo.authenticator.internal.crypto.Algorithm
import at.mcbabo.authenticator.internal.crypto.AuthType
import at.mcbabo.authenticator.internal.crypto.OtpSecretEncryption
import javax.inject.Inject

class OtpAccountRepository @Inject constructor(
    private val dao: OtpAccountDao,
    private val encryption: OtpSecretEncryption
) {
    suspend fun addAccount(
        accountName: String,
        issuer: String,
        secret: String,
        algorithm: Algorithm = Algorithm.SHA1,
        digits: Int = 6,
        period: Int = 30,
        type: AuthType = AuthType.TOTP
    ): Result<Long> {
        return encryption.encrypt(secret).mapCatching { encryptedSecret ->
            val account = OtpAccount(
                accountName = accountName,
                issuer = issuer,
                encryptedSecret = encryptedSecret,
                algorithm = algorithm,
                digits = digits,
                period = period,
                type = type
            )
            dao.insertAccount(account)
        }
    }

    suspend fun getDecryptedSecret(accountId: Long): Result<String> {
        return runCatching {
            val account = dao.getAccountById(accountId)
                ?: throw NoSuchElementException("Account not found: $accountId")

            encryption.decrypt(account.encryptedSecret).getOrThrow()
        }
    }

    suspend fun getAllAccounts(): List<OtpAccount> {
        return dao.getAllAccounts()
    }

    suspend fun getAccountById(accountId: Long): OtpAccount? {
        return dao.getAccountById(accountId)
    }

    suspend fun updateAccount(account: OtpAccount): Result<Unit> {
        return runCatching {
            dao.updateAccount(account)
        }
    }

    suspend fun updateCounter(account: OtpAccount) {
        dao.updateCounter(account.id)
    }

    suspend fun updateAccountSecret(accountId: Long, newSecret: String): Result<Unit> {
        return encryption.encrypt(newSecret).mapCatching { encryptedSecret ->
            val account = dao.getAccountById(accountId)
                ?: throw NoSuchElementException("Account not found: $accountId")

            dao.updateAccount(account.copy(encryptedSecret = encryptedSecret))
        }
    }

    suspend fun deleteAccount(accountId: Long): Result<Unit> {
        return runCatching {
            dao.deleteAccountById(accountId)
        }
    }

    suspend fun deleteAccount(account: OtpAccount): Result<Unit> {
        return runCatching {
            dao.deleteAccount(account)
        }
    }

    suspend fun reorderAccounts(accounts: List<OtpAccount>): Result<Unit> {
        return runCatching {
            accounts.forEachIndexed { index, account ->
                dao.updateAccount(account.copy(sortOrder = index))
            }
        }
    }

    suspend fun verifyEncryptionAvailable(): Boolean {
        return encryption.verifyKeyAccess()
    }

    suspend fun exportAccounts(): Result<List<Pair<OtpAccount, String>>> {
        return runCatching {
            val accounts = dao.getAllAccounts()
            accounts.map { account ->
                val decryptedSecret = encryption.decrypt(account.encryptedSecret).getOrThrow()
                account to decryptedSecret
            }
        }
    }
}
