package com.simats.kisancareai

import android.app.Application
import android.content.Context

class KisanMitraApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }
}
