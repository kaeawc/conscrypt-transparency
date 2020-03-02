package io.kaeawc.conscrypttransparency.di

import dagger.Component
import io.kaeawc.conscrypttransparency.App
import io.kaeawc.conscrypttransparency.ui.MainActivity
import javax.inject.Singleton

@Singleton
@Component(modules = [
    ApiModule::class
])
interface AppComponent {

    // Activities
    fun inject(mainActivity: MainActivity)

    // Android Application runtime
    fun inject(app: App)

    companion object {
        fun init(app: App): AppComponent =
            DaggerAppComponent.builder()
                .apiModule(ApiModule(app))
                .build()
    }
}
