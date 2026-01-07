package com.example.todonotediary.di

import com.example.todonotediary.data.remote.sentiment.SentimentApiService
import com.example.todonotediary.data.repository.SentimentRepositoryImpl
import com.example.todonotediary.domain.repository.SentimentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object SentimentModule {
    
    private const val SENTIMENT_BASE_URL = "http://192.168.1.5:8000/"
    
    @Provides
    @Singleton
    @Named("SentimentOkHttpClient")
    fun provideSentimentOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    @Named("SentimentRetrofit")
    fun provideSentimentRetrofit(
        @Named("SentimentOkHttpClient") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SENTIMENT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideSentimentApiService(
        @Named("SentimentRetrofit") retrofit: Retrofit
    ): SentimentApiService {
        return retrofit.create(SentimentApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideSentimentRepository(
        sentimentApiService: SentimentApiService
    ): SentimentRepository {
        return SentimentRepositoryImpl(sentimentApiService)
    }
}
