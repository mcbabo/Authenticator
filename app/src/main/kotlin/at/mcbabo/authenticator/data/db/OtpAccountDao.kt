package at.mcbabo.authenticator.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface OtpAccountDao {
    @Query("SELECT * FROM otp_accounts ORDER BY sort_order ASC, account_name ASC")
    suspend fun getAllAccounts(): List<OtpAccount>

    @Query("SELECT * FROM otp_accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): OtpAccount?

    @Query("SELECT * FROM otp_accounts WHERE issuer LIKE :issuer OR account_name LIKE :query")
    suspend fun searchAccounts(query: String, issuer: String = "%$query%"): List<OtpAccount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: OtpAccount): Long

    @Update
    suspend fun updateAccount(account: OtpAccount)

    @Query("UPDATE otp_accounts SET counter = counter + 1 WHERE id = :id")
    suspend fun updateCounter(id: Long)

    @Delete
    suspend fun deleteAccount(account: OtpAccount)

    @Query("DELETE FROM otp_accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)
}
