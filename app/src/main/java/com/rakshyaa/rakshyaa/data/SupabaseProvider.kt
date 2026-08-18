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
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(
        @ApplicationContext context: Context
    ): SupabaseClient {
        // Load Supabase credentials from local.properties file
        val props = Properties()
        val supabaseUrl: String
        val supabaseKey: String

        try {
            context.assets.open("local.properties").use { input ->
                props.load(input)
            }
            supabaseUrl = props.getProperty("SUPABASE_URL") ?: "https://glbaaslnwmodgpxqiuwn.supabase.co"
            supabaseKey = props.getProperty("SUPABASE_ANON_KEY") ?: "your-anon-key-here"
        } catch (e: IOException) {
            // Fallback to hardcoded values if local.properties is not found
            supabaseUrl = "https://glbaaslnwmodgpxqiuwn.supabase.co"
            supabaseKey = "your-anon-key-here" // Replace with actual anon key
        }

        return createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        )
    }
}