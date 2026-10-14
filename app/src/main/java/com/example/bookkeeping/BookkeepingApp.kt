package com.example.bookkeeping

import android.app.Application

class BookkeepingApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.prepareStartup()
    }
}
