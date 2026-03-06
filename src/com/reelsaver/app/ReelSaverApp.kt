package com.reelsaver.app

import android.app.Application
import com.reelsaver.app.data.DatabaseHelper

class ReelSaverApp : Application() {
    lateinit var database: DatabaseHelper

    override fun onCreate() {
        super.onCreate()
        database = DatabaseHelper(this)
    }
}
