package com.medconnect

import android.app.Application
import com.medconnect.data.MedConnectRepository
import com.medconnect.data.local.TokenStore

class MedConnectApp : Application() {
    lateinit var repository: MedConnectRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = MedConnectRepository(TokenStore(this))
    }

    companion object {
        lateinit var instance: MedConnectApp
            private set
    }
}
