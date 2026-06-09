package com.github.meypod.al_azan.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.navigation.BindBackStackWithController
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.core.presentation.navigation.navigateTo
import com.github.meypod.al_azan.core.presentation.navigation.rememberHorizontalSlideDirections
import com.github.meypod.al_azan.main.about.AboutScreen
import com.github.meypod.al_azan.main.about.AboutViewModel
import com.github.meypod.al_azan.main.counter.CounterScreen
import com.github.meypod.al_azan.main.counter.CounterViewModel
import com.github.meypod.al_azan.main.home.HomePermissionGate
import com.github.meypod.al_azan.main.home.HomeScreen
import com.github.meypod.al_azan.main.home.HomeViewModel
import com.github.meypod.al_azan.main.home.RamadanNoticeGate
import com.github.meypod.al_azan.main.location.LocationScreen
import com.github.meypod.al_azan.main.location.LocationViewModel
import com.github.meypod.al_azan.main.monthly.MonthlyViewScreen
import com.github.meypod.al_azan.main.monthly.MonthlyViewViewModel
import com.github.meypod.al_azan.main.qibla.QiblaCompassRoute
import com.github.meypod.al_azan.main.qibla.QiblaCompassViewModel
import com.github.meypod.al_azan.main.qibla.QiblaScreen
import com.github.meypod.al_azan.main.qibla.QiblaViewModel
import com.github.meypod.al_azan.main.reminder.ReminderScreen
import com.github.meypod.al_azan.main.reminder.ReminderViewModel
import com.github.meypod.al_azan.main.settings.adhan.AdhanSettingsScreen
import com.github.meypod.al_azan.main.settings.adhan.AdhanSettingsViewModel
import com.github.meypod.al_azan.main.settings.adhan.PrayerScheduleScreen
import com.github.meypod.al_azan.main.settings.adhan.ScheduleAndMuezzinScreen
import com.github.meypod.al_azan.main.settings.appearance.InterfaceSettingsScreen
import com.github.meypod.al_azan.main.settings.appearance.InterfaceSettingsViewModel
import com.github.meypod.al_azan.main.settings.backup.BackupRestoreScreen
import com.github.meypod.al_azan.main.settings.backup.BackupRestoreViewModel
import com.github.meypod.al_azan.main.settings.calculation.CalculationSettingsScreen
import com.github.meypod.al_azan.main.settings.calculation.CalculationSettingsViewModel
import com.github.meypod.al_azan.main.settings.calculation.adjustments.AdjustmentsScreen
import com.github.meypod.al_azan.main.settings.calculation.adjustments.AdjustmentsViewModel
import com.github.meypod.al_azan.main.settings.calculation.advanced.AdvancedCalcScreen
import com.github.meypod.al_azan.main.settings.calculation.advanced.AdvancedCalcViewModel
import com.github.meypod.al_azan.main.settings.developer.DeveloperScreen
import com.github.meypod.al_azan.main.settings.developer.DeveloperViewModel
import com.github.meypod.al_azan.main.settings.menu.SettingsMenuScreen
import com.github.meypod.al_azan.main.settings.menu.SettingsMenuViewModel
import com.github.meypod.al_azan.main.settings.troubleshoot.TroubleshootScreen
import com.github.meypod.al_azan.main.settings.troubleshoot.TroubleshootViewModel
import com.github.meypod.al_azan.main.settings.troubleshoot.advanced.AdvancedTroubleshootScreen
import com.github.meypod.al_azan.main.settings.troubleshoot.advanced.AdvancedTroubleshootViewModel
import com.github.meypod.al_azan.main.settings.widget.WidgetSettingsScreen
import com.github.meypod.al_azan.main.settings.widget.WidgetSettingsViewModel
import com.github.meypod.al_azan.main.upcoming_alarms.UpcomingAlarmsScreen
import com.github.meypod.al_azan.main.upcoming_alarms.UpcomingAlarmsViewModel
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Composable
fun MainNavigation(
    startingRoute: Route?,
    modifier: Modifier = Modifier,
) {
    val slideDirections = rememberHorizontalSlideDirections()

    val mainBackstack =
        rememberNavBackStack(
            configuration =
                SavedStateConfiguration {
                    serializersModule = SerializersModule {
                        polymorphic(NavKey::class) {
                            subclass(Route.Main.Home::class, Route.Main.Home.serializer())
                            subclass(Route.Main.Location::class, Route.Main.Location.serializer())
                            subclass(Route.Main.CalendarView::class, Route.Main.CalendarView.serializer())
                            subclass(Route.Main.MonthlyView::class, Route.Main.MonthlyView.serializer())
                            subclass(Route.Main.Reminder::class, Route.Main.Reminder.serializer())
                            subclass(Route.Main.Qibla::class, Route.Main.Qibla.serializer())
                            subclass(Route.Main.QiblaCompass::class, Route.Main.QiblaCompass.serializer())
                            subclass(Route.Main.Counter::class, Route.Main.Counter.serializer())
                            subclass(Route.Main.Settings::class, Route.Main.Settings.serializer())
                            subclass(Route.Main.Settings.InterfaceSettings::class, Route.Main.Settings.InterfaceSettings.serializer())
                            subclass(
                                Route.Main.Settings.SoundAndNotifications::class,
                                Route.Main.Settings.SoundAndNotifications.serializer(),
                            )
                            subclass(Route.Main.Settings.Calculations::class, Route.Main.Settings.Calculations.serializer())
                            subclass(Route.Main.Settings.Troubleshoot::class, Route.Main.Settings.Troubleshoot.serializer())
                            subclass(Route.Main.Settings.WidgetSettings::class, Route.Main.Settings.WidgetSettings.serializer())
                            subclass(Route.Main.Settings.BackupAndRestore::class, Route.Main.Settings.BackupAndRestore.serializer())
                            subclass(Route.Main.Settings.Developer::class, Route.Main.Settings.Developer.serializer())
                            subclass(
                                Route.Main.Settings.Calculations.Adjustments::class,
                                Route.Main.Settings.Calculations.Adjustments.serializer(),
                            )
                            subclass(
                                Route.Main.Settings.Calculations.AdvancedCalculation::class,
                                Route.Main.Settings.Calculations.AdvancedCalculation.serializer(),
                            )
                            subclass(
                                Route.Main.Settings.Troubleshoot.AdvancedTroubleshoot::class,
                                Route.Main.Settings.Troubleshoot.AdvancedTroubleshoot.serializer(),
                            )
                            subclass(
                                Route.Main.Settings.SoundAndNotifications.ScheduleAndMuezzin::class,
                                Route.Main.Settings.SoundAndNotifications.ScheduleAndMuezzin.serializer(),
                            )
                            subclass(
                                Route.Main.Settings.SoundAndNotifications.PrayerSchedule::class,
                                Route.Main.Settings.SoundAndNotifications.PrayerSchedule.serializer(),
                            )
                            subclass(Route.Main.UpcomingAlarms::class, Route.Main.UpcomingAlarms.serializer())
                            subclass(Route.Main.About::class, Route.Main.About.serializer())
                        }
                    }
                },
            startingRoute ?: Route.Main.Home,
        )

    BindBackStackWithController(
        currentRoute = { mainBackstack.lastOrNull() },
        navigateTo = { route -> mainBackstack.navigateTo(route) },
        popBack = { mainBackstack.removeAt(mainBackstack.lastIndex) },
        canPopBack = { mainBackstack.size > 1 },
    )

    NavDisplay(
        backStack = mainBackstack,
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        transitionSpec = {
            slideInHorizontally(animationSpec = tween(280), initialOffsetX = { fw -> fw * slideDirections.forwardEnter }) togetherWith
                slideOutHorizontally(animationSpec = tween(280), targetOffsetX = { fw -> fw * slideDirections.forwardExit / 2 })
        },
        popTransitionSpec = {
            slideInHorizontally(animationSpec = tween(280), initialOffsetX = { fw -> fw * slideDirections.backEnter / 2 }) togetherWith
                slideOutHorizontally(animationSpec = tween(280), targetOffsetX = { fw -> fw * slideDirections.backExit })
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(animationSpec = tween(280), initialOffsetX = { fw -> fw * slideDirections.backEnter / 2 }) togetherWith
                slideOutHorizontally(animationSpec = tween(280), targetOffsetX = { fw -> fw * slideDirections.backExit })
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Route.Main.Home> {
                val vm = hiltViewModel<HomeViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                HomeScreen(s, vm::onAction)
                HomePermissionGate(
                    isDontAskAgain = vm::isDontAskAgain,
                    onDontAskAgain = vm::onPermissionDontAskAgain,
                    onReschedule = vm::rescheduleAlarms,
                    onCleanup = vm::cleanupAlarms,
                    getCheck = vm::permissionCheck,
                )
                RamadanNoticeGate(
                    shouldShow = vm::shouldShowRamadanNotice,
                    onRemindNextYear = vm::onRamadanRemindNextYear,
                    onDontShowAgain = vm::onRamadanDontShowAgain,
                )
            }
            entry<Route.Main.Location> {
                val vm = hiltViewModel<LocationViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                LocationScreen(s, vm::onAction, getCountries = vm::getCountries, getCities = vm::getCities)
            }
            entry<Route.Main.Settings> {
                val vm = hiltViewModel<SettingsMenuViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                SettingsMenuScreen(s, vm::onAction)
            }
            entry<Route.Main.UpcomingAlarms> {
                val vm = hiltViewModel<UpcomingAlarmsViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                UpcomingAlarmsScreen(s, vm::onAction)
            }
            entry<Route.Main.About> {
                val vm = hiltViewModel<AboutViewModel>()
                AboutScreen(vm::onAction)
            }
            entry<Route.Main.MonthlyView> {
                val vm = hiltViewModel<MonthlyViewViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                MonthlyViewScreen(s, vm::onAction)
            }
            entry<Route.Main.Reminder> {
                val vm = hiltViewModel<ReminderViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                ReminderScreen(s, vm::onAction)
            }
            entry<Route.Main.Qibla> {
                val vm = hiltViewModel<QiblaViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                QiblaScreen(s, vm::onAction)
            }
            entry<Route.Main.QiblaCompass> {
                val vm = hiltViewModel<QiblaCompassViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                QiblaCompassRoute(s, vm.readings, vm::onAction)
            }
            entry<Route.Main.Counter> {
                val vm = hiltViewModel<CounterViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                CounterScreen(s, vm::onAction)
            }
            entry<Route.Main.Settings.InterfaceSettings> {
                val vm = hiltViewModel<InterfaceSettingsViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                InterfaceSettingsScreen(s, vm::onAction)
            }
            entry<Route.Main.Settings.SoundAndNotifications> {
                val vm = hiltViewModel<AdhanSettingsViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                AdhanSettingsScreen(s, vm::onAction)
            }
            entry<Route.Main.Settings.Calculations> {
                val vm = hiltViewModel<CalculationSettingsViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                ScreenScaffold(
                    title = stringResource(R.string.calculation_title),
                    onBackClick = { NavigationController.navigateBack() },
                ) {
                    CalculationSettingsScreen(s, vm::onAction)
                }
            }
            entry<Route.Main.Settings.Troubleshoot> {
                val vm = hiltViewModel<TroubleshootViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                ScreenScaffold(
                    title = stringResource(R.string.troubleshoot_title),
                    onBackClick = { NavigationController.navigateBack() },
                ) {
                    TroubleshootScreen(s, vm::onAction)
                }
            }
            entry<Route.Main.Settings.WidgetSettings> {
                val vm = hiltViewModel<WidgetSettingsViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                WidgetSettingsScreen(s, vm::onAction, events = vm.events)
            }
            entry<Route.Main.Settings.BackupAndRestore> {
                val vm = hiltViewModel<BackupRestoreViewModel>()
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                BackupRestoreScreen(
                    uiState = uiState,
                    onAction = vm::onAction,
                    events = vm.events,
                )
            }
            entry<Route.Main.Settings.Developer> {
                val vm = hiltViewModel<DeveloperViewModel>()
                DeveloperScreen(vm::onAction)
            }
            entry<Route.Main.Settings.Calculations.Adjustments> {
                val vm = hiltViewModel<AdjustmentsViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                AdjustmentsScreen(s, vm::onAction)
            }
            entry<Route.Main.Settings.Calculations.AdvancedCalculation> {
                val vm = hiltViewModel<AdvancedCalcViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                AdvancedCalcScreen(s, vm::onAction)
            }
            entry<Route.Main.Settings.Troubleshoot.AdvancedTroubleshoot> {
                val vm = hiltViewModel<AdvancedTroubleshootViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                AdvancedTroubleshootScreen(s, vm::onAction)
            }
            entry<Route.Main.Settings.SoundAndNotifications.ScheduleAndMuezzin> {
                val vm = hiltViewModel<AdhanSettingsViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                ScheduleAndMuezzinScreen(s, vm::onAction)
            }
            entry<Route.Main.Settings.SoundAndNotifications.PrayerSchedule> { key ->
                val vm = hiltViewModel<AdhanSettingsViewModel>()
                val s by vm.uiState.collectAsStateWithLifecycle()
                PrayerScheduleScreen(key.prayer, s, vm::onAction)
            }
        },
    )
}
