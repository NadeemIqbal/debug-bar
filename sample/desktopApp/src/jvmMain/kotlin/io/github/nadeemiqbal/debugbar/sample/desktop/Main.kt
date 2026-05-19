package io.github.nadeemiqbal.debugbar.sample.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.nadeemiqbal.debugbar.sample.SampleApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DebugBar Sample",
        state = rememberWindowState(width = 600.dp, height = 800.dp),
    ) {
        SampleApp()
    }
}
