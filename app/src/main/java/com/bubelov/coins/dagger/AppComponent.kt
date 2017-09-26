package com.bubelov.coins.dagger

import android.content.Context
import com.bubelov.coins.App
import com.bubelov.coins.ui.viewmodel.*

import javax.inject.Singleton

import dagger.Component
import dagger.BindsInstance
import dagger.android.AndroidInjectionModule

/**
 * @author Igor Bubelov
 */

@Singleton
@Component(modules = arrayOf(AppModule::class, DatabaseModule::class, AndroidInjectionModule::class, ActivityBuilder::class, FragmentBuilder::class, ServiceBuilder::class))
interface AppComponent {
    fun inject(app: App)

    fun inject(target: MainViewModel)
    fun inject(target: ExchangeRatesViewModel)
    fun inject(target: NotificationAreaViewModel)
    fun inject(target: PlacesSearchViewModel)
    fun inject(target: EditPlaceViewModel)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder
        fun build(): AppComponent
    }
}