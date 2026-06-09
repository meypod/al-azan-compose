package com.github.meypod.al_azan.core.presentation.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.LightColorScheme
import com.github.meypod.al_azan.core.presentation.components.LocalSnackbarController
import com.github.meypod.al_azan.core.presentation.components.SnackbarController
import com.github.meypod.al_azan.core.presentation.feedback.ScheduleFeedbackInfo
import com.github.meypod.al_azan.core.presentation.feedback.ScheduleFeedbackViewModel
import com.github.meypod.al_azan.core.presentation.mapper.reminderDisplayName
import com.github.meypod.al_azan.intro.IntroNavigation
import com.github.meypod.al_azan.main.MainNavigation
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Composable
fun NavigationRoot(
    appIntroDone: Boolean,
    startingRoute: Route?,
) {
    val rootBackStack =
        rememberNavBackStack(
            configuration =
                SavedStateConfiguration {
                    serializersModule = SerializersModule {
                        polymorphic(NavKey::class) {
                            subclass(Route.Intro::class, Route.Intro.serializer())
                            subclass(Route.Main::class, Route.Main.serializer())
                        }
                    }
                },
            if (appIntroDone) Route.Main else Route.Intro,
        )

    val snackbarController = remember { SnackbarController(SnackbarHostState()) }

    val scheduleFeedbackViewModel = hiltViewModel<ScheduleFeedbackViewModel>()
    val resources = LocalResources.current
    LaunchedEffect(snackbarController, scheduleFeedbackViewModel) {
        // Replace only within the same key: rapid reschedules of one thing (dragging a parameter)
        // collapse to the latest, while distinct signals (adhan vs a reminder, two reminders) queue
        // so neither clobbers the other.
        var shownKey: String? = null
        var showJob: Job? = null
        scheduleFeedbackViewModel.rescheduled.collect { info ->
            if (info.key == shownKey && showJob?.isActive == true) {
                // Same key still on screen → cancel its show() (which dismisses it) and reshow.
                showJob?.cancel()
            } else {
                // Different key → let the current one finish so it isn't overwritten.
                showJob?.join()
            }
            val message = when (info) {
                is ScheduleFeedbackInfo.Adhan ->
                    resources.getString(
                        R.string.prayer_times_rescheduled,
                        resources.getString(info.prayer.stringRes),
                        info.formattedTime,
                    )

                is ScheduleFeedbackInfo.Reminder ->
                    resources.getString(
                        R.string.reminder_rescheduled,
                        reminderDisplayName(resources, info.label, info.duration, info.durationModifier, info.prayer),
                        info.formattedTime,
                    )

                is ScheduleFeedbackInfo.ReminderBatch ->
                    resources.getString(R.string.reminders_rescheduled_batch, info.count)
            }
            shownKey = info.key
            showJob = launch { snackbarController.show(message) }
        }
    }

    CompositionLocalProvider(LocalSnackbarController provides snackbarController) {
        NavDisplay(
            backStack = rootBackStack,
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider =
                entryProvider {
                    entry<Route.Intro> {
                        MaterialTheme(colorScheme = LightColorScheme) {
                            IntroNavigation(
                                onFinishIntro = {
                                    rootBackStack.clear()
                                    rootBackStack.add(Route.Main)
                                },
                            )
                        }
                    }

                    entry<Route.Main> {
                        MainNavigation(
                            startingRoute,
                        )
                    }
                },
        )
    }
}
