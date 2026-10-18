package com.widgetfactory.app

import android.app.Application
import com.widgetfactory.app.log.Logger

class WidgetFactoryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.init(this)
    }
}
