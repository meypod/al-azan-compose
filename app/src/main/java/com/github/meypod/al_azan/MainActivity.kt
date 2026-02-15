package com.github.meypod.al_azan

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.navigation.NavigationRoot
import com.github.meypod.al_azan.di.LanguageSync
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var languageSync: LanguageSync

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appIntroDone = runBlocking { settingsRepository.fetch().appIntroDone }
        setContent {
            LaunchedEffect(Unit) {
                // ensures our view of settings stays up to date with user's selected language
                // in case they decide to change it outside the app
                languageSync.run()
            }
            AlAzanTheme(settingsRepository = settingsRepository) {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    NavigationRoot(
                        modifier = Modifier.padding(paddingValues),
                        appIntroDone = appIntroDone,
                    )
                }
            }
        }
    }
}
