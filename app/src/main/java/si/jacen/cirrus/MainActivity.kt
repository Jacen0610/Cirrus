package si.jacen.cirrus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
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
}
