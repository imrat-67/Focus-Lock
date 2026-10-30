package com.imrat.focuslock.service

import android.app.admin.DeviceAdminReceiver

/** Active device admin blocks uninstall while enabled (SEC-1). No policies are used beyond that. */
class AdminReceiver : DeviceAdminReceiver()
