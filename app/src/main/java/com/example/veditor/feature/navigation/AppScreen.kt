package com.example.veditor.feature.navigation

import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

@Parcelize
object HomeScreen : Screen

@Parcelize
object ImportScreen : Screen

@Parcelize
data class EditorScreen(
    val selectedUris: ArrayList<String>,
) : Screen


