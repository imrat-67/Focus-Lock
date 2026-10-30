package com.imrat.focuslock

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.imrat.focuslock.data.AppListRepository
import com.imrat.focuslock.data.AuthRepository
import com.imrat.focuslock.data.FormatRepository
import com.imrat.focuslock.data.LinkRepository
import com.imrat.focuslock.data.SessionRepository
import com.imrat.focuslock.data.UserRepository

/** Manual dependency container (no DI framework, keeps the APK small). */
class AppContainer(app: Application) {
    private val db = FirebaseFirestore.getInstance()
    val auth = AuthRepository(FirebaseAuth.getInstance(), app.getString(R.string.default_web_client_id))
    val users = UserRepository(db)
    val links = LinkRepository(db)
    val sessions = SessionRepository(db)
    val appList = AppListRepository(db)
    val formats = FormatRepository(db)
}

class FocusLockApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
