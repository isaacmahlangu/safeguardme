// =============================================
// AppModule.kt - Updated DI Configuration
// =============================================

package com.safeguardme.app.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.safeguardme.app.auth.AuthRepository
import com.safeguardme.app.data.repositories.ContactRepository
import com.safeguardme.app.data.repositories.EmergencyContactRepository
import com.safeguardme.app.data.repositories.FirebaseSafetyDataSource
import com.safeguardme.app.data.repositories.IncidentRepository
import com.safeguardme.app.data.repositories.SafetyDataSource
import com.safeguardme.app.data.repositories.SafetyRepository
import com.safeguardme.app.data.repositories.SafetyRepositoryImpl
import com.safeguardme.app.data.repositories.SettingsRepository
import com.safeguardme.app.data.repositories.StorageRepository
import com.safeguardme.app.data.repositories.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // === FIREBASE CORE ===
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    // === REPOSITORIES ===
    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): UserRepository = UserRepository(firestore, auth)

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        userRepository: UserRepository
    ): AuthRepository = AuthRepository(auth, userRepository)

    // ✅ NEW: EmergencyContactRepository
    @Provides
    @Singleton
    fun provideEmergencyContactRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        userRepository: UserRepository
    ): EmergencyContactRepository = EmergencyContactRepository(firestore, auth, userRepository)

    @Provides
    @Singleton
    fun provideContactRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): ContactRepository = ContactRepository(firestore, auth)

    @Provides
    @Singleton
    fun provideIncidentRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): IncidentRepository = IncidentRepository(firestore, auth)

    @Provides
    @Singleton
    fun provideStorageRepository(
        storage: FirebaseStorage,
        auth: FirebaseAuth
    ): StorageRepository = StorageRepository(storage, auth)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepository(context)

    @Provides @Singleton
    fun provideSafetyDataSource(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): SafetyDataSource =
        FirebaseSafetyDataSource(firestore, auth)          // ← concrete impl

    @Provides @Singleton
    fun provideSafetyRepository(
        dataSource: SafetyDataSource,
        userRepository: UserRepository
    ): SafetyRepository =
        SafetyRepositoryImpl(dataSource, userRepository)
}
