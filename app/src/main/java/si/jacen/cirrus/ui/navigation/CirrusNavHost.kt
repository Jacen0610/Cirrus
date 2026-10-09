package si.jacen.cirrus.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import si.jacen.cirrus.R
import si.jacen.cirrus.ui.benchmark.BenchmarkScreen
import si.jacen.cirrus.ui.camera.CameraScreen
import si.jacen.cirrus.ui.diary.DiaryScreen
import si.jacen.cirrus.ui.diary.ObservationDetailScreen
import si.jacen.cirrus.ui.home.HomeScreen
import si.jacen.cirrus.ui.model.ModelInfoScreen
import si.jacen.cirrus.ui.result.ResultScreen

object Routes {
    const val ARG_PHOTO_FILE_NAME = "photoFileName"
    const val ARG_OBSERVATION_ID = "id"

    const val HOME = "home"
    const val CAMERA = "camera"
    const val DIARY = "diary"
    const val MODEL = "model"
    const val RESULT = "result/{$ARG_PHOTO_FILE_NAME}"
    const val OBSERVATION = "observation/{$ARG_OBSERVATION_ID}"
    const val BENCHMARK = "benchmark"

    private val TOP_LEVEL = setOf(HOME, CAMERA, DIARY, MODEL)

    fun isTopLevel(route: String?): Boolean = route in TOP_LEVEL

    fun result(photoFileName: String) = "result/$photoFileName"
    fun observation(id: Long) = "observation/$id"
}

private data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

@Composable
fun CirrusNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val topLevelDestinations = listOf(
        TopLevelDestination(Routes.HOME, R.string.tab_home, Icons.Filled.Home),
        TopLevelDestination(Routes.CAMERA, R.string.tab_capture, Icons.Filled.Add),
        TopLevelDestination(Routes.DIARY, R.string.tab_diary, Icons.Filled.List),
        TopLevelDestination(Routes.MODEL, R.string.tab_model, Icons.Filled.Info),
    )

    Scaffold(
        bottomBar = {
            if (Routes.isTopLevel(currentRoute)) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(destination.icon, contentDescription = stringResource(destination.labelRes))
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onStartCapture = { navController.navigateToTopLevel(Routes.CAMERA) },
                )
            }

            composable(Routes.CAMERA) {
                CameraScreen(
                    onPhotoCaptured = { fileName ->
                        navController.navigate(Routes.result(fileName))
                    },
                )
            }

            composable(Routes.DIARY) {
                DiaryScreen(
                    onBack = null,
                    onOpenObservation = { id ->
                        navController.navigate(Routes.observation(id))
                    },
                )
            }

            composable(Routes.MODEL) {
                ModelInfoScreen(
                    onOpenBenchmark = { navController.navigate(Routes.BENCHMARK) },
                )
            }

            composable(
                route = Routes.RESULT,
                arguments = listOf(
                    navArgument(Routes.ARG_PHOTO_FILE_NAME) { type = NavType.StringType },
                ),
            ) {
                ResultScreen(
                    onSaved = {
                        navController.navigate(Routes.DIARY) {
                            popUpTo(Routes.HOME)
                        }
                    },
                    onDiscard = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.OBSERVATION,
                arguments = listOf(
                    navArgument(Routes.ARG_OBSERVATION_ID) { type = NavType.LongType },
                ),
            ) {
                ObservationDetailScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.BENCHMARK) {
                BenchmarkScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun androidx.navigation.NavController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
