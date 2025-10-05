package at.mcbabo.authenticator

import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import at.mcbabo.authenticator.data.store.UserPreferences
import at.mcbabo.authenticator.navigation.Navigation
import at.mcbabo.authenticator.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var appSettings: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            val useDynamicColors by appSettings.useDynamicColors.collectAsState(true)

            AppTheme(dynamicColor = useDynamicColors) {
                SystemBarsTheme()
                DismissKeyboard {
                    Navigation(navController)
                }
            }
        }
    }
}

@Composable
fun SystemBarsTheme(backgroundColor: Color = MaterialTheme.colorScheme.background) {
    val activity = LocalActivity.current
    val insetsController = WindowCompat.getInsetsController(activity?.window!!, activity.window.decorView)
    val isLightBackground = backgroundColor.luminance() > 0.5f

    LaunchedEffect(backgroundColor) {
        activity.window?.setBackgroundDrawable(
            backgroundColor.toArgb().toDrawable()
        )

        activity.window.setNavigationBarContrastEnforced(false)

        insetsController.isAppearanceLightStatusBars = isLightBackground
        insetsController.isAppearanceLightNavigationBars = isLightBackground
    }
}

@Composable
fun DismissKeyboard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val focusManager = LocalFocusManager.current

    Box(
        modifier =
            modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.first()

                        if (change.pressed) {
                            focusManager.clearFocus()
                        }
                    }
                }
            }
    ) {
        content()
    }
}
