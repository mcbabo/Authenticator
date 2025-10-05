package at.mcbabo.authenticator.data.di

import android.content.Context
import androidx.room.Room
import at.mcbabo.authenticator.data.db.AppDatabase
import at.mcbabo.authenticator.data.db.OtpAccountDao
import at.mcbabo.authenticator.data.repository.OtpAccountRepository
import at.mcbabo.authenticator.data.store.UserPreferences
import at.mcbabo.authenticator.internal.crypto.OTPGenerator
import at.mcbabo.authenticator.internal.crypto.OtpSecretEncryption
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

const val DATABASE_NAME = "otp-db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideOtpDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideOtpAccountDao(database: AppDatabase): OtpAccountDao {
        return database.otpAccountDao()
    }
}


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object CryptoModule {

    @Provides
    @Singleton
    fun provideOtpSecretEncryption(): OtpSecretEncryption {
        return OtpSecretEncryption()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object OtpModule {

    @Provides
    @Singleton
    fun provideTOTPGenerator(): OTPGenerator {
        return OTPGenerator()
    }

    @Provides
    @Singleton
    fun provideOtpAccountRepository(
        dao: OtpAccountDao,
        encryption: OtpSecretEncryption
    ): OtpAccountRepository {
        return OtpAccountRepository(dao, encryption)
    }
}
