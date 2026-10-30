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
import com.imrat.focuslock.ui.SchedulesScreen
import com.imrat.focuslock.lock.LockActivity
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
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
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
                            var showFormats by rememberSaveable { mutableStateOf(false) }
                            var showLockNow by rememberSaveable { mutableStateOf(false) }
                            var showSchedules by rememberSaveable { mutableStateOf(false) }
                            val apps by produceState(emptyList<AppEntry>()) { value = withContext(Dispatchers.IO) { InstalledApps.load(this@MainActivity) } }
                            if (showPerms) {
                                PermissionsScreen { showPerms = false }
                            } else if (showSchedules) {
                                BackHandler { showSchedules = false }
                                SchedulesScreen(s.profile.uid, apps, container.schedules, container.formats) { showSchedules = false }
                            } else if (showLockNow) {
                                BackHandler { showLockNow = false }
                                LockNowScreen(s.profile.uid, apps, container.formats, { mode, pkgs, ms ->
                                    lifecycleScope.launch {
                                        val started = withContext(Dispatchers.IO) {
                                            SessionEngine(this@MainActivity).startSelfInstant(s.profile.uid, mode, pkgs, ms)
                                        }
                                        showLockNow = false
                                        if (started != null) startActivity(LockActivity.intent(this@MainActivity))
                                    }
                                }) { showLockNow = false }
                            } else if (showFormats) {
                                BackHandler { showFormats = false }
                                FormatsScreen(s.profile.uid, s.profile.uid, apps, container.formats) { showFormats = false }
                            } else {
                                HomeScreen(
                                    s.profile,
                                    Perms.missing(this@MainActivity).size,
                                    { showPerms = true },
                                    { showFormats = true },
                                    { showLockNow = true },
                                    { showSchedules = true },
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
