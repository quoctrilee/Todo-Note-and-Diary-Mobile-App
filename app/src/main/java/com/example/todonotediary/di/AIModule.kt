package com.example.todonotediary.di

import android.content.Context
import com.example.todonotediary.BuildConfig
import com.example.todonotediary.data.remote.groq.GroqApiService
import com.example.todonotediary.data.remote.groq.GroqClient
import com.example.todonotediary.data.repository.AIRepositoryImpl
import com.example.todonotediary.domain.repository.AIRepository
import com.example.todonotediary.utils.SpeechRecognizerHelper
import com.example.todonotediary.utils.TextToSpeechHelper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GroqRetrofit

@Module
@InstallIn(SingletonComponent::class)
object AIModule {
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
    
    @Provides
    @Singleton
    fun provideGroqOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    @GroqRetrofit
    fun provideGroqRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideGroqApiService(
        @GroqRetrofit retrofit: Retrofit
    ): GroqApiService {
        return retrofit.create(GroqApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideGroqClient(
        apiService: GroqApiService,
        gson: Gson
    ): GroqClient {
        return GroqClient(apiService, gson)
    }
    
    @Provides
    @Singleton
    fun provideAIRepository(
        groqClient: GroqClient
    ): AIRepository {
        return AIRepositoryImpl(groqClient)
    }
    
    @Provides
    @Singleton
    fun provideSpeechRecognizer(
        @ApplicationContext context: Context
    ): SpeechRecognizerHelper {
        return SpeechRecognizerHelper(context)
    }
    
    @Provides
    @Singleton
    fun provideTextToSpeech(
        @ApplicationContext context: Context
    ): TextToSpeechHelper {
        return TextToSpeechHelper(context)
    }
}
