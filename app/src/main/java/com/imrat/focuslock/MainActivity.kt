package com.imrat.focuslock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.produceState
import com.imrat.focuslock.data.AppEntry
import com.imrat.focuslock.lock.InstalledApps
import com.imrat.focuslock.ui.FormatsScreen
import com.imrat.focuslock.ui.LockNowScreen
import com.imrat.focuslock.ui.LinksScreen
import com.imrat.focuslock.ui.RemoteDeviceScreen
import com.imrat.focuslock.lock.SessionCache
import android.os.SystemClock
import com.imrat.focuslock.ui.SchedulesScreen
import com.imrat.focuslock.lock.LockActivity
import com.imrat.focuslock.R
import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.ui.LockState
import com.imrat.focuslock.lock.SessionEngine
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imrat.focuslock.ui.AuthUiState
import com.imrat.focuslock.ui.AuthViewModel
import com.imrat.focuslock.ui.HomeScreen
import com.imrat.focuslock.ui.AddDeviceScreen
import com.imrat.focuslock.ui.AddRemoteScreen
import com.imrat.focuslock.ui.ThisDeviceScreen
import com.imrat.focuslock.data.DeviceStore
import androidx.compose.runtime.remember
import com.imrat.focuslock.ui.FocusLockTheme
import com.imrat.focuslock.ui.LoadingScreen
import com.imrat.focuslock.security.Perms
import com.imrat.focuslock.service.CoreService
import com.imrat.focuslock.ui.OtherDeviceScreen
import com.imrat.focuslock.ui.PermissionsScreen
import com.imrat.focuslock.ui.SignInScreen
import com.imrat.focuslock.util.DeviceId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as FocusLockApp).container
        setContent {
            FocusLockTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val vm: AuthViewModel = viewModel(
                        factory = AuthViewModel.Factory(container.auth, container.users, DeviceId.get(this)),
                    )
                    val state by vm.state.collectAsStateWithLifecycle()
                    var showPerms by rememberSaveable { mutableStateOf(Perms.missing(this@MainActivity).isNotEmpty()) }
                    when (val s = state) {
                        AuthUiState.Loading -> LoadingScreen()
                        is AuthUiState.SignedOut -> SignInScreen(s.errorRes) { vm.signIn(this) }
                        is AuthUiState.Ready -> {
                            LaunchedEffect(Unit) { CoreService.start(this@MainActivity) }
                            var route by rememberSaveable { mutableStateOf("home") }
                            var remoteUid by rememberSaveable { mutableStateOf<String?>(null) }
                            var remoteName by rememberSaveable { mutableStateOf("") }
                            val store = remember(s.profile.uid) { DeviceStore(this@MainActivity, s.profile.uid) }
                            val apps by produceState(emptyList<AppEntry>()) { value = withContext(Dispatchers.IO) { InstalledApps.load(this@MainActivity) } }
                            val lockStateNow by produceState(LockState.UNLOCKED) { // AUTH-3: re-check every second
                                while (true) {
                                    val t = SystemClock.elapsedRealtime()
                                    value = LockState.of(SessionCache(this@MainActivity).get().filter { it.endElapsedMs > t }.map { it.mode })
                                    kotlinx.coroutines.delay(1_000)
                                }
                            }
                            val lockActiveNow = lockStateNow != LockState.UNLOCKED
                            val uid = s.profile.uid
                            if (showPerms) {
                                PermissionsScreen { showPerms = false }
                            } else if (remoteUid != null) {
                                BackHandler { remoteUid = null }
                                RemoteDeviceScreen(remoteUid.orEmpty(), uid, remoteName, container) { remoteUid = null }
                            } else when (route) {
                                "add" -> {
                                    BackHandler { route = "home" }
                                    AddDeviceScreen(store.myDevice.value, { store.setMyDevice(true); route = "home" }, { route = "addRemote" }) { route = "home" }
                                }
                                "addRemote" -> {
                                    BackHandler { route = "add" }
                                    AddRemoteScreen(uid, container, { route = "home" }) { route = "add" }
                                }
                                "this" -> {
                                    BackHandler { route = "home" }
                                    ThisDeviceScreen(lockStateNow, { route = "instant" }, { route = "schedules" }, { route = "formats" }) { route = "home" }
                                }
                                "schedules" -> {
                                    BackHandler { route = "this" }
                                    SchedulesScreen(uid, apps, container.schedules, container.formats) { route = "this" }
                                }
                                "formats" -> {
                                    BackHandler { route = "this" }
                                    FormatsScreen(uid, uid, apps, container.formats) { route = "this" }
                                }
                                "instant" -> {
                                    BackHandler { route = "this" }
                                    LockNowScreen(uid, apps, container.formats, { mode, pkgs, ms ->
                                        lifecycleScope.launch {
                                            val started = withContext(Dispatchers.IO) {
                                                SessionEngine(this@MainActivity).startSelfInstant(uid, mode, pkgs, ms)
                                            }
                                            route = "this"
                                            if (started != null) {
                                                // Only a Full Lock covers Focus Lock itself; an Apps Lock just confirms (T14g).
                                                if (started.mode == LockMode.FULL) startActivity(LockActivity.intent(this@MainActivity))
                                                else android.widget.Toast.makeText(this@MainActivity, R.string.toast_apps_lock_on, android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }) { route = "this" }
                                }
                                else -> HomeScreen(
                                    s.profile, Perms.missing(this@MainActivity).size, lockStateNow, container, store,
                                    { route = "add" }, { route = "this" }, { u, n -> remoteUid = u; remoteName = n },
                                    { showPerms = true },
                                ) { vm.signOut(this@MainActivity) }
                            }
                        }
                        AuthUiState.OtherDevice -> OtherDeviceScreen { vm.signOut(this) }
                    }
                }
            }
        }
    }
}
