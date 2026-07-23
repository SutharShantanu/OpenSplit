package com.opensplit

import android.app.Application
import com.opensplit.di.AppContainer

class OpenSplitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
