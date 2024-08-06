package de.moekadu.tuner.hilt

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesScopeModule {
    @ApplicationScope
    @Singleton
    @Provides
    fun providesCoroutineContext(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}
