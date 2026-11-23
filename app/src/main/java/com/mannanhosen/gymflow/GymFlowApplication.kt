package com.mannanhosen.gymflow

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Enables Hilt dependency injection for the whole app.
 */
@HiltAndroidApp
class GymFlowApplication : Application()