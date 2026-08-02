package com.rakshyaa.rakshyaa.data

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jmnarloch.supabase.kaft.SupabaseClient
import io.github.jmnarloch.supabase.kaft.createSupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(
        @ApplicationContext context: Context
    ): SupabaseClient {
        // In a real app, these would come from secure storage or local.properties
        // For now, using placeholder values - replace with actual Supabase credentials
        val supabaseUrl = "https://glbaaslnwmodgpxqiuwn.supabase.co"
        val supabaseKey = "your-anon-key-here" // Replace with actual anon key

        return createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        )
    }
}