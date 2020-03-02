package io.kaeawc.conscrypttransparency.di

import android.content.Context
import android.content.SharedPreferences
import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.squareup.moshi.Moshi
import dagger.Lazy
import dagger.Module
import dagger.Provides
import io.kaeawc.conscrypttransparency.*
import io.kaeawc.conscrypttransparency.api.GenericApi
import io.kaeawc.conscrypttransparency.okhttp.EmptyResponseFactory
import io.kaeawc.conscrypttransparency.storage.Prefs
import io.kaeawc.conscrypttransparency.utils.DateAdapter
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
class ApiModule(val app: App) {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder().add(DateAdapter::class.java).build()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(): SharedPreferences {
        return app.applicationContext.getSharedPreferences("ct", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun providePrefs(sharedPreferences: SharedPreferences, moshi: Moshi): Prefs {
        return Prefs(sharedPreferences, moshi)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient().newBuilder()
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(FlipperOkhttpInterceptor(NetworkFlipperPlugin()))
            .build()
    }

    @Provides
    @Singleton
    fun provideGenericApi(
            client: Lazy<OkHttpClient>,
            prefs: Prefs,
            moshi: Moshi
    ): GenericApi {

        return Retrofit.Builder()
            .baseUrl("https://${prefs.hostName}/")
            .callFactory(object: Call.Factory {
                override fun newCall(request: Request): Call {
                    return client.get().newCall(request)
                }
            })
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
            .addConverterFactory(EmptyResponseFactory())
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(GenericApi::class.java)
    }
}
