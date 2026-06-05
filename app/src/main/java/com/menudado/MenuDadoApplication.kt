package com.menudado

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.firebase.analytics.FirebaseAnalytics
import com.menudado.analytics.FirebaseMenuDadoAnalytics
import com.menudado.analytics.MenuDadoAnalytics
import com.menudado.ai.FirebaseHealthAnalyzer
import com.menudado.data.AiDailyUsageStore
import com.menudado.data.AiQuotaRetryStore
import com.menudado.data.MenuDadoDatabase
import com.menudado.data.MenuRepository
import com.menudado.data.DietaryProfileStore
import com.menudado.data.SharedPreferencesAiDailyUsageStore
import com.menudado.data.SharedPreferencesAiQuotaRetryStore
import com.menudado.data.SharedPreferencesDietaryProfileStore

class MenuDadoApplication : Application() {
    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE menus ADD COLUMN calories INTEGER")
            db.execSQL("ALTER TABLE menus ADD COLUMN lastPickedDate TEXT")
        }
    }

    private val database: MenuDadoDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            MenuDadoDatabase::class.java,
            "menu-dado.db"
        )
            .addMigrations(migration1To2)
            .build()
    }

    val repository: MenuRepository by lazy {
        MenuRepository(
            menuDao = database.menuDao(),
            healthAnalyzer = FirebaseHealthAnalyzer()
        )
    }

    val aiQuotaRetryStore: AiQuotaRetryStore by lazy {
        SharedPreferencesAiQuotaRetryStore(applicationContext)
    }

    val aiDailyUsageStore: AiDailyUsageStore by lazy {
        SharedPreferencesAiDailyUsageStore(applicationContext)
    }

    val dietaryProfileStore: DietaryProfileStore by lazy {
        SharedPreferencesDietaryProfileStore(applicationContext)
    }

    val analytics: MenuDadoAnalytics by lazy {
        FirebaseMenuDadoAnalytics(FirebaseAnalytics.getInstance(applicationContext))
    }
}
