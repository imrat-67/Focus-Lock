package com.imrat.focuslock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.imrat.focuslock.R
import com.imrat.focuslock.data.UserProfile

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
fun LoadingScreen() = Centered {
    CircularProgressIndicator()
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.signing_in), style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun SignInScreen(errorRes: Int?, onSignIn: () -> Unit) = Centered {
    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
    Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(32.dp))
    Button(onClick = onSignIn) { Text(stringResource(R.string.sign_in_google)) }
    if (errorRes != null) {
        Spacer(Modifier.height(16.dp))
        Text(stringResource(errorRes), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    }
}

@Composable
fun OtherDeviceScreen(onSignOut: () -> Unit) = Centered {
    Text(stringResource(R.string.other_device_title), style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.other_device_body), textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
    Button(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
}
