package com.mannanhosen.gymflow.core.util

import javax.inject.Qualifier

/** Qualifiers for injecting dispatchers, keeping ViewModels testable. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher