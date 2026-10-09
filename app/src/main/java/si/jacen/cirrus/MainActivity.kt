package si.jacen.cirrus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import si.jacen.cirrus.data.local.PhotoFileStore
import si.jacen.cirrus.ui.common.LocalPhotoFileStore
import si.jacen.cirrus.ui.navigation.CirrusNavHost
import si.jacen.cirrus.ui.theme.CirrusTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var photoFileStore: PhotoFileStore

    override fun onCreate(savedInstanceState: Bundle?) {
        // 系统 SplashScreen：保证品牌图标至少短暂展示后平滑过渡到主页
        val splashScreen = installSplashScreen()
        val splashShownAt = System.currentTimeMillis()
        splashScreen.setKeepOnScreenCondition {
            System.currentTimeMillis() - splashShownAt < MIN_SPLASH_DURATION_MS
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CirrusTheme {
                CompositionLocalProvider(LocalPhotoFileStore provides photoFileStore) {
                    CirrusNavHost()
                }
            }
        }
    }

    companion object {
        private const val MIN_SPLASH_DURATION_MS = 500L
    }
}
