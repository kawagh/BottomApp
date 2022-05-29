package jp.kawagh.bottomapp

import android.app.AppOpsManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import jp.kawagh.bottomapp.ui.theme.BottomAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO check permission from @Composable MainScreen when RecentContent is displayed
        if (needUsageStatsPermission()) {
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                startActivity(this)
            }
        }
        setContent {
            BottomAppTheme {
                SideEffectChangingSystemUi()
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    App()
                }
            }
        }
    }

    // PACKAGE_USAGE_STATS is not a runtimePermission
    private fun needUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        // `AppOpsManager.checkOpNoThrow` is renamed from Android Q
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                applicationInfo.uid, applicationInfo.packageName
            )
        } else {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                applicationInfo.uid, applicationInfo.packageName
            )
        }
        return mode != AppOpsManager.MODE_ALLOWED
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun App() {
    MainScreen()
}

@Composable
fun SideEffectChangingSystemUi() {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colors.isLight

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )
    }

}
